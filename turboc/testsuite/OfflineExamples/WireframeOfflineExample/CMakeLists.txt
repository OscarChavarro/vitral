# Part of testsuite/OfflineExamples/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

add_executable(WireframeOfflineExample
    src/WireframeOfflineExample.cpp
)

target_link_libraries(WireframeOfflineExample ${VITRAL_OFFLINE_EXAMPLES_LIBRARIES})
