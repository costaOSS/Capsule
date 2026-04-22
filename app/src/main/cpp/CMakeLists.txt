cmake_minimum_required(VERSION 3.25.1)

project(capsule-runtime C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

add_library(capsule-runtime SHARED
    capsule_runtime.cpp
    squashfs/squashfs_mount.c
    overlay/overlay_mount.c
    pty/pty_manager.c
    namespace/ns_enter.c
    dev/dev_setup.c
)

target_include_directories(capsule-runtime PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(log-lib log)
find_library(utils-lib utils)
find_library(rt-lib rt)
find_library(fuse-lib fuse)

target_link_libraries(capsule-runtime
    ${log-lib}
    ${utils-lib}
    ${rt-lib}
    ${fuse-lib}
)

target_compile_options(capsule-runtime PRIVATE
    -Wall
    -Wextra
    -Werror=implicit-function-declaration
    -Wno-unused-parameter
)