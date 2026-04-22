#!/bin/bash
set -euo pipefail

VERSION="${1:-40}"
ARCH="${2:-arm64}"
OUTPUT_DIR="${3:-.}"

echo "Building Fedora $VERSION for $ARCH..."

TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

FEDORA_VERSION="$VERSION"
FEDORA_ARCH="aarch64"

ROOTFS_DIR="$TEMP_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

if command -v dnf &> /dev/null; then
    echo "Installing minimal packages..."
    dnf install -y --installroot="$ROOTFS_DIR" \
        dnf \
        bash \
        coreutils \
        filesystem \
        --releasever="$FEDORA_VERSION" \
        --baseurl="https://dl.fedoraproject.org/pub/fedora/linux/releases/$FEDORA_VERSION/Everything/aarch64/os/"
else
    echo "Warning: dnf not found, creating minimal rootfs"
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
mksquashfs "$ROOTFS_DIR" "$OUTPUT_DIR/fedora-${FEDORA_VERSION}-${ARCH}.sfs" \
    -comp zstd \
    -Xcompression-level 19 \
    -b 131072 \
    -noappend

echo "Calculating SHA256..."
SHA256=$(sha256sum "$OUTPUT_DIR/fedora-${FEDORA_VERSION}-${ARCH}.sfs" | cut -d' ' -f1)
echo "fedora-${FEDORA_VERSION}-${ARCH}.sfs: $SHA256" >> "$OUTPUT_DIR/SHA256SUMS"

echo "Done!"