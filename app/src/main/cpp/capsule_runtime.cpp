#include <jni.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <android/log.h>

#include "squashfs/squashfs_mount.h"
#include "overlay/overlay_mount.h"
#include "pty/pty_manager.h"
#include "namespace/ns_enter.h"
#include "dev/dev_setup.h"

#define LOG_TAG "CapsuleRuntime"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_mountSquashfs(JNIEnv* env, jobject thiz,
                                              jstring sfsPath, jstring basePath) {
    const char* sfs_path = env->GetStringUTFChars(sfsPath, NULL);
    const char* base_path = env->GetStringUTFChars(basePath, NULL);

    int result = capsule_mount_squashfs(sfs_path, base_path);

    env->ReleaseStringUTFChars(sfsPath, sfs_path);
    env->ReleaseStringUTFChars(basePath, base_path);

    return result;
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_mountOverlay(JNIEnv* env, jobject thiz,
                                             jstring basePath, jstring overlayPath,
                                             jstring mergedPath) {
    const char* base_path = env->GetStringUTFChars(basePath, NULL);
    const char* overlay_path = env->GetStringUTFChars(overlayPath, NULL);
    const char* merged_path = env->GetStringUTFChars(mergedPath, NULL);

    char upper[512];
    char work[512];
    snprintf(upper, sizeof(upper), "%s/upper", overlay_path);
    snprintf(work, sizeof(work), "%s/work", overlay_path);

    int result = capsule_mount_overlay(base_path, upper, work, merged_path);

    env->ReleaseStringUTFChars(basePath, base_path);
    env->ReleaseStringUTFChars(overlayPath, overlay_path);
    env->ReleaseStringUTFChars(mergedPath, merged_path);

    return result;
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_setupDev(JNIEnv* env, jobject thiz,
                                               jstring mergedPath) {
    const char* merged_path = env->GetStringUTFChars(mergedPath, NULL);

    int result = capsule_setup_dev(merged_path);

    env->ReleaseStringUTFChars(mergedPath, merged_path);

    return result;
}

JNIEXPORT jintArray JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_enterSession(JNIEnv* env, jobject thiz,
                                                jstring mergedPath, jstring shell) {
    const char* merged_path = env->GetStringUTFChars(mergedPath, NULL);
    const char* shell_path = env->GetStringUTFChars(shell, NULL);

    int* result = capsule_enter_session(merged_path, shell_path);

    env->ReleaseStringUTFChars(mergedPath, merged_path);
    env->ReleaseStringUTFChars(shell, shell_path);

    if (!result) {
        return NULL;
    }

    jintArray arr = env->NewIntArray(2);
    jint elements[2] = { result[0], result[1] };
    env->SetIntArrayRegion(arr, 0, 2, elements);

    return arr;
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_writePty(JNIEnv* env, jobject thiz,
                                                 jint ptyFd, jbyteArray data) {
    jbyte* bytes = env->GetByteArrayElements(data, NULL);
    jsize len = env->GetArrayLength(data);

    int result = capsule_write_pty(ptyFd, (uint8_t*)bytes, len);

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    return result;
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_killSession(JNIEnv* env, jobject thiz,
                                                   jint pid, jint signal) {
    return capsule_kill_session(pid, signal);
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_unmount(JNIEnv* env, jobject thiz,
                                            jstring path) {
    const char* unmount_path = env->GetStringUTFChars(path, NULL);

    int result = capsule_unmount_overlay(unmount_path);
    if (result != 0) {
        result = capsule_unmount_squashfs(unmount_path);
    }

    env->ReleaseStringUTFChars(path, unmount_path);

    return result;
}

JNIEXPORT jboolean JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_isFuseAvailable(JNIEnv* env, jobject thiz) {
    struct stat st;
    return (stat("/dev/fuse", &st) == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_dev_capsule_runtime_CapsuleRuntime_resizePty(JNIEnv* env, jobject thiz,
                                                  jint ptyFd, jint rows,
                                                  jint cols) {
    return capsule_resize_pty(ptyFd, rows, cols);
}