# Capsule - Next-Generation Linux Environment Launcher for Android

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2029%2B-brightgreen" alt="Android API 29+">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a%20%2F%20x86__64-yellow" alt="Architecture">
</p>

## What is Capsule?

Capsule is a standalone Android application that provides a native Linux environment on Android devices without requiring root access. Unlike Termux, UserLAnd, or AnLinux, Capsule uses a unique architecture with squashfs-based distro images and FUSE overlay filesystems for persistent storage.

## Key Features

- **Native Terminal Emulator**: Built from the ground up using Kotlin with Canvas rendering
- **Squashfs-Based Images**: Pre-built, compressed Linux root filesystems for minimal storage
- **FUSE Overlay**: Persistent writable layer on top of read-only squashfs
- **OCI Image Support**: Pull Docker/OCI images directly from Docker Hub or GHCR
- **No Root Required**: Works on any Android 10+ device
- **Fast Startup**: Under 800ms cold start to shell prompt
- **Low RAM Overhead**: Under 20MB per idle session

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Capsule App (Kotlin)                   │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                                  │
│  ├── HomeScreen - Distro grid                                │
│  ├── TerminalScreen - TerminalView + toolbar                │
│  ├── InstallScreen - Image download                         │
│  └── SettingsScreen - Preferences                           │
├─────────────────────────────────────────────────────────────┤
│  Runtime Layer                                              │
│  ├── SessionManager - Session lifecycle                     │
│  ├── PtyReader - PTY output flow                           │
│  └── CapsuleRuntime JNI Bridge                              │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ├── Room Database - Image/Session metadata                 │
│  ├── DataStore - Preferences                               │
│  ├── Ktor Client - Network operations                      │
│  └── ImageRepository - Image management                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼ JNI
┌─────────────────────────────────────────────────────────────┐
│                    CapsuleRuntime.so (NDK)                   │
├─────────────────────────────────────────────────────────────┤
│  squashfs_mount.c - Mount squashfuse via /dev/fuse          │
│  overlay_mount.c - Mount fuse-overlayfs                    │
│  pty_manager.c - PTY creation, fork/exec                   │
│  ns_enter.c - Namespace setup, pivot_root/chroot           │
│  dev_setup.c - /dev, /proc, /sys setup                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Android Kernel                           │
│  ├── /dev/fuse - FUSE device                               │
│  ├── /dev/pts - PTY devices                                │
│  └── Linux namespaces (if available)                       │
└─────────────────────────────────────────────────────────────┘
```

## Boot Path

```
User taps distro on HomeScreen
           │
           ▼
SessionManager.createSession(distro)
           │
           ▼
┌─────────────────────────────────────┐
│ CapsuleRuntime.mountSquashfs()       │
│ sfsPath → filesDir/images/<distro>.sfs│
│ basePath → filesDir/mnt/<distro>/base │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ CapsuleRuntime.mountOverlay()        │
│ basePath → filesDir/mnt/<distro>/base│
│ overlayPath → filesDir/overlay/<distro>│
│ mergedPath → filesDir/mnt/<distro>/merged│
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ CapsuleRuntime.setupDev()            │
│ Mount tmpfs on merged/dev            │
│ Create device nodes (null, zero, etc)│
│ Mount devpts, bind /proc, /sys      │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ CapsuleRuntime.enterSession()        │
│ posix_openpt() → PTY master FD      │
│ fork() → child process              │
│ chroot(mergedPath)                  │
│ execve(shell)                       │
│ Returns: [ptyFd, pid]               │
└─────────────────────────────────────┘
           │
           ▼
Navigate to TerminalScreen
TerminalView subscribes to PtyReader
Shell prompt appears
```

## Comparison with Alternatives

| Feature | Termux | UserLAnd | AnLinux | Capsule |
|---------|--------|----------|---------|---------|
| Root Required | No | No | No | No |
| Own Terminal | Yes | No (VNC) | No (VNC) | Yes |
| Squashfs Images | No | No | No | Yes |
| OCI Support | No | No | No | Yes |
| Session Isolation | Partial | No | No | Yes (process groups) |
| RAM Overhead | High | Very High | High | <20MB |
| PRoot Dependency | Optional | Yes | Yes | Never |
| Startup Speed | Slow | Very Slow | Slow | <800ms |

## Tech Stack

### Android App (Kotlin)
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 35
- **UI**: Jetpack Compose
- **Terminal**: Custom View with Canvas rendering
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Network**: Ktor
- **Async**: Kotlin Coroutines + Flow

### NDK Layer (C)
- **NDK Version**: r26 LTS
- **ABI**: arm64-v8a, x86_64
- **Components**:
  - squashfuse (embedded)
  - fuse-overlayfs (embedded)
  - PTY management
  - Namespace handling
  - Device setup

## Supported Distros

- Ubuntu 24.04 LTS
- Debian
- Arch Linux
- Alpine Linux
- Void Linux
- Fedora

## Building from Source

### Prerequisites
- JDK 17+
- Android SDK 35
- Android NDK r26

### Build Commands

```bash
# Clone the repository
git clone https://github.com/costaOSS/Capsule.git
cd Capsule

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### CI/CD

The project uses GitHub Actions for:
- **Android Build**: Builds APK on every push
- **RootFS Build**: Builds squashfs images weekly
- **Release**: Creates signed releases on tag push

## Directory Structure

```
app/src/main/
├── kotlin/dev/capsule/
│   ├── CapsuleApp.kt
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── theme/
│   │   ├── navigation/
│   │   ├── screens/home/
│   │   ├── screens/terminal/
│   │   ├── screens/install/
│   │   ├── screens/settings/
│   │   └── terminal/
│   ├── runtime/
│   ├── image/
│   ├── oci/
│   ├── data/
│   │   ├── db/
│   │   └── prefs/
│   └── di/
├── cpp/
│   ├── CMakeLists.txt
│   ├── capsule_runtime.cpp
│   ├── squashfs/
│   ├── overlay/
│   ├── pty/
│   ├── namespace/
│   └── dev/
└── res/
```

## Security Considerations

- No root privileges required
- All operations run in userspace via FUSE
- Process isolation via fork()/chroot()
- SHA256 verification for all downloaded images
- OCI layer digest verification

## License

MIT License - See LICENSE file for details.

## Acknowledgments

- [Termux](https://termux.com/) - Terminal emulator library (Apache 2.0)
- [squashfuse](https://github.com/vasi/squashfuse) - Squashfs FUSE implementation
- [fuse-overlayfs](https://github.com/containers/fuse-overlayfs) - OverlayFS implementation
