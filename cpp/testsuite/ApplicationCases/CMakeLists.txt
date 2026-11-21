# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

if(APPLE AND
   (NOT DEFINED SCENE_EDITOR_VARIANT OR
    SCENE_EDITOR_VARIANT MATCHES "^(xm|xaw)_opengl4$"))
    message(STATUS
        "SceneEditorApplication skipped in the aggregate macOS build: "
        "request gtk4_opengl4 with GTK 4.6 or newer installed, or an opengl1 Xt "
        "variant, to build it explicitly.")
else()
    add_subdirectory(SceneEditorApplication)
endif()
