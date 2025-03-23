cmake_minimum_required(VERSION 3.30)
project(jvmti4j_native C)

set(CMAKE_C_STANDARD 11)
set(INCLUDE_DIR "${PROJECT_SOURCE_DIR}/include")
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:DebugDLL>")

include_directories(
        ${INCLUDE_DIR}
        ${INCLUDE_DIR}/jni8
        ${INCLUDE_DIR}/jni8/win32
)

add_library(jvmti4j_native SHARED src/library.c)
