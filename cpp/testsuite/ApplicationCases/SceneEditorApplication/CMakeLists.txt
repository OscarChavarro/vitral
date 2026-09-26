cmake_minimum_required(VERSION 3.10)
project(SceneEditorApplication CXX)

# Variant of the application, chosen when building: the widget set of its
# Xt GUI (each executable links either Motif or Athena, never both: they
# must not share a process) and the OpenGL version of its drawing area
# (OpenGL 4.1 core, or OpenGL 1.2 fixed function, without shaders)
set(SCENE_EDITOR_VARIANT "xm_opengl4" CACHE STRING
    "Variant: xm_opengl4, xaw_opengl4, xm_opengl1 or xaw_opengl1")
set_property(CACHE SCENE_EDITOR_VARIANT PROPERTY STRINGS
    xm_opengl4 xaw_opengl4 xm_opengl1 xaw_opengl1)
if(NOT SCENE_EDITOR_VARIANT MATCHES "^(xm|xaw)_(opengl4|opengl1)$")
    message(FATAL_ERROR
        "SCENE_EDITOR_VARIANT must be xm_opengl4, xaw_opengl4, xm_opengl1 "
        "or xaw_opengl1, not '${SCENE_EDITOR_VARIANT}'")
endif()
set(VITRAL_XT_WIDGET_SET "${CMAKE_MATCH_1}")
set(SCENE_EDITOR_OPENGL "${CMAKE_MATCH_2}")
message(STATUS "SceneEditorApplication variant: ${SCENE_EDITOR_VARIANT}")

if(APPLE AND SCENE_EDITOR_OPENGL STREQUAL "opengl4")
    message(FATAL_ERROR
        "SceneEditorApplication with Xt + GLX + OpenGL 4.1 is only supported "
        "on Linux/X11 with OpenGL 4.1-capable GLX drivers. XQuartz on macOS "
        "exposes only legacy OpenGL: build an opengl1 variant instead.")
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY
    "${CMAKE_CURRENT_LIST_DIR}/build/${SCENE_EDITOR_VARIANT}")

# On macOS the whole X11 stack (libX11, libXt, widget set, GLX libGL and
# libGLU) must come from one place: mixing XQuartz and Homebrew loads two
# libXt and two libX11 in the process, and gl* functions of OpenGL.framework
# crash without the CGL context that only the GLX of XQuartz makes current.
# Athena comes from XQuartz (Homebrew has no libXaw); Motif only from
# Homebrew, with its libX11 and its Mesa (libGL and mesa-glu).
# The libX11 of Homebrew looks for its locale modules as `*.so.2`, but
# installs them as `*.2.so`: without links with the expected names it
# supports no locale and no UTF-8 XFontSet can be created
if(APPLE)
    if(VITRAL_XT_WIDGET_SET STREQUAL "xaw" AND
       EXISTS "/opt/X11/lib/libX11.dylib")
        set(SCENE_EDITOR_X11_PREFIX "/opt/X11")
    else()
        set(SCENE_EDITOR_X11_PREFIX "/opt/homebrew")
    endif()
    list(INSERT CMAKE_PREFIX_PATH 0 "${SCENE_EDITOR_X11_PREFIX}")
    find_library(OPENGL_gl_LIBRARY NAMES GL
                 PATHS "${SCENE_EDITOR_X11_PREFIX}/lib" NO_DEFAULT_PATH)
    find_library(OPENGL_glu_LIBRARY NAMES GLU
                 PATHS "${SCENE_EDITOR_X11_PREFIX}/lib" NO_DEFAULT_PATH)
    if(NOT OPENGL_gl_LIBRARY OR NOT OPENGL_glu_LIBRARY)
        message(FATAL_ERROR
            "libGL (with GLX) and libGLU not found in "
            "${SCENE_EDITOR_X11_PREFIX}/lib: install XQuartz for xaw "
            "variants, or `brew install mesa mesa-glu` for xm variants")
    endif()
    set(CMAKE_FIND_FRAMEWORK NEVER)
endif()
find_package(OpenGL REQUIRED)
find_package(X11 REQUIRED)
unset(CMAKE_FIND_FRAMEWORK)

