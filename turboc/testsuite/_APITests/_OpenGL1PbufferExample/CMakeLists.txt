# Part of testsuite/_APITests/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")

# Color buffer of a GLUT window, read with glReadPixels
add_executable(_OpenGL1PbufferExampleGlut
    src/OpenGL1PbufferExample.cpp
)
target_link_libraries(_OpenGL1PbufferExampleGlut ${VITRAL_API_TESTS_LIBRARIES})

# Default variant: Mesa offscreen rendering when available (libOSMesa is a
# whole OpenGL implementation, so it must not link libGL, nor the toolkit
# modules that use it), else the GLUT one
add_executable(_OpenGL1PbufferExample
    src/OpenGL1PbufferExample.cpp
)
if(OSMESA_FOUND)
    set_target_properties(_OpenGL1PbufferExample PROPERTIES
        COMPILE_FLAGS "-DVITRAL_WITH_OSMESA")
    include_directories(${OSMESA_INCLUDE_DIR})
    target_link_libraries(_OpenGL1PbufferExample
        vitral_base ${OSMESA_LIBRARY} pthread)
else(OSMESA_FOUND)
    target_link_libraries(_OpenGL1PbufferExample ${VITRAL_API_TESTS_LIBRARIES})
endif(OSMESA_FOUND)
