#!/bin/bash
set -euo pipefail

VERSION="${1:-latest}"
ARCH="${2:-arm64}"
OUTPUT_DIR="${3:-.}"

echo "Building Alpine Linux $VERSION for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

ALPINE_VERSION="${VERSION:-3.19}"
ALPINE_ARCH="$ARCH"

ALPINE_MIRROR="https://dl-cdn.alpinelinux.org/alpine/v${ALPINE_VERSION}/releases/${ALPINE_ARCH}"

if [ "$ARCH" = "arm64" ]; then
    ARCH_NAME="aarch64"
else
    ARCH_NAME="x86_64"
fi

ROOTFS_URL="${ALPINE_MIRROR}/alpine-minirootfs-${ALPINE_VERSION}-${ARCH_NAME}.tar.gz"

echo "Downloading: $ROOTFS_URL"
curl -fsSL "$ROOTFS_URL" -o "$TEMP_DIR/rootfs.tar.gz"

echo "Extracting rootfs..."
mkdir -p "$TEMP_DIR/rootfs"
tar -xzf "$TEMP_DIR/rootfs.tar.gz -C $TEMP_DIR/rootfs"

echo "Setting up init..."
cat > "$TEMP_DIR/rootfs/sbin/init" << 'INIT_EOF'
#!/bin/sh
export HOME=/root
export TERM=xterm-256color
exec /bin/sh
INIT_EOF
chmod +x "$TEMP_DIR/rootfs/sbin/init"

echo "Creating squashfs..."
mksquashfs "$TEMP_DIR/rootfs" "$OUTPUT_DIR/alpine-${ALPINE_VERSION}-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend \
    -e usr/share/doc \
    -e usr/share/man \
    -e usr/share/locale

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/alpine-${ALPINE_VERSION}-${ARCH}.sfs" | cut -d' ' -f1)

echo "alpine-${ALPINE_VERSION}-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done! Output: $OUTPUT_DIR/alpine-${ALPINE_VERSION}-${ARCH}.sfs"
echo "SHA256: $SHA256"