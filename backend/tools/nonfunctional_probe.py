#!/usr/bin/env python3
import argparse
import json
import math
import os
import random
import statistics
import string
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


def now_ms():
    return int(time.time() * 1000)


def unique_suffix():
    rand = "".join(random.choices(string.ascii_uppercase + string.digits, k=6))
    return f"{int(time.time() * 1000)}{rand}"


def percentile(values, ratio):
    if not values:
        return None
    ordered = sorted(values)
    idx = min(len(ordered) - 1, max(0, math.ceil(ratio * len(ordered)) - 1))
    return ordered[idx]


def summarize_results(name, results):
    durations = [item["duration_ms"] for item in results if item.get("duration_ms") is not None]
    statuses = {}
    logical_codes = {}
    for item in results:
        statuses[str(item.get("status"))] = statuses.get(str(item.get("status")), 0) + 1
        payload = item.get("json")
        if isinstance(payload, dict) and "code" in payload:
            logical_codes[str(payload["code"])] = logical_codes.get(str(payload["code"]), 0) + 1
    ok = sum(1 for item in results if 200 <= (item.get("status") or 0) < 300)
    return {
        "name": name,
        "requests": len(results),
        "http_ok": ok,
        "http_error": len(results) - ok,
        "status_counts": statuses,
        "logical_code_counts": logical_codes,
        "p50_ms": percentile(durations, 0.50),
        "p95_ms": percentile(durations, 0.95),
        "max_ms": max(durations) if durations else None,
        "mean_ms": round(statistics.mean(durations), 2) if durations else None,
        "sample_errors": [item for item in results if item.get("error")][:5],
    }


class ProbeClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/") + "/"

    def request(self, method, path, payload=None, headers=None, raw_body=None, content_type="application/json"):
        url = urllib.parse.urljoin(self.base_url, path.lstrip("/"))
        body = None
        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
        elif raw_body is not None:
            body = raw_body.encode("utf-8")
        request = urllib.request.Request(url=url, method=method.upper(), data=body)
        request.add_header("Accept", "application/json")
        if body is not None:
            request.add_header("Content-Type", content_type)
        for key, value in (headers or {}).items():
            request.add_header(key, value)

        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                response_body = response.read().decode("utf-8", errors="replace")
                status = response.getcode()
                return self._build_result(status, response_body, time.perf_counter() - started)
        except urllib.error.HTTPError as exc:
            response_body = exc.read().decode("utf-8", errors="replace")
            return self._build_result(exc.code, response_body, time.perf_counter() - started, error=f"HTTPError {exc.code}")
        except Exception as exc:  # noqa: BLE001
            return {
                "status": None,
                "body": None,
                "json": None,
                "duration_ms": round((time.perf_counter() - started) * 1000, 2),
                "error": repr(exc),
            }

    def _build_result(self, status, body, duration_seconds, error=None):
        try:
            parsed = json.loads(body)
        except Exception:  # noqa: BLE001
            parsed = None
        return {
            "status": status,
            "body": body,
            "json": parsed,
            "duration_ms": round(duration_seconds * 1000, 2),
            "error": error,
        }


def ensure_success(result, label):
    payload = result.get("json")
    if result.get("status") != 200 or not isinstance(payload, dict) or payload.get("code") != 0:
        raise RuntimeError(f"{label} failed: {result}")
    return payload["data"]


def seed_auth(client):
    suffix = unique_suffix()
    phone = "13" + suffix[-9:]
    password = "123456"
    register = client.request(
        "POST",
        "auth/register",
        {"phone": phone, "password": password, "verify_code": "021218"},
    )
    register_data = ensure_success(register, "register")
    login = client.request("POST", "auth/login", {"phone": phone, "password": password})
    login_data = ensure_success(login, "login")
    return {
        "phone": phone,
        "password": password,
        "register": register_data,
        "login": login_data,
        "token": login_data["token"],
        "refresh_token": login_data["refresh_token"],
    }


def seed_product(client, prefix="NF-P", stock=200.0):
    suffix = unique_suffix()
    payload = {
        "code": f"{prefix}-{suffix}",
        "name": f"{prefix}-商品-{suffix}",
        "category": "非功能验证",
        "unit": "件",
        "sale_price": 39.9,
        "purchase_price": 18.6,
        "stock": stock,
        "safe_stock": 8.0,
        "status": 1,
        "description": "nonfunctional probe seed",
    }
    created = ensure_success(client.request("POST", "products", payload), "create product")
    return created


