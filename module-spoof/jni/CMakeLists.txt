# module-spoof/jni/CMakeLists.txt
cmake_minimum_required(VERSION 3.18)
project(astraveil_spoof CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── Dobby (inline hook 框架) ──
set(DOBBY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/external/Dobby)
set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
add_subdirectory(${DOBBY_DIR} dobby_build)

# ── 主模块 ──
# 源文件平铺在 jni/ 根目录下（entry.cpp / property_hook.cpp /
# gl_spoof.cpp / config_reader.cpp），头文件位于 jni/include/。
add_library(astraveil_spoof SHARED
    entry.cpp
    property_hook.cpp
    gl_spoof.cpp
    config_reader.cpp
)

target_include_directories(astraveil_spoof PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${DOBBY_DIR}/include
)

target_link_libraries(astraveil_spoof
    dobby
    log
    EGL
    GLESv2
)

# 剥离符号，减小体积 + 增加逆向难度
target_link_options(astraveil_spoof PRIVATE
    -s
    -Wl,--exclude-libs,ALL
)
