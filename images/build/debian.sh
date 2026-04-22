#!/bin/bash
set -euo pipefail

VERSION="${1:-12}"
ARCH="${2:-arm64}"
OUTPUT_DIR="${3:-.}"

echo "Building Debian $VERSION for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

DEB_VERSION="$VERSION"
DEB_ARCH="arm64"

ROOTFS_DIR="$TEMP_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

if command -v debootstrap &> /dev/null; then
    echo "Running debootstrap..."
    debootstrap --arch "$DEB_ARCH" --variant=minbase "$DEB_VERSION" "$ROOTFS_DIR" \
        http://deb.debian.org/debian/
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

echo "Creating squashfs..."
mksquashfs "$ROOTFS_DIR" "$OUTPUT_DIR/debian-${DEB_VERSION}-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/debian-${DEB_VERSION}-${ARCH}.sfs" | cut -d' ' -f1)
echo "debian-${DEB_VERSION}-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done! Size: $(du -h "$OUTPUT_DIR/debian-${DEB_VERSION}-${ARCH}.sfs" | cut -f1)"