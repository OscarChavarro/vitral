# Part of testsuite/OpenGL1Examples/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

add_executable(PolygonClippingExample
    src/PolygonClippingExample.cpp
    src/model/PolygonClippingFixtures.cpp
    src/options/CommandLineOptions.cpp
    src/render/PolygonClippingHudRenderer.cpp
    src/render/PolygonTriangularRenderer.cpp
)

target_link_libraries(PolygonClippingExample ${VITRAL_OPENGL1_EXAMPLES_LIBRARIES})
