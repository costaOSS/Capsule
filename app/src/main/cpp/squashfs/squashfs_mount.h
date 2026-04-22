#ifndef SQUASHFS_MOUNT_H
#define SQUASHFS_MOUNT_H

int capsule_mount_squashfs(const char* sfs_path, const char* mountpoint);
int capsule_unmount_squashfs(const char* mountpoint);

#endif