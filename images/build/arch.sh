#!/bin/bash
set -euo pipefail

ARCH="${1:-arm64}"
OUTPUT_DIR="${2:-.}"

echo "Building Arch Linux for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

ARCH_ARCH="aarch64"

ROOTFS_DIR="$TEMP_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

if command -v pacstrap &> /dev/null; then
    echo "Running pacstrap..."
    pacstrap -C /dev/null -M - "$ROOTFS_DIR" base
else
    echo "Warning: pacstrap not found, creating minimal rootfs"
    mkdir -p "$ROOTFS_DIR"/{bin,etc,home,root,var,sys,proc,dev,tmp,usr/bin,usr/sbin,lib,lib64}
fi

echo "Setting up init..."
cat > "$ROOTFS_DIR/sbin/init" << 'INIT_EOF'
#!/bin/sh
export HOME=/root
export TERM=xterm-256color
exec /bin/bash
INIT_EOF
chmod +x "$ROOTFS_DIR/sbin/init"

echo "Creating squashfs..."
mksquashfs "$ROOTFS_DIR" "$OUTPUT_DIR/arch-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/arch-${ARCH}.sfs" | cut -d' ' -f1)
echo "arch-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done!"