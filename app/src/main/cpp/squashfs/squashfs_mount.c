#ifndef SQUASHFS_MOUNT_C
#define SQUASHFS_MOUNT_C

#include "squashfs_mount.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mount.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <errno.h>

int capsule_mount_squashfs(const char* sfs_path, const char* mountpoint) {
    if (access(sfs_path, R_OK) != 0) {
        return -1;
    }

    if (mkdir(mountpoint, 0755) != 0 && errno != EEXIST) {
        return -1;
    }

    int fuse_fd = open("/dev/fuse", O_RDWR);
    if (fuse_fd < 0) {
        return -1;
    }

    pid_t pid = fork();
    if (pid < 0) {
        close(fuse_fd);
        return -1;
    }

    if (pid == 0) {
        close(fuse_fd);

        char fd_str[32];
        snprintf(fd_str, sizeof(fd_str), "%d", fuse_fd);

        execlp("squashfuse", "squashfuse",
               "-o", "fd=3,allow_other,default_permissions",
               sfs_path, mountpoint, NULL);

        _exit(1);
    }

    close(fuse_fd);
    return pid;
}

int capsule_unmount_squashfs(const char* mountpoint) {
    if (umount2(mountpoint, MNT_DETACH) != 0) {
        return -1;
    }
    rmdir(mountpoint);
    return 0;
}

#endif