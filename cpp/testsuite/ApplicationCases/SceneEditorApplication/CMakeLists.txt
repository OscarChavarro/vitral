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
find_package(GLEW REQUIRED)
find_package(X11 REQUIRED)
find_library(VITRAL_GLEW_LIB NAMES GLEW glew REQUIRED)

if(NOT X11_Xt_LIB)
    message(FATAL_ERROR "SceneEditorApplication requires libXt from the X11 development packages.")
endif()

set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")

if(NOT TARGET vitral_base)
    set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
    set(WITH_JPEG ON CACHE BOOL "Enable JPEG image format support" FORCE)
    set(WITH_PNG ON CACHE BOOL "Enable PNG image format support" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
endif()
if(NOT TARGET vitral_opengl4)
    add_subdirectory("${VITRAL_OPENGL4_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_opengl4")
endif()

# The technology-independent application classes and the OpenGL 4 rendering
# bridge ported from the Java application.  The bridge is deliberately not
# instantiated by the current Xt/GLX shell: it is compiled and ready for the
# future GUI integration, without changing the application's current path.
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
    src/io/FileSuffixFilter.cpp
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
    src/render/opengl4/OpenGL4VisualRayDebugRenderer.cpp
)

target_include_directories(SceneEditorApplication PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    ${X11_INCLUDE_DIR}
    ${GLEW_INCLUDE_DIRS}
)

target_link_libraries(SceneEditorApplication PRIVATE
    OpenGL::GL
    ${VITRAL_GLEW_LIB}
    ${X11_Xt_LIB}
    ${X11_LIBRARIES}
    vitral_base
    vitral_opengl4
)
