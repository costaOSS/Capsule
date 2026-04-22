#!/bin/bash
set -euo pipefail

DISTRO="${1:-alpine}"
VERSION="${2:-latest}"
ARCH="${3:-arm64}"
OUTPUT_DIR="${4:-.}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$DISTRO" in
    alpine)
        "$SCRIPT_DIR/alpine.sh" "$VERSION" "$ARCH" "$OUTPUT_DIR"
        ;;
    void)
        "$SCRIPT_DIR/void.sh" "$VERSION" "$ARCH" "$OUTPUT_DIR"
        ;;
    debian)
        "$SCRIPT_DIR/debian.sh" "$VERSION" "$ARCH" "$OUTPUT_DIR"
        ;;
    ubuntu)
        "$SCRIPT_DIR/ubuntu.sh" "$VERSION" "$ARCH" "$OUTPUT_DIR"
        ;;
    arch)
        "$SCRIPT_DIR/arch.sh" "$ARCH" "$OUTPUT_DIR"
        ;;
    fedora)
        "$SCRIPT_DIR/fedora.sh" "$VERSION" "$ARCH" "$OUTPUT_DIR"
        ;;
    *)
        echo "Unknown distro: $DISTRO"
        exit 1
        ;;
esac

echo "Build complete: $DISTRO-$VERSION-$ARCH"