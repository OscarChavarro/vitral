# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

# Part of testsuite/OpenGL1Examples/CMakeLists.txt (CMake 2.4 syntax)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

add_executable(MeshExample
    src/MeshExample.cpp
    src/animation/AnimationController.cpp
    src/gui/MeshKeyboardInteractionTechniques.cpp
    src/gui/MeshMouseInteractionTechniques.cpp
    src/gui/TangibleInterfaceInteractionTechniques.cpp
    src/model/MeshModel.cpp
    src/options/CommandLineOptions.cpp
    src/render/OpenGL1DebuggerRenderer.cpp
)

target_link_libraries(MeshExample ${VITRAL_OPENGL1_EXAMPLES_LIBRARIES})
