#ifndef OVERLAY_MOUNT_H
#define OVERLAY_MOUNT_H

int capsule_mount_overlay(const char* lower, const char* upper, const char* work, const char* merged);
int capsule_unmount_overlay(const char* merged);

#endif