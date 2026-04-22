#ifndef PTY_MANAGER_H
#define PTY_MANAGER_H

#include <stdint.h>

int* capsule_enter_session(const char* merged, const char* shell);
int capsule_write_pty(int fd, const uint8_t* data, int len);
int capsule_resize_pty(int fd, int rows, int cols);
int capsule_kill_session(int pid, int signal);

#endif