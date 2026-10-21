# Part of testsuite/OpenGL1Examples/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

add_executable(ImageExample
    src/ImageExample.cpp
)

target_link_libraries(ImageExample ${VITRAL_OPENGL1_EXAMPLES_LIBRARIES})
