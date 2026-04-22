#!/bin/bash
set -euo pipefail

VERSION="${1:-24.04}"
ARCH="${2:-arm64}"
OUTPUT_DIR="${3:-.}"

echo "Building Ubuntu $VERSION for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

UBUNTU_VERSION="$VERSION"
UBUNTU_ARCH="arm64"

ROOTFS_DIR="$TEMP_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

if command -v debootstrap &> /dev/null; then
    echo "Running debootstrap for Ubuntu..."
    debootstrap --arch "$UBUNTU_ARCH" --variant=minbase "$UBUNTU_VERSION" "$ROOTFS_DIR" \
        http://archive.ubuntu.com/ubuntu/
else
    echo "Error: debootstrap not found"
    exit 1
fi

echo "Setting up init..."
cat > "$ROOTFS_DIR/sbin/init" << 'INIT_EOF'
#!/bin/sh
export HOME=/root
export TERM=xterm-256color
exec /bin/bash --login
INIT_EOF
chmod +x "$ROOTFS_DIR/sbin/init"

echo "Stripping unnecessary files..."
rm -rf "$ROOTFS_DIR/usr/share/doc"/*
rm -rf "$ROOTFS_DIR/usr/share/man"/*
rm -rf "$ROOTFS_DIR/var/lib/apt/lists"/*
find "$ROOTFS_DIR/var/cache" -type f -delete 2>/dev/null || true

echo "Creating squashfs..."
mksquashfs "$ROOTFS_DIR" "$OUTPUT_DIR/ubuntu-${UBUNTU_VERSION}-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/ubuntu-${UBUNTU_VERSION}-${ARCH}.sfs" | cut -d' ' -f1)
echo "ubuntu-${UBUNTU_VERSION}-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done! Size: $(du -h "$OUTPUT_DIR/ubuntu-${UBUNTU_VERSION}-${ARCH}.sfs" | cut -f1)"