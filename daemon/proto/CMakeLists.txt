# ─────────────────────────────────────────────────────────────────────────────
# AstraVeil Daemon — Protobuf C++ 集成
#
# 此文件由 daemon/CMakeLists.txt 通过 add_subdirectory(proto) 引入。
# 生成 C++ lite 代码供 daemon 使用。
#
# 前置条件:
#   - protoc 编译器在 PATH 中（或通过 Protobuf_PROTOC_EXECUTABLE 指定）
#   - protobuf-lite C++ 库已安装（或通过 vcpkg/conan 获取）
#
# 用法:
#   在 daemon/CMakeLists.txt 中添加:
#     find_package(Protobuf REQUIRED)
#     add_subdirectory(proto)
#     target_link_libraries(astrad PRIVATE astra_proto)
# ─────────────────────────────────────────────────────────────────────────────

cmake_minimum_required(VERSION 3.22)

# ── 查找 protoc ──
find_program(PROTOC_EXECUTABLE
    NAMES protoc
    HINTS
        ${Protobuf_PROTOC_EXECUTABLE}
        $ENV{PROTOC_PATH}
        /usr/local/bin
        /usr/bin
)

if(NOT PROTOC_EXECUTABLE)
    message(WARNING "[AstraVeil] protoc 未找到，daemon 将使用 JSON fallback IPC。")
    set(ASTRA_DAEMON_HAVE_PROTOBUF OFF)
    return()
endif()

message(STATUS "[AstraVeil] protoc: ${PROTOC_EXECUTABLE}")
set(ASTRA_DAEMON_HAVE_PROTOBUF ON)

# ── Proto 源文件（与 :proto Gradle 模块共享同一套 .proto） ──
set(PROTO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../proto/src/main/proto")

set(PROTO_FILES
    ${PROTO_ROOT}/astra/common/v1/types.proto
    ${PROTO_ROOT}/astra/ipc/v1/ipc.proto
    ${PROTO_ROOT}/astra/policy/v1/policy.proto
    ${PROTO_ROOT}/astra/module/v1/module.proto
)

# ── 生成 C++ 代码 ──
set(PROTO_GEN_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
file(MAKE_DIRECTORY ${PROTO_GEN_DIR})

set(PROTO_SRCS "")
set(PROTO_HDRS "")

foreach(PROTO_FILE ${PROTO_FILES})
    # 计算相对路径以保持目录结构
    file(RELATIVE_PATH REL_PATH ${PROTO_ROOT} ${PROTO_FILE})
    get_filename_component(REL_DIR ${REL_PATH} DIRECTORY)
    get_filename_component(PROTO_NAME ${REL_PATH} NAME_WE)

    set(GEN_SRC "${PROTO_GEN_DIR}/${REL_DIR}/${PROTO_NAME}.pb.cc")
    set(GEN_HDR "${PROTO_GEN_DIR}/${REL_DIR}/${PROTO_NAME}.pb.h")

    # 确保输出子目录存在
    file(MAKE_DIRECTORY "${PROTO_GEN_DIR}/${REL_DIR}")

    add_custom_command(
        OUTPUT ${GEN_SRC} ${GEN_HDR}
        COMMAND ${PROTOC_EXECUTABLE}
            --proto_path=${PROTO_ROOT}
            --cpp_out=lite:${PROTO_GEN_DIR}
            ${PROTO_FILE}
        DEPENDS ${PROTO_FILE}
        COMMENT "[AstraVeil] Generating C++ for ${REL_PATH}"
        VERBATIM
    )

    list(APPEND PROTO_SRCS ${GEN_SRC})
    list(APPEND PROTO_HDRS ${GEN_HDR})
endforeach()

# ── 静态库目标 ──
add_library(astra_proto STATIC ${PROTO_SRCS})

target_include_directories(astra_proto PUBLIC ${PROTO_GEN_DIR})

target_link_libraries(astra_proto PUBLIC
    protobuf::libprotobuf-lite
)

target_compile_definitions(astra_proto PUBLIC
    GOOGLE_PROTOBUF_NO_RTTI
)

# 抑制生成代码的警告
target_compile_options(astra_proto PRIVATE -w)

message(STATUS "[AstraVeil] Protobuf C++ 集成已启用")