def seed_customer(client):
    suffix = unique_suffix()
    payload = {
        "name": f"NF-客户-{suffix}",
        "phone": "139" + suffix[-8:],
        "level": 2,
        "address": "非功能测试地址",
        "notes": "nonfunctional probe seed",
        "balance": 0.0,
        "status": 1,
    }
    return ensure_success(client.request("POST", "customers", payload), "create customer")


def run_concurrent(count, workers, func):
    gate = threading.Event()

    def wrapped(index):
        gate.wait()
        return func(index)

    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = [executor.submit(wrapped, index) for index in range(count)]
        started = time.perf_counter()
        gate.set()
        results = [future.result() for future in as_completed(futures)]
    elapsed = round((time.perf_counter() - started) * 1000, 2)
    return results, elapsed


def load_suite(client, auth_seed, product_seed):
    scenarios = {}

    health_results, health_elapsed = run_concurrent(
        count=200,
        workers=25,
        func=lambda _: client.request("GET", "sync/health"),
    )
    scenarios["sync_health_read_burst"] = summarize_results("sync_health_read_burst", health_results)
    scenarios["sync_health_read_burst"]["wall_time_ms"] = health_elapsed

    code = urllib.parse.quote(product_seed["code"], safe="")
    by_code_results, by_code_elapsed = run_concurrent(
        count=160,
        workers=20,
        func=lambda _: client.request("GET", f"products/by-code?code={code}"),
    )
    scenarios["products_by_code_read_burst"] = summarize_results("products_by_code_read_burst", by_code_results)
    scenarios["products_by_code_read_burst"]["wall_time_ms"] = by_code_elapsed

    login_results, login_elapsed = run_concurrent(
        count=80,
        workers=12,
        func=lambda _: client.request(
            "POST",
            "auth/login",
            {"phone": auth_seed["phone"], "password": auth_seed["password"]},
        ),
    )
    scenarios["auth_login_burst"] = summarize_results("auth_login_burst", login_results)
    scenarios["auth_login_burst"]["wall_time_ms"] = login_elapsed

    def create_product(index):
        suffix = unique_suffix() + f"-{index}"
        return client.request(
            "POST",
            "products",
            {
                "code": f"NF-LOAD-{suffix}",
                "name": f"NF-LOAD-商品-{suffix}",
                "category": "压测",
                "unit": "件",
                "sale_price": 11.1,
                "purchase_price": 5.5,
                "stock": 5.0,
                "safe_stock": 1.0,
                "status": 1,
            },
        )

    create_results, create_elapsed = run_concurrent(count=60, workers=12, func=create_product)
    scenarios["product_create_write_burst"] = summarize_results("product_create_write_burst", create_results)
    scenarios["product_create_write_burst"]["wall_time_ms"] = create_elapsed
    return scenarios


