#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import sqlite3
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


ROOM_IDENTITY_HASH = "e896ad50def3e9d177c893c4a4038e29"
EPSILON = 1e-9

SOURCE_DEFAULT = Path(
    "/Users/sunyiyang/Desktop/Project/master-goods/migration_source_zhihuiji/9ffd7446d3f1480197908a113565d0ef.db"
)
OUTPUT_DEFAULT = Path(
    "/Users/sunyiyang/Desktop/Project/master-goods/migration_output/zhihuiji.db"
)
ADB_DEFAULT = Path("/Users/sunyiyang/Library/Android/sdk/platform-tools/adb")


CREATE_SQL = [
    "PRAGMA foreign_keys = ON",
    "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `unit` TEXT NOT NULL, `salePrice` REAL NOT NULL, `purchasePrice` REAL NOT NULL, `stock` REAL NOT NULL, `safeStock` REAL NOT NULL, `status` INTEGER NOT NULL, `syncStatus` INTEGER, `syncVersion` INTEGER, `createdAt` INTEGER, `updatedAt` INTEGER, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `level` INTEGER NOT NULL, `address` TEXT, `notes` TEXT, `balance` REAL NOT NULL, `status` INTEGER NOT NULL, `syncStatus` INTEGER, `syncVersion` INTEGER, `createdAt` INTEGER, `updatedAt` INTEGER, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `suppliers` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT, `notes` TEXT, `balance` REAL NOT NULL, `status` INTEGER NOT NULL, `syncStatus` INTEGER, `syncVersion` INTEGER, `createdAt` INTEGER, `updatedAt` INTEGER, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sale_orders` (`id` INTEGER NOT NULL, `orderNo` TEXT NOT NULL, `customerId` INTEGER, `customerName` TEXT, `subtotalAmount` REAL NOT NULL, `discountAmount` REAL NOT NULL, `totalAmount` REAL NOT NULL, `paidAmount` REAL NOT NULL, `notes` TEXT, `status` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sale_order_items` (`id` INTEGER NOT NULL, `orderId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productCode` TEXT NOT NULL, `productName` TEXT NOT NULL, `quantity` REAL NOT NULL, `unitPrice` REAL NOT NULL, `amount` REAL NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`orderId`) REFERENCES `sale_orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_sale_order_items_orderId` ON `sale_order_items` (`orderId`)",
    "CREATE INDEX IF NOT EXISTS `index_sale_order_items_productId` ON `sale_order_items` (`productId`)",
    "CREATE INDEX IF NOT EXISTS `index_sale_order_items_productCode` ON `sale_order_items` (`productCode`)",
    "CREATE INDEX IF NOT EXISTS `index_sale_order_items_productName` ON `sale_order_items` (`productName`)",
    "CREATE TABLE IF NOT EXISTS `purchase_orders` (`id` INTEGER NOT NULL, `orderNo` TEXT NOT NULL, `supplierName` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `notes` TEXT, `status` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `pay_orders` (`id` INTEGER NOT NULL, `orderNo` TEXT NOT NULL, `supplierId` INTEGER, `supplierName` TEXT NOT NULL, `amount` REAL NOT NULL, `method` INTEGER NOT NULL, `referenceNo` TEXT, `notes` TEXT, `status` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `finance_records` (`id` INTEGER NOT NULL, `recordNo` TEXT NOT NULL, `type` INTEGER NOT NULL, `category` TEXT NOT NULL, `partnerName` TEXT, `amount` REAL NOT NULL, `method` INTEGER NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `agent_notifications` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `isDelivered` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `sync_cursors` (`entityType` TEXT NOT NULL, `cursor` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`entityType`))",
    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
]


@dataclass
class Summary:
    products: int = 0
    customers: int = 0
    suppliers: int = 0
    sale_orders: int = 0
    sale_order_items: int = 0
    purchase_orders: int = 0
    pay_orders: int = 0
    finance_records: int = 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Migrate rooted Kingdee Zhihuiji local SQLite data into the local Room schema."
    )
    parser.add_argument("--source-db", type=Path, default=SOURCE_DEFAULT)
    parser.add_argument("--output-db", type=Path, default=OUTPUT_DEFAULT)
    parser.add_argument("--adb", type=Path, default=ADB_DEFAULT)
    parser.add_argument("--device-serial", default="")
    parser.add_argument("--deploy", action="store_true", help="Push the generated DB into com.zhihuiji.app on a rooted device.")
    parser.add_argument("--package-name", default="com.zhihuiji.app")
    return parser.parse_args()


