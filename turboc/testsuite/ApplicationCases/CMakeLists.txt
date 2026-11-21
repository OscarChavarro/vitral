# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

# SceneEditorApplication of the turboc port: only its Xt + OpenGL 1.2
# variants (xm_opengl1, xaw_opengl1), chosen with SCENE_EDITOR_VARIANT. It
# is usually built alone with its launchers (see its README.md).
add_subdirectory(SceneEditorApplication)