def concurrency_suite(client):
    report = {}

    customer = seed_customer(client)
    oversell_product = seed_product(client, prefix="NF-RACE-SALE", stock=5.0)

    def create_sale_order(_):
        payload = {
            "customer_id": customer["id"],
            "customer_name": customer["name"],
            "items": [{"product_id": oversell_product["id"], "quantity": 1.0, "unit_price": 20.0}],
            "notes": "concurrency sale order",
            "discount_amount": 0.0,
        }
        return client.request("POST", "sale-orders", payload)

    sale_results, sale_elapsed = run_concurrent(count=12, workers=12, func=create_sale_order)
    refreshed_product = ensure_success(
        client.request("GET", f"products/by-code?code={urllib.parse.quote(oversell_product['code'], safe='')}"),
        "fetch race sale product",
    )
    sale_success = sum(1 for item in sale_results if item.get("status") == 200 and isinstance(item.get("json"), dict) and item["json"].get("code") == 0)
    sale_failures = [item for item in sale_results if not (item.get("status") == 200 and isinstance(item.get("json"), dict) and item["json"].get("code") == 0)]
    sale_invariant_ok = sale_success <= 5 and abs(refreshed_product["stock"] - (5.0 - sale_success)) < 0.000001
    report["sale_order_oversell_race"] = {
        "requests": 12,
        "workers": 12,
        "wall_time_ms": sale_elapsed,
        "success_count": sale_success,
        "failure_count": len(sale_failures),
        "final_stock": refreshed_product["stock"],
        "expected_stock_from_successes": 5.0 - sale_success,
        "invariant_ok": sale_invariant_ok,
        "sample_failures": sale_failures[:5],
    }

    adjust_product = seed_product(client, prefix="NF-RACE-ADJUST", stock=5.0)

    def adjust_stock(_):
        return client.request(
            "POST",
            f"products/{adjust_product['id']}/adjust-stock",
            {"delta": -1.0, "reason": "concurrency test", "operator": "probe"},
        )

    adjust_results, adjust_elapsed = run_concurrent(count=12, workers=12, func=adjust_stock)
    adjusted_product = ensure_success(
        client.request("GET", f"products/by-code?code={urllib.parse.quote(adjust_product['code'], safe='')}"),
        "fetch adjust race product",
    )
    adjust_success = sum(1 for item in adjust_results if item.get("status") == 200 and isinstance(item.get("json"), dict) and item["json"].get("code") == 0)
    adjust_failures = [item for item in adjust_results if not (item.get("status") == 200 and isinstance(item.get("json"), dict) and item["json"].get("code") == 0)]
    adjust_invariant_ok = adjust_success <= 5 and abs(adjusted_product["stock"] - (5.0 - adjust_success)) < 0.000001
    report["adjust_stock_outflow_race"] = {
        "requests": 12,
        "workers": 12,
        "wall_time_ms": adjust_elapsed,
        "success_count": adjust_success,
        "failure_count": len(adjust_failures),
        "final_stock": adjusted_product["stock"],
        "expected_stock_from_successes": 5.0 - adjust_success,
        "invariant_ok": adjust_invariant_ok,
        "sample_failures": adjust_failures[:5],
    }
    return report


def read_process_stats(pid):
    if not pid:
        return None
    command = (
        f"$p = Get-Process -Id {pid} -ErrorAction SilentlyContinue; "
        "if ($null -eq $p) { Write-Output '' } "
        "else { [pscustomobject]@{"
        "id=$p.Id; working_set_mb=[math]::Round($p.WorkingSet64/1MB,2); "
        "private_memory_mb=[math]::Round($p.PrivateMemorySize64/1MB,2); "
        "threads=$p.Threads.Count; handles=$p.HandleCount } | ConvertTo-Json -Compress }"
    )
    result = subprocess.run(
        ["powershell.exe", "-NoProfile", "-Command", command],
        capture_output=True,
        text=True,
        check=False,
    )
    stdout = result.stdout.strip()
    if not stdout:
        return None
    try:
        return json.loads(stdout)
    except json.JSONDecodeError:
        return {"raw": stdout}


def soak_suite(client, auth_seed, product_seed, duration_seconds, pid):
    code = urllib.parse.quote(product_seed["code"], safe="")
    deadline = time.time() + duration_seconds
    stop = threading.Event()
    results = []
    lock = threading.Lock()
    memory_samples = []
    endpoints = [
        lambda: client.request("GET", "sync/health"),
        lambda: client.request("GET", f"products/by-code?code={code}"),
        lambda: client.request("POST", "auth/login", {"phone": auth_seed["phone"], "password": auth_seed["password"]}),
    ]

    def worker(seed_offset):
        local_random = random.Random(seed_offset + now_ms())
        while not stop.is_set() and time.time() < deadline:
            endpoint = endpoints[local_random.randrange(len(endpoints))]
            result = endpoint()
            with lock:
                results.append(result)
            if result.get("status") is None:
                stop.set()
                return

    def sampler():
        while not stop.is_set() and time.time() < deadline:
            sample = read_process_stats(pid)
            if sample is not None:
                memory_samples.append({"ts": now_ms(), **sample})
            time.sleep(5)

    sampler_thread = threading.Thread(target=sampler, daemon=True)
    sampler_thread.start()
    workers = [threading.Thread(target=worker, args=(idx,), daemon=True) for idx in range(8)]
    for thread in workers:
        thread.start()
    for thread in workers:
        thread.join()
    stop.set()
    sampler_thread.join(timeout=2)

    summary = summarize_results("soak_mixed_traffic", results)
    summary["duration_seconds"] = duration_seconds
    summary["request_count"] = len(results)
    summary["memory_samples"] = memory_samples
    if memory_samples:
        summary["max_working_set_mb"] = max(sample.get("working_set_mb", 0) for sample in memory_samples)
        summary["min_working_set_mb"] = min(sample.get("working_set_mb", 0) for sample in memory_samples)
        summary["max_threads"] = max(sample.get("threads", 0) for sample in memory_samples)
    return summary


