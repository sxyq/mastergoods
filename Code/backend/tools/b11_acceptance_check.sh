#!/usr/bin/env bash
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "${BACKEND_DIR}/../.." && pwd)"
ANDROID_DIR="${REPO_ROOT}/Code/frontend/android"
JDK21_HOME="${JDK21_HOME:-/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home}"
EVIDENCE_ROOT="${EVIDENCE_ROOT:-${REPO_ROOT}/docs/acceptance-evidence/b11}"

usage() {
  cat <<'USAGE'
Usage: ./tools/b11_acceptance_check.sh <target>

Targets:
  backend-smoke      Run focused backend B11 /v2, migration, and compatibility tests.
  android-contract  Run current focused Android /v2 model/network and partial repository contract tests.
  android-assemble  Run Android assembleDebug with JDK 21.
  android-assemble-release  Run Android assembleRelease with JDK 21.
  backend-bootjar   Run backend bootJar with JDK 21.
  evidence-dirs     Create the B11 evidence directory structure.

Environment:
  JDK21_HOME         Defaults to /Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
  EVIDENCE_ROOT     Defaults to docs/acceptance-evidence/b11

Note:
  This script prints command output to stdout/stderr and does not archive logs automatically.
  Redirect or tee output into EVIDENCE_ROOT when recording B11 evidence.
USAGE
}

ensure_jdk() {
  if [[ ! -x "${JDK21_HOME}/bin/java" ]]; then
    echo "JDK21_HOME does not point to an executable JDK: ${JDK21_HOME}" >&2
    exit 2
  fi
}

evidence_dirs() {
  mkdir -p \
    "${EVIDENCE_ROOT}/backend" \
    "${EVIDENCE_ROOT}/android" \
    "${EVIDENCE_ROOT}/screenshots" \
    "${EVIDENCE_ROOT}/performance" \
    "${EVIDENCE_ROOT}/security-release"
  echo "B11 evidence directories are ready under ${EVIDENCE_ROOT}"
}

backend_smoke() {
  ensure_jdk
  cd "${REPO_ROOT}"
  local gradle_cmd="${BACKEND_DIR}/gradlew"
  if [[ ! -x "${gradle_cmd}" ]]; then
    if [[ -x "${ANDROID_DIR}/gradlew" ]]; then
      gradle_cmd="${ANDROID_DIR}/gradlew"
    elif command -v gradle >/dev/null 2>&1; then
      gradle_cmd="gradle"
    else
      echo "BLOCKED: no backend Gradle wrapper, no reusable Android Gradle wrapper, and no global gradle command found." >&2
      exit 127
    fi
  fi
  JAVA_HOME="${JDK21_HOME}" "${gradle_cmd}" \
    -p "${BACKEND_DIR}" \
    test \
    --tests 'com.zhihuiji.backend.application.service.v2.*' \
    --tests 'com.zhihuiji.backend.api.controller.V2*' \
    --tests 'com.zhihuiji.backend.api.controller.V1*CompatibilityControllerTest' \
    --tests 'com.zhihuiji.backend.infrastructure.db.*' \
    --console=plain \
    -Dorg.gradle.java.home="${JDK21_HOME}"
}

android_contract() {
  ensure_jdk
  cd "${REPO_ROOT}"
  JAVA_HOME="${JDK21_HOME}" "${ANDROID_DIR}/gradlew" \
    -p "${ANDROID_DIR}" \
    :core:model:testDebugUnitTest \
    :core:network:testDebugUnitTest \
    :data:agent:testDebugUnitTest \
    :data:finance:testDebugUnitTest \
    --console=plain \
    -Dorg.gradle.java.home="${JDK21_HOME}"
}

android_assemble() {
  ensure_jdk
  cd "${REPO_ROOT}"
  JAVA_HOME="${JDK21_HOME}" "${ANDROID_DIR}/gradlew" \
    -p "${ANDROID_DIR}" \
    assembleDebug \
    --console=plain \
    -Dorg.gradle.java.home="${JDK21_HOME}"
}

android_assemble_release() {
  ensure_jdk
  cd "${REPO_ROOT}"
  JAVA_HOME="${JDK21_HOME}" "${ANDROID_DIR}/gradlew" \
    -p "${ANDROID_DIR}" \
    assembleRelease \
    --console=plain \
    -Dorg.gradle.java.home="${JDK21_HOME}"
}

backend_bootjar() {
  ensure_jdk
  cd "${REPO_ROOT}"
  JAVA_HOME="${JDK21_HOME}" "${BACKEND_DIR}/gradlew" \
    -p "${BACKEND_DIR}" \
    bootJar \
    --console=plain \
    -Dorg.gradle.java.home="${JDK21_HOME}"
}

target="${1:-}"
case "${target}" in
  backend-smoke)
    backend_smoke
    ;;
  android-contract)
    android_contract
    ;;
  android-assemble)
    android_assemble
    ;;
  android-assemble-release)
    android_assemble_release
    ;;
  backend-bootjar)
    backend_bootjar
    ;;
  evidence-dirs)
    evidence_dirs
    ;;
  ""|-h|--help|help)
    usage
    ;;
  *)
    echo "Unknown target: ${target}" >&2
    usage >&2
    exit 2
    ;;
esac
