cmake_minimum_required(VERSION 3.10)
project(SceneEditorApplication CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_library(VITRAL_GLEW_LIB NAMES GLEW glew REQUIRED)

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

# Entry point and pure application classes: the ones of the Java port that
# depend neither on AWT nor on JOGL (packages model, io, render, gui and the
# GuiEventExecutor of application). Nothing uses them yet: they are ready for
# the Xt + GLX + OpenGL 4 GUI.
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
)

target_include_directories(SceneEditorApplication PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    ${GLEW_INCLUDE_DIRS}
)

target_link_libraries(SceneEditorApplication PRIVATE
    OpenGL::GL
    ${VITRAL_GLEW_LIB}
    vitral_base
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(SceneEditorApplication PRIVATE GL_SILENCE_DEPRECATION)

    option(VITRAL_SIGN_EXECUTABLES "Sign macOS executables after build" ON)
    set(VITRAL_CODESIGN_IDENTITY "-" CACHE STRING "codesign identity used for macOS executable signing")

    if(VITRAL_SIGN_EXECUTABLES)
        find_program(CODESIGN_EXECUTABLE codesign)
        if(CODESIGN_EXECUTABLE)
            add_custom_command(TARGET SceneEditorApplication POST_BUILD
                COMMAND ${CODESIGN_EXECUTABLE}
                    --force
                    --sign "${VITRAL_CODESIGN_IDENTITY}"
                    --timestamp=none
                    "$<TARGET_FILE:SceneEditorApplication>"
                COMMENT "Signing SceneEditorApplication with identity '${VITRAL_CODESIGN_IDENTITY}'"
                VERBATIM)
        else()
            message(WARNING "codesign tool not found; SceneEditorApplication will not be signed")
        endif()
    endif()
endif()
