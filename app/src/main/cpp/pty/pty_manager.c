#ifndef PTY_MANAGER_C
#define PTY_MANAGER_C

#include "pty_manager.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <errno.h>
#include <string.h>
#include <termios.h>
#include <pts_name.h>

int* capsule_enter_session(const char* merged, const char* shell) {
    static int result[2];

    int master_fd = posix_openpt(O_RDWR | O_NOCTTY);
    if (master_fd < 0) {
        return NULL;
    }

    if (grantpt(master_fd) != 0) {
        close(master_fd);
        return NULL;
    }

    if (unlockpt(master_fd) != 0) {
        close(master_fd);
        return NULL;
    }

    char* slave_name = ptsname(master_fd);
    if (!slave_name) {
        close(master_fd);
        return NULL;
    }

    pid_t pid = fork();
    if (pid < 0) {
        close(master_fd);
        return NULL;
    }

    if (pid == 0) {
        close(master_fd);

        setsid();

        int slave_fd = open(slave_name, O_RDWR);
        if (slave_fd < 0) {
            _exit(1);
        }

        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);

        if (slave_fd > STDERR_FILENO) {
            close(slave_fd);
        }

        chdir(merged);
        chroot(merged);
        chdir("/");

        execve(shell, (char*[]){ (char*)shell, NULL }, (char*[]){ NULL });

        _exit(1);
    }

    result[0] = master_fd;
    result[1] = pid;
    return result;
}

int capsule_write_pty(int fd, const uint8_t* data, int len) {
    return write(fd, data, len);
}

int capsule_resize_pty(int fd, int rows, int cols) {
    struct winsize ws;
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    return ioctl(fd, TIOCSWINSZ, &ws);
}

int capsule_kill_session(int pid, int signal) {
    return kill(pid, signal);
}

#endif