def approx_zero(value: float | int | None) -> bool:
    return value is None or abs(float(value)) <= EPSILON


def clean_text(value: object | None) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def fallback_code(prefix: str, row_id: int, raw: object | None) -> str:
    value = clean_text(raw)
    return value or f"{prefix}{row_id:06d}"


def parse_time(value: object | None) -> int:
    if value is None:
        return 0
    text = str(value).strip()
    if not text:
        return 0
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            dt = datetime.strptime(text, fmt)
            return int(dt.replace(tzinfo=timezone.utc).timestamp() * 1000)
        except ValueError:
            continue
    try:
        return int(float(text))
    except ValueError:
        return 0


def payment_method_from_acct_type(acct_type: object | None) -> int:
    value = int(acct_type or 0)
    if value == 1:
        return 1  # cash
    if value == 4:
        return 2  # wechat
    if value == 5:
        return 3  # alipay
    if value == 2:
        return 4  # bank
    return 5  # other


def compose_notes(*parts: str | None) -> str | None:
    normalized = [part.strip() for part in parts if part and part.strip()]
    return " | ".join(normalized) if normalized else None


def sale_total(row: sqlite3.Row) -> float:
    base_total = float(row["disc_amt"] if not approx_zero(row["disc_amt"]) or approx_zero(row["bill_amt"]) else row["bill_amt"])
    express = float(row["express_amt"] or 0.0)
    deduction = float(row["deduction_amt"] or 0.0)
    return base_total + express - deduction


def purchase_total(row: sqlite3.Row) -> float:
    base_total = float(row["disc_amt"] if not approx_zero(row["disc_amt"]) or approx_zero(row["bill_amt"]) else row["bill_amt"])
    express = float(row["express_amt"] or 0.0)
    deduction = float(row["deduction_amt"] or 0.0)
    return base_total + express - deduction


def finance_category(code: str, source_type: int, net_amount: float) -> str:
    if code.startswith("SKD"):
        return "收款"
    if code.startswith("SZD"):
        return "支出"
    if code.startswith("CZD"):
        return "储值"
    if source_type == 2:
        return "收款"
    if source_type == 3:
        return "支出" if net_amount < 0 else "其他收入"
    return "收入" if net_amount >= 0 else "支出"


def open_conn(path: Path) -> sqlite3.Connection:
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    return conn


def create_target_schema(conn: sqlite3.Connection) -> None:
    for statement in CREATE_SQL:
        conn.execute(statement)
    conn.execute(
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
        (ROOM_IDENTITY_HASH,),
    )
    conn.commit()


def load_lookup(src: sqlite3.Connection, sql: str) -> dict[int, sqlite3.Row]:
    return {int(row["id"]): row for row in src.execute(sql)}


