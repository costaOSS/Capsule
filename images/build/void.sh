#!/bin/bash
set -euo pipefail

VERSION="${1:-latest}"
ARCH="${2:-arm64}"
OUTPUT_DIR="${3:-.}"

echo "Building Void Linux for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

VOID_VERSION="${VERSION:-20240101}"

echo "Setting up container for Void Linux..."
mkdir -p "$TEMP_DIR/rootfs"

if command -v xbps-install-static &> /dev/null; then
    xbps-install-static -r "$TEMP_DIR/rootfs" base-files
else
    echo "Warning: xbps-install-static not available, creating minimal rootfs"
    mkdir -p "$TEMP_DIR/rootfs"/{bin,etc,home,root,var,sys,proc,dev,tmp,usr/bin,usr/sbin}

    echo "#!/bin/sh" > "$TEMP_DIR/rootfs/sbin/init"
    echo "exec /bin/sh" >> "$TEMP_DIR/rootfs/sbin/init"
    chmod +x "$TEMP_DIR/rootfs/sbin/init"
fi

echo "Creating squashfs..."
mksquashfs "$TEMP_DIR/rootfs" "$OUTPUT_DIR/void-${VOID_VERSION}-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/void-${VOID_VERSION}-${ARCH}.sfs" | cut -d' ' -f1)
echo "void-${VOID_VERSION}-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done!"