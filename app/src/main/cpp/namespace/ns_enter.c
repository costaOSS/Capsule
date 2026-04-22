#ifndef NS_ENTER_C
#define NS_ENTER_C

#include "ns_enter.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <errno.h>
#include <sched.h>

int capsule_enter_namespace(const char* merged) {
    if (unshare(CLONE_NEWNS) != 0) {
    }

    char put_old[512];
    snprintf(put_old, sizeof(put_old), "%s/put_old", merged);

    if (mkdir(put_old, 0755) != 0 && errno != EEXIST) {
    }

    if (pivot_root(merged, put_old) != 0) {
        if (chroot(merged) != 0) {
            return -1;
        }
        chdir("/");
        return 0;
    }

    chdir("/");
    if (umount2("/put_old", MNT_DETACH) != 0) {
    }

    return 0;
}

#endif