def migrate(source_db: Path, output_db: Path) -> Summary:
    if not source_db.exists():
        raise FileNotFoundError(f"Source DB not found: {source_db}")

    output_db.parent.mkdir(parents=True, exist_ok=True)
    if output_db.exists():
        output_db.unlink()

    shutil.copyfile(source_db, output_db.parent / f"{output_db.stem}.source-backup{output_db.suffix}")
    src = open_conn(source_db)
    dst = open_conn(output_db)
    create_target_schema(dst)

    companies = load_lookup(
        src,
        """
        SELECT id, name, tye, linkman, tel, mobile, addr, cur_amt, remark, is_stop, create_at, revise_at
        FROM companies
        WHERE is_del = 0
        """,
    )
    products = load_lookup(
        src,
        """
        SELECT id, name, code, unit, ptype_id, pur_prc, sale_prc, cur_stock, min_stock, remark, is_stop, create_at, revise_at
        FROM products
        WHERE is_del = 0
        """,
    )
    accounts = load_lookup(
        src,
        """
        SELECT id, name, tye
        FROM accts
        WHERE is_del = 0
        """,
    )

    summary = Summary()

    product_rows = []
    for row in products.values():
        product_rows.append(
            (
                int(row["id"]),
                fallback_code("P", int(row["id"]), row["code"]),
                clean_text(row["name"]) or f"商品{row['id']}",
                "",
                clean_text(row["unit"]) or "件",
                float(row["sale_prc"] or 0.0),
                float(row["pur_prc"] or 0.0),
                float(row["cur_stock"] or 0.0),
                float(row["min_stock"] or 0.0),
                0 if int(row["is_stop"] or 0) else 1,
                None,
                None,
                parse_time(row["create_at"]),
                parse_time(row["revise_at"]),
            )
        )
    dst.executemany(
        """
        INSERT INTO products
        (id, code, name, category, unit, salePrice, purchasePrice, stock, safeStock, status, syncStatus, syncVersion, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        product_rows,
    )
    summary.products = len(product_rows)

    customer_rows = []
    supplier_rows = []
    for row in companies.values():
        company_type = int(row["tye"] or 0)
        common = (
            int(row["id"]),
            clean_text(row["name"]) or f"往来单位{row['id']}",
            clean_text(row["mobile"]) or clean_text(row["tel"]) or "",
            clean_text(row["addr"]),
            clean_text(row["remark"]),
            float(row["cur_amt"] or 0.0),
            0 if int(row["is_stop"] or 0) else 1,
            None,
            None,
            parse_time(row["create_at"]),
            parse_time(row["revise_at"]),
        )
        if company_type == 2:
            supplier_rows.append(common)
        else:
            customer_rows.append((common[0], common[1], common[2], 0, common[3], common[4], common[5], common[6], common[7], common[8], common[9], common[10]))

    dst.executemany(
        """
        INSERT INTO customers
        (id, name, phone, level, address, notes, balance, status, syncStatus, syncVersion, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        customer_rows,
    )
    dst.executemany(
        """
        INSERT INTO suppliers
        (id, name, phone, address, notes, balance, status, syncStatus, syncVersion, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        supplier_rows,
    )
    summary.customers = len(customer_rows)
    summary.suppliers = len(supplier_rows)

    sale_rows = list(
        src.execute(
            """
            SELECT id, code, tye, company_id, bill_amt, disc_amt, deduction_amt, express_amt, pay_amt, owe_amt, remark, create_at, revise_at
            FROM sales
            WHERE is_del = 0
            ORDER BY id
            """
        )
    )
    sale_insert_rows = []
    sale_created_map: dict[int, int] = {}
    for row in sale_rows:
        order_total = sale_total(row)
        subtotal = float(row["bill_amt"] or 0.0)
        discount = subtotal - order_total
        company = companies.get(int(row["company_id"] or 0))
        extra_note = "退货单" if int(row["tye"] or 1) == 2 else None
        created_at = parse_time(row["create_at"])
        updated_at = parse_time(row["revise_at"])
        sale_created_map[int(row["id"])] = created_at
        sale_insert_rows.append(
            (
                int(row["id"]),
                fallback_code("SO", int(row["id"]), row["code"]),
                int(row["company_id"]) if int(row["company_id"] or 0) > 0 else None,
                clean_text(company["name"]) if company else None,
                subtotal,
                discount,
                order_total,
                float(row["pay_amt"] or 0.0),
                compose_notes(clean_text(row["remark"]), extra_note),
                1,
                created_at,
                updated_at,
            )
        )
    dst.executemany(
        """
        INSERT INTO sale_orders
        (id, orderNo, customerId, customerName, subtotalAmount, discountAmount, totalAmount, paidAmount, notes, status, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        sale_insert_rows,
    )
    summary.sale_orders = len(sale_insert_rows)

    sale_item_rows = list(
        src.execute(
            """
            SELECT id, sale_id, product_id, prc, qty, amt
            FROM saleitems
            WHERE is_del = 0
            ORDER BY id
            """
        )
    )
    sale_item_insert_rows = []
    for row in sale_item_rows:
        product = products.get(int(row["product_id"]))
        sale_item_insert_rows.append(
            (
                int(row["id"]),
                int(row["sale_id"]),
                int(row["product_id"]),
                fallback_code("P", int(row["product_id"]), product["code"] if product else None),
                clean_text(product["name"]) if product else f"商品{row['product_id']}",
                float(row["qty"] or 0.0),
                float(row["prc"] or 0.0),
                float(row["amt"] or 0.0),
                sale_created_map.get(int(row["sale_id"]), 0),
            )
        )
    dst.executemany(
        """
        INSERT INTO sale_order_items
        (id, orderId, productId, productCode, productName, quantity, unitPrice, amount, createdAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        sale_item_insert_rows,
    )
    summary.sale_order_items = len(sale_item_insert_rows)

    purchase_rows = list(
        src.execute(
            """
            SELECT id, code, company_id, acct_id, bill_amt, disc_amt, deduction_amt, express_amt, pay_amt, owe_amt, remark, create_at, revise_at
            FROM purs
            WHERE is_del = 0
            ORDER BY id
            """
        )
    )
    purchase_insert_rows = []
    pay_order_insert_rows = []
    for row in purchase_rows:
        total = purchase_total(row)
        company = companies.get(int(row["company_id"] or 0))
        supplier_name = clean_text(company["name"]) if company else "默认供应商"
        created_at = parse_time(row["create_at"])
        updated_at = parse_time(row["revise_at"])
        purchase_insert_rows.append(
            (
                int(row["id"]),
                fallback_code("PO", int(row["id"]), row["code"]),
                supplier_name,
                total,
                clean_text(row["remark"]),
                1,
                created_at,
                updated_at,
            )
        )
        paid_amount = float(row["pay_amt"] or 0.0)
        if not approx_zero(paid_amount):
            acct = accounts.get(int(row["acct_id"] or 0))
            pay_order_insert_rows.append(
                (
                    int(row["id"]),
                    f"FK-{fallback_code('PO', int(row['id']), row['code'])}",
                    int(row["company_id"]) if int(row["company_id"] or 0) > 0 else None,
                    supplier_name,
                    abs(paid_amount),
                    payment_method_from_acct_type(acct["tye"] if acct else None),
                    fallback_code("PO", int(row["id"]), row["code"]),
                    compose_notes(clean_text(row["remark"]), "迁移自旧系统采购付款"),
                    1,
                    created_at,
                    updated_at,
                )
            )
    dst.executemany(
        """
        INSERT INTO purchase_orders
        (id, orderNo, supplierName, totalAmount, notes, status, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        purchase_insert_rows,
    )
    dst.executemany(
        """
        INSERT INTO pay_orders
        (id, orderNo, supplierId, supplierName, amount, method, referenceNo, notes, status, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        pay_order_insert_rows,
    )
    summary.purchase_orders = len(purchase_insert_rows)
    summary.pay_orders = len(pay_order_insert_rows)

    fund_rows = list(
        src.execute(
            """
            SELECT id, code, company_id, acct_id, tye, in_amt, out_amt, remark, create_at, revise_at
            FROM funds
            WHERE is_del = 0
            ORDER BY id
            """
        )
    )
    finance_insert_rows = []
    for row in fund_rows:
        acct = accounts.get(int(row["acct_id"] or 0))
        company = companies.get(int(row["company_id"] or 0))
        net = float(row["in_amt"] or 0.0) - float(row["out_amt"] or 0.0)
        finance_insert_rows.append(
            (
                int(row["id"]),
                fallback_code("FR", int(row["id"]), row["code"]),
                1 if net >= 0 else 2,
                finance_category(fallback_code("FR", int(row["id"]), row["code"]), int(row["tye"] or 0), net),
                clean_text(company["name"]) if company else None,
                abs(net),
                payment_method_from_acct_type(acct["tye"] if acct else None),
                clean_text(row["remark"]),
                parse_time(row["create_at"]),
                parse_time(row["revise_at"]),
            )
        )
    dst.executemany(
        """
        INSERT INTO finance_records
        (id, recordNo, type, category, partnerName, amount, method, notes, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        finance_insert_rows,
    )
    summary.finance_records = len(finance_insert_rows)

    dst.commit()
    src.close()
    dst.close()
    return summary


def run(cmd: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, text=True, capture_output=True, check=check)


def adb_cmd(adb_path: Path, serial: str, *args: str) -> list[str]:
    cmd = [str(adb_path)]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(args)
    return cmd


def adb_shell_su(adb_path: Path, serial: str, command: str) -> str:
    result = run(adb_cmd(adb_path, serial, "shell", "su", "-c", command))
    return result.stdout.strip()


def deploy_to_device(output_db: Path, adb_path: Path, serial: str, package_name: str) -> None:
    if not adb_path.exists():
        raise FileNotFoundError(f"adb not found: {adb_path}")
    devices_output = run(adb_cmd(adb_path, "", "devices", "-l")).stdout
    if serial and serial not in devices_output:
        raise RuntimeError(f"Device serial not connected: {serial}")
    if not serial:
        lines = [line for line in devices_output.splitlines() if "\tdevice" in line]
        if len(lines) != 1:
            raise RuntimeError("Expected exactly one connected device when --device-serial is omitted.")
        serial = lines[0].split()[0]

    remote_tmp = "/data/local/tmp/zhihuiji-migration.db"
    remote_dir = f"/data/user/0/{package_name}/databases"
    run(adb_cmd(adb_path, serial, "shell", "am", "force-stop", package_name))
    run(adb_cmd(adb_path, serial, "push", str(output_db), remote_tmp))

    run_as_probe = run(adb_cmd(adb_path, serial, "shell", "run-as", package_name, "pwd"), check=False)
    if run_as_probe.returncode == 0 and package_name in run_as_probe.stdout:
        run(
            adb_cmd(
                adb_path,
                serial,
                "shell",
                "run-as",
                package_name,
                "rm",
                "-f",
                f"{remote_dir}/zhihuiji.db",
                f"{remote_dir}/zhihuiji.db-wal",
                f"{remote_dir}/zhihuiji.db-shm",
            )
        )
        run(
            adb_cmd(
                adb_path,
                serial,
                "shell",
                "run-as",
                package_name,
                "cp",
                remote_tmp,
                f"{remote_dir}/zhihuiji.db",
            )
        )
    else:
        owner = adb_shell_su(adb_path, serial, f"ls -ldn /data/data/{package_name} | awk '{{print $3\":\"$4}}'")
        if ":" not in owner:
            raise RuntimeError(f"Failed to determine owner for {package_name}: {owner}")
        adb_shell_su(
            adb_path,
            serial,
            f"""
            mkdir -p /data/data/{package_name}/databases &&
            if [ -f /data/data/{package_name}/databases/zhihuiji.db ]; then
              cp /data/data/{package_name}/databases/zhihuiji.db /data/data/{package_name}/databases/zhihuiji.db.bak.$(date +%s);
            fi &&
            rm -f /data/data/{package_name}/databases/zhihuiji.db /data/data/{package_name}/databases/zhihuiji.db-wal /data/data/{package_name}/databases/zhihuiji.db-shm &&
            dd if={remote_tmp} of=/data/data/{package_name}/databases/zhihuiji.db bs=4096 &&
            chown {owner} /data/data/{package_name}/databases/zhihuiji.db &&
            chmod 660 /data/data/{package_name}/databases/zhihuiji.db
            """.replace("\n", " "),
        )

    run(adb_cmd(adb_path, serial, "shell", "rm", "-f", remote_tmp))


def print_summary(summary: Summary) -> None:
    print("迁移完成：")
    print(f"  products        : {summary.products}")
    print(f"  customers       : {summary.customers}")
    print(f"  suppliers       : {summary.suppliers}")
    print(f"  sale_orders     : {summary.sale_orders}")
    print(f"  sale_order_items: {summary.sale_order_items}")
    print(f"  purchase_orders : {summary.purchase_orders}")
    print(f"  pay_orders      : {summary.pay_orders}")
    print(f"  finance_records : {summary.finance_records}")


def main() -> int:
    args = parse_args()
    try:
        summary = migrate(args.source_db, args.output_db)
        print_summary(summary)
        print(f"输出数据库：{args.output_db}")
        if args.deploy:
            deploy_to_device(args.output_db, args.adb, args.device_serial, args.package_name)
            print(f"已推送到设备 app 沙箱：{args.package_name}")
    except Exception as exc:  # pragma: no cover - operational script
        print(f"迁移失败：{exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
