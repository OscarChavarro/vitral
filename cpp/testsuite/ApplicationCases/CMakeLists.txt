if(APPLE AND
   (NOT DEFINED SCENE_EDITOR_VARIANT OR
    SCENE_EDITOR_VARIANT MATCHES "^(xm|xaw)_opengl4$"))
    message(STATUS
        "SceneEditorApplication skipped in the aggregate macOS build: "
        "request gtk4_opengl4 with GTK 4.22 installed, or an opengl1 Xt "
        "variant, to build it explicitly.")
else()
    add_subdirectory(SceneEditorApplication)
endif()
