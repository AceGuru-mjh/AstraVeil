#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

BUILD_MODE="release"
TARGET_ABIS=("arm64-v8a" "armeabi-v7a")
DO_CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --debug) BUILD_MODE="debug"; shift ;;
        --abi)   TARGET_ABIS=("$2"); shift 2 ;;
        --clean) DO_CLEAN=true; shift ;;
        --help|-h) head -20 "$0" | tail -15; exit 0 ;;
        *) error "Unknown arg: $1"; exit 1 ;;
    esac
done

check_prerequisites() {
    command -v cargo &>/dev/null || { error "cargo not installed. Run: curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"; exit 1; }
    cargo ndk --version &>/dev/null || { error "cargo-ndk not installed. Run: cargo install cargo-ndk"; exit 1; }
    local ndk="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
    if [[ -z "$ndk" ]]; then
        local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
        ndk=$(find "$sdk/ndk" -maxdepth 1 -type d 2>/dev/null | sort -V | tail -1)
        [[ -z "$ndk" ]] && { error "NDK not found. Set ANDROID_NDK_HOME."; exit 1; }
        export ANDROID_NDK_HOME="$ndk"
    fi
    info "NDK: $ANDROID_NDK_HOME"
    for target in aarch64-linux-android armv7-linux-androideabi; do
        rustup target list --installed | grep -q "$target" || { warn "Adding target: $target"; rustup target add "$target"; }
    done
}

build() {
    local cargo_args=("ndk")
    for abi in "${TARGET_ABIS[@]}"; do cargo_args+=("-t" "$abi"); done
    cargo_args+=("--platform" "26" "--" "build")
    [[ "$BUILD_MODE" == "release" ]] && cargo_args+=("--release")
    info "cargo ${cargo_args[*]}"
    cargo "${cargo_args[@]}"
    echo ""
    info "Build complete. Output:"
    for abi in "${TARGET_ABIS[@]}"; do
        local triple; case $abi in arm64-v8a) triple="aarch64-linux-android";; armeabi-v7a) triple="armv7-linux-androideabi";; *) triple="$abi";; esac
        local lib="target/${triple}/${BUILD_MODE}/libastra_rust.a"
        [[ -f "$lib" ]] && echo -e "  ${GREEN}✓${NC} $lib ($(du -h "$lib" | cut -f1))" || echo -e "  ${RED}✗${NC} $lib (not found)"
    done
}

info "AstraVeil Rust policy engine build"
check_prerequisites
[[ "$DO_CLEAN" == true ]] && { info "Cleaning..."; cargo clean; }
build
info "Done. Gradle will auto-detect these libs."
