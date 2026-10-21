# Part of testsuite/_APITests/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")

add_executable(_OpenGL1HelloWorld
    src/HelloWorldOpenGL1.cpp
)

target_link_libraries(_OpenGL1HelloWorld ${VITRAL_API_TESTS_LIBRARIES})