set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_OPENGL_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../${SCENE_EDITOR_OPENGL}")
set(VITRAL_XT_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../xt")
set(VITRAL_WIDGET_SET_DIR
    "${CMAKE_CURRENT_LIST_DIR}/../../../${VITRAL_XT_WIDGET_SET}")

if(NOT TARGET vitral_base)
    set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
    set(WITH_JPEG ON CACHE BOOL "Enable JPEG image format support" FORCE)
    set(WITH_PNG ON CACHE BOOL "Enable PNG image format support" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
endif()
if(NOT TARGET vitral_${SCENE_EDITOR_OPENGL})
    add_subdirectory("${VITRAL_OPENGL_DIR}"
                     "${CMAKE_CURRENT_BINARY_DIR}/vitral_${SCENE_EDITOR_OPENGL}")
endif()
if(NOT TARGET vitral_xt)
    add_subdirectory("${VITRAL_XT_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_xt")
endif()
if(NOT TARGET vitral_${VITRAL_XT_WIDGET_SET})
    add_subdirectory("${VITRAL_WIDGET_SET_DIR}"
                     "${CMAKE_CURRENT_BINARY_DIR}/vitral_${VITRAL_XT_WIDGET_SET}")
endif()

# The libraries of each variant go next to its executable: by default they
# share the build folder of their module, and the last variant built would
# replace the ones of the others, linked with another X11/OpenGL stack
set_target_properties(
    vitral_base vitral_xt vitral_${VITRAL_XT_WIDGET_SET}
    vitral_${SCENE_EDITOR_OPENGL}
    PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${CMAKE_RUNTIME_OUTPUT_DIRECTORY}")

# The GUI classes of the chosen widget set; each set defines createUiFactory
if(VITRAL_XT_WIDGET_SET STREQUAL "xm")
    set(SCENE_EDITOR_WIDGET_SET_SOURCES
        src/gui/xm/XmFileDialog.cpp
        src/gui/xm/XmUiFactory.cpp
    )
else()
    set(SCENE_EDITOR_WIDGET_SET_SOURCES
        src/gui/xaw/XawFileDialog.cpp
        src/gui/xaw/XawUiFactory.cpp
    )
endif()

# The application classes and the rendering bridge of the OpenGL version
# chosen, each one defines the Xt<OpenGL version> classes the shared ones use
# (see XtSceneEditorOpenGLVariant.h)
if(SCENE_EDITOR_OPENGL STREQUAL "opengl4")
    set(SCENE_EDITOR_OPENGL_SOURCES
        src/application/XtOpenGL4GuiEventExecutor.cpp
        src/application/mcp/XtOpenGL4VitralEditorMCP.cpp
        src/application/mcp/XtOpenGL4VitralEditorMCPProtocol.cpp
        src/render/opengl4/OpenGL4DrawingAreaRenderer.cpp
        src/render/opengl4/OpenGL4FrameBufferSource.cpp
        src/render/opengl4/OpenGL4ProjectedViewRenderer.cpp
        src/render/opengl4/OpenGL4RenderPrimitiveRenderer.cpp
        src/render/opengl4/OpenGL4SceneRenderer.cpp
        src/render/opengl4/XtOpenGL4SceneBridge.cpp
        src/render/opengl4/OpenGL4VisualRayDebugRenderer.cpp
    )
else()
    set(SCENE_EDITOR_OPENGL_SOURCES
        src/application/XtOpenGL1GuiEventExecutor.cpp
        src/application/mcp/XtOpenGL1VitralEditorMCP.cpp
        src/application/mcp/XtOpenGL1VitralEditorMCPProtocol.cpp
        src/render/opengl1/OpenGL1DrawingAreaRenderer.cpp
        src/render/opengl1/OpenGL1FrameBufferSource.cpp
        src/render/opengl1/OpenGL1ProjectedViewRenderer.cpp
        src/render/opengl1/OpenGL1RenderPrimitiveRenderer.cpp
        src/render/opengl1/OpenGL1SceneRenderer.cpp
        src/render/opengl1/XtOpenGL1SceneBridge.cpp
        src/render/opengl1/OpenGL1VisualRayDebugRenderer.cpp
    )
endif()

# The technology-independent application classes, the OpenGL rendering
# bridge ported from the Java application and its Xt GUI, built over the
# Xt layer of the toolkit (vitral_xt, as the Java one over `awt`), the
# widget set chosen (vitral_xm or vitral_xaw) and the OpenGL version chosen
# (vitral_opengl4 or vitral_opengl1).
add_executable(SceneEditorApplication
    src/SceneEditorApplication.cpp
    src/application/GuiEventExecutor.cpp
    src/gui/DrawingAreaInteractionTechniques.cpp
    src/gui/PointerCursor.cpp
    src/gui/PopupDismissClickFilter.cpp
    src/gui/SelectedBodyMapToggles.cpp
    src/gui/VisualRayDebugController.cpp
    src/gui/history/InteractionEditRecorder.cpp
    src/gui/history/UndoRedoInteractionTechnique.cpp
    src/gui/xt/XlibLabelImageProvider.cpp
    src/gui/xt/XtImageControlWindow.cpp
    src/gui/xt/XtSelectorDialog.cpp
    src/gui/xt/XtModifyPanel.cpp
    src/gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.cpp
    src/io/FileSuffixFilter.cpp
    src/io/FolderListing.cpp
    src/io/GuiI18nContextBuilder.cpp
    src/io/GuiJsonReader.cpp
    src/io/XmlManager.cpp
    src/model/ApplicationModel.cpp
    src/model/DrawingArea.cpp
    src/model/GuiState.cpp
    src/model/ProjectedViewsDebugPlan.cpp
    src/model/Scene.cpp
    src/model/SceneLightFactory.cpp
    src/model/editor/FunctionalExplicitSurfaceEditor.cpp
    src/model/history/BodyTransformState.cpp
    src/model/history/CameraState.cpp
    src/model/history/CompositeOperation.cpp
    src/model/history/LightTransformState.cpp
    src/model/history/SceneElementKind.cpp
    src/model/history/SceneHistory.cpp
    src/model/history/SceneMembershipOperation.cpp
    src/model/history/SceneSnapshot.cpp
    src/model/history/SceneTransformationOperation.cpp
    src/model/history/UndoQueue.cpp
    src/model/history/ViewportHistory.cpp
    src/model/history/ViewportState.cpp
    src/model/history/ViewportStateOperation.cpp
    src/model/selection/LightPicker.cpp
    src/model/selection/ScenePicker.cpp
    src/model/selection/SceneSelectionEditor.cpp
    src/model/selection/SelectionSet.cpp
    src/render/DrawingAreaGizmoPresenter.cpp
    src/render/FrameCaptureService.cpp
    src/render/ProjectedViewsDebugger.cpp
    src/render/VisualRayDebugGeometry.cpp
    ${SCENE_EDITOR_OPENGL_SOURCES}
    ${SCENE_EDITOR_WIDGET_SET_SOURCES}
)

if(SCENE_EDITOR_OPENGL STREQUAL "opengl1")
    target_compile_definitions(SceneEditorApplication PRIVATE
        VITRAL_SCENE_EDITOR_OPENGL1)
    if(APPLE)
        target_compile_definitions(SceneEditorApplication PRIVATE
            GL_SILENCE_DEPRECATION)
    endif()
endif()

target_include_directories(SceneEditorApplication PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_OPENGL_DIR}/src/main"
    "${VITRAL_XT_DIR}/src/main"
    "${VITRAL_WIDGET_SET_DIR}/src/main"
    ${X11_INCLUDE_DIR}
)
if(SCENE_EDITOR_OPENGL STREQUAL "opengl4")
    target_include_directories(SceneEditorApplication PRIVATE
        "${VITRAL_OPENGL_DIR}/src/glad/include")
endif()

# The widget set first. Motif must come before Xt in the libraries of the
# executable: its VendorShell replaces the one of Xt only if the dynamic
# linker finds it first
if(VITRAL_XT_WIDGET_SET STREQUAL "xm")
    find_package(Motif REQUIRED)
    set(SCENE_EDITOR_WIDGET_SET_LIBRARIES ${MOTIF_LIBRARIES})
endif()
target_link_libraries(SceneEditorApplication PRIVATE
    ${SCENE_EDITOR_WIDGET_SET_LIBRARIES}
    vitral_${VITRAL_XT_WIDGET_SET}
    vitral_xt
    OpenGL::GL
    ${X11_LIBRARIES}
    vitral_base
    vitral_${SCENE_EDITOR_OPENGL}
)
