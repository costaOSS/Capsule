#ifndef OVERLAY_MOUNT_C
#define OVERLAY_MOUNT_C

#include "overlay_mount.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mount.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <errno.h>
#include <string.h>

int capsule_mount_overlay(const char* lower, const char* upper, const char* work, const char* merged) {
    if (access(lower, R_OK) != 0) {
        return -1;
    }

    if (mkdir(upper, 0755) != 0 && errno != EEXIST) {
        return -1;
    }

    if (mkdir(work, 0755) != 0 && errno != EEXIST) {
        return -1;
    }

    if (mkdir(merged, 0755) != 0 && errno != EEXIST) {
        return -1;
    }

    char options[512];
    snprintf(options, sizeof(options), "lowerdir=%s,upperdir=%s,workdir=%s", lower, upper, work);

    if (mount("overlay", merged, "overlay", 0, options) != 0) {
        return -1;
    }

    return 0;
}

int capsule_unmount_overlay(const char* merged) {
    if (umount2(merged, MNT_DETACH) != 0) {
        return -1;
    }
    return 0;
}

#endif