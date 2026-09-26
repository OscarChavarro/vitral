cmake_minimum_required(VERSION 3.10)
project(SceneEditorApplication CXX)

if(APPLE)
    message(FATAL_ERROR
        "SceneEditorApplication uses Xt + GLX + OpenGL 4.1 and is only "
        "supported on Linux/X11 with OpenGL 4.1-capable GLX drivers. "
        "XQuartz on macOS exposes only legacy OpenGL for this path.")
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)
find_package(X11 REQUIRED)

if(NOT X11_Xt_LIB OR NOT X11_Xaw_LIB)
    message(FATAL_ERROR "SceneEditorApplication requires libXt and libXaw from the X11 development packages.")
endif()

set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")
set(VITRAL_XT_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../xt")

if(NOT TARGET vitral_base)
    set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
    set(WITH_JPEG ON CACHE BOOL "Enable JPEG image format support" FORCE)
    set(WITH_PNG ON CACHE BOOL "Enable PNG image format support" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
endif()
if(NOT TARGET vitral_opengl4)
    add_subdirectory("${VITRAL_OPENGL4_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_opengl4")
endif()
if(NOT TARGET vitral_xt)
    add_subdirectory("${VITRAL_XT_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_xt")
endif()

# The technology-independent application classes, the OpenGL 4 rendering
# bridge ported from the Java application and its Xt GUI, built over the
# Xt/Xaw layer of the toolkit (vitral_xt, as the Java one over `awt`).
add_executable(SceneEditorApplication
    src/SceneEditorApplication.cpp
    src/application/GuiEventExecutor.cpp
    src/application/XtOpenGL4GuiEventExecutor.cpp
    src/application/mcp/XtOpenGL4VitralEditorMCP.cpp
    src/application/mcp/XtOpenGL4VitralEditorMCPProtocol.cpp
    src/gui/DrawingAreaInteractionTechniques.cpp
    src/gui/PointerCursor.cpp
    src/gui/PopupDismissClickFilter.cpp
    src/gui/SelectedBodyMapToggles.cpp
    src/gui/VisualRayDebugController.cpp
    src/gui/history/InteractionEditRecorder.cpp
    src/gui/history/UndoRedoInteractionTechnique.cpp
    src/gui/xt/XlibLabelImageProvider.cpp
    src/gui/xt/XtFileDialog.cpp
    src/gui/xt/XtImageControlWindow.cpp
    src/gui/xt/XtSelectorDialog.cpp
    src/gui/xt/XtModifyPanel.cpp
    src/gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.cpp
    src/io/FileSuffixFilter.cpp
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
    src/render/opengl4/OpenGL4DrawingAreaRenderer.cpp
    src/render/opengl4/OpenGL4FrameBufferSource.cpp
    src/render/opengl4/OpenGL4ProjectedViewRenderer.cpp
    src/render/opengl4/OpenGL4RenderPrimitiveRenderer.cpp
    src/render/opengl4/OpenGL4SceneRenderer.cpp
    src/render/opengl4/XtOpenGL4SceneBridge.cpp
    src/render/opengl4/OpenGL4VisualRayDebugRenderer.cpp
)

target_include_directories(SceneEditorApplication PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    "${VITRAL_XT_DIR}/src/main"
    ${X11_INCLUDE_DIR}
    "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4/src/glad/include"
)

target_link_libraries(SceneEditorApplication PRIVATE
    OpenGL::GL
    ${X11_Xt_LIB}
    ${X11_Xaw_LIB}
    ${X11_LIBRARIES}
    vitral_base
    vitral_opengl4
    vitral_xt
)