def fuzz_suite(client, auth_seed):
    huge = "9" * 5000
    auth_token = auth_seed["token"]
    cases = {
        "register_empty_body": lambda: client.request("POST", "auth/register", {}),
        "register_null_fields": lambda: client.request("POST", "auth/register", {"phone": None, "password": None, "verify_code": None}),
        "register_wrong_types": lambda: client.request("POST", "auth/register", {"phone": 123, "password": ["a"], "verify_code": {"x": 1}}),
        "register_huge_phone": lambda: client.request("POST", "auth/register", {"phone": huge, "password": "123456", "verify_code": "021218"}),
        "register_malformed_json": lambda: client.request("POST", "auth/register", raw_body='{"phone":', content_type="application/json"),
        "login_empty_body": lambda: client.request("POST", "auth/login", {}),
        "login_wrong_types": lambda: client.request("POST", "auth/login", {"phone": 123, "password": {"x": 1}}),
        "refresh_empty_body": lambda: client.request("POST", "auth/refresh", {}),
        "refresh_garbage": lambda: client.request("POST", "auth/refresh", {"refresh_token": "not-a-real-refresh"}),
        "logout_malformed_bearer": lambda: client.request("POST", "auth/logout", headers={"Authorization": "Basic nope"}),
        "me_missing_bearer": lambda: client.request("GET", "auth/users/me"),
        "me_garbage_bearer": lambda: client.request("GET", "auth/users/me", headers={"Authorization": "Bearer nope"}),
        "me_valid_bearer": lambda: client.request("GET", "auth/users/me", headers={"Authorization": f"Bearer {auth_token}"}),
        "verify_code_provider_missing": lambda: client.request("POST", "auth/verify-code", {"phone": auth_seed["phone"], "type": "register"}),
        "product_create_empty_body": lambda: client.request("POST", "products", {}),
        "product_create_wrong_types": lambda: client.request(
            "POST",
            "products",
            {"code": 1, "name": [], "category": {}, "unit": True, "sale_price": "x", "purchase_price": "y", "stock": "z", "safe_stock": "w"},
        ),
        "product_create_huge_name": lambda: client.request(
            "POST",
            "products",
            {
                "code": f"NF-FUZZ-{unique_suffix()}",
                "name": "X" * 5000,
                "category": "fuzz",
                "unit": "件",
                "sale_price": 1,
                "purchase_price": 1,
                "stock": 1,
                "safe_stock": 0,
                "status": 1,
            },
        ),
    }
    results = {}
    for name, runner in cases.items():
        result = runner()
        results[name] = {
            "status": result.get("status"),
            "logical_code": result.get("json", {}).get("code") if isinstance(result.get("json"), dict) else None,
            "message": result.get("json", {}).get("message") if isinstance(result.get("json"), dict) else None,
            "body_preview": (result.get("body") or "")[:400],
            "error": result.get("error"),
        }
    return results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:18080/v1/")
    parser.add_argument("--soak-seconds", type=int, default=120)
    parser.add_argument("--backend-pid", type=int, default=0)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    random.seed(42)
    client = ProbeClient(args.base_url)

    started = time.time()
    auth_seed = seed_auth(client)
    product_seed = seed_product(client, prefix="NF-SEED", stock=200.0)

    report = {
        "started_at": now_ms(),
        "base_url": args.base_url,
        "backend_pid": args.backend_pid,
        "seeds": {
            "auth_phone": auth_seed["phone"],
            "product_code": product_seed["code"],
        },
    }
    report["load"] = load_suite(client, auth_seed, product_seed)
    report["concurrency"] = concurrency_suite(client)
    report["soak"] = soak_suite(client, auth_seed, product_seed, args.soak_seconds, args.backend_pid)
    report["fuzz"] = fuzz_suite(client, auth_seed)
    report["finished_at"] = now_ms()
    report["total_runtime_seconds"] = round(time.time() - started, 2)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(str(output_path))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001
        print(f"nonfunctional probe failed: {exc}", file=sys.stderr)
        raise
