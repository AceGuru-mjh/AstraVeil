cmake_minimum_required(VERSION 3.18)
project(astraveil_spoof CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(DOBBY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/external/Dobby)
set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
add_subdirectory(${DOBBY_DIR} dobby_build)

add_library(astraveil_spoof SHARED
    src/entry.cpp
    src/property_hook.cpp
    src/gl_spoof.cpp
    src/config_reader.cpp
)

target_include_directories(astraveil_spoof PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${DOBBY_DIR}/include
)

target_link_libraries(astraveil_spoof dobby log EGL GLESv2)

target_link_options(astraveil_spoof PRIVATE -s -Wl,--exclude-libs,ALL)
