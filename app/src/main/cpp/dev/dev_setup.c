#ifndef DEV_SETUP_C
#define DEV_SETUP_C

#include "dev_setup.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>

static int make_device(const char* path, int mode, int dev) {
    if (mknod(path, mode, dev) != 0 && errno != EEXIST) {
        return -1;
    }
    return 0;
}

static int ensure_dir(const char* path) {
    if (mkdir(path, 0755) != 0 && errno != EEXIST) {
        return -1;
    }
    return 0;
}

int capsule_setup_dev(const char* merged) {
    char path[512];

    snprintf(path, sizeof(path), "%s/dev", merged);
    ensure_dir(path);
    mount("tmpfs", path, "tmpfs", MS_NOSUID, "mode=755");

    snprintf(path, sizeof(path), "%s/dev/null", merged);
    make_device(path, S_IFCHR | 0666, 1);

    snprintf(path, sizeof(path), "%s/dev/zero", merged);
    make_device(path, S_IFCHR | 0666, 5);

    snprintf(path, sizeof(path), "%s/dev/random", merged);
    make_device(path, S_IFCHR | 0666, 8);

    snprintf(path, sizeof(path), "%s/dev/urandom", merged);
    make_device(path, S_IFCHR | 0666, 9);

    snprintf(path, sizeof(path), "%s/dev/tty", merged);
    make_device(path, S_IFCHR | 0666, 5);

    snprintf(path, sizeof(path), "%s/dev/pts", merged);
    ensure_dir(path);
    mount("devpts", path, "devpts", 0, NULL);

    snprintf(path, sizeof(path), "%s/proc", merged);
    ensure_dir(path);
    mount("/proc", path, NULL, MS_BIND | MS_REC, NULL);

    snprintf(path, sizeof(path), "%s/sys", merged);
    ensure_dir(path);
    mount("/sys", path, NULL, MS_BIND | MS_REC, NULL);

    return 0;
}

#endif