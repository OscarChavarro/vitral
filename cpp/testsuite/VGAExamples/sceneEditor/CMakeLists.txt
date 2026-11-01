cmake_minimum_required(VERSION 3.10)
project(VitralVgaSceneEditor CXX)

# The Scene Editor of Vitral on the svgalib backend of the framebuffer GUI, for the text console of a Linux host:
# the sceneEditor program of the Vitral Kernel (copied here from x86_64_smp_kernel/src/main/programs/sceneEditor) is
# only a main that picks the graphics mode and hands the screen to FramebufferSceneEditor, compiled with the classes of
# its x11_framebuffer variant from where they live (../../ApplicationCases/SceneEditorApplication). Inside the kernel
# it uses the kernel's svgalib, readline and libc; here, the operating system's svgalib (-lvga -lvgagl), GNU readline
# and libc. See README.md of the parent folder.

if(NOT UNIX OR APPLE)
    message(FATAL_ERROR "sceneEditor needs svgalib, which only runs on Linux")
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

set(VITRAL_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../..")
set(VITRAL_BASE_DIR "${VITRAL_CPP_DIR}/base")
set(VITRAL_SVGALIB_DIR "${VITRAL_CPP_DIR}/svgalib" CACHE PATH "Vitral C++ svgalib backends")
set(VITRAL_VGA_PORTABLE_SOURCE_DIR "${VITRAL_CPP_DIR}/x86_64_smp_kernel/src/main" CACHE PATH
    "Path to the src directory containing lib/mouseCursor and lib/vgaModes")
set(SCENE_EDITOR_APPLICATION_SOURCE_DIR "${VITRAL_CPP_DIR}/testsuite/ApplicationCases/SceneEditorApplication"
    CACHE PATH "Vitral C++ Scene Editor application")
# The application reads ../../../../etc, so the program runs from the folder of its sources.
get_filename_component(SCENE_EDITOR_APPLICATION_DIR "${SCENE_EDITOR_APPLICATION_SOURCE_DIR}" ABSOLUTE)

# ---- svgalib (libvga and libvgagl) and GNU readline (used by lib/vgaModes) ----
find_path(SVGALIB_INCLUDE_DIR NAMES vga.h)
find_path(SVGALIB_MOUSE_INCLUDE_DIR NAMES vgamouse.h)
find_path(SVGALIB_GL_INCLUDE_DIR NAMES vgagl.h)
find_library(SVGALIB_LIBRARY NAMES vga)
find_library(SVGALIB_GL_LIBRARY NAMES vgagl)
find_path(READLINE_INCLUDE_DIR NAMES readline/readline.h)
find_library(READLINE_LIBRARY NAMES readline)
foreach(VITRAL_REQUIRED SVGALIB_INCLUDE_DIR SVGALIB_MOUSE_INCLUDE_DIR SVGALIB_GL_INCLUDE_DIR SVGALIB_LIBRARY
        SVGALIB_GL_LIBRARY READLINE_INCLUDE_DIR READLINE_LIBRARY)
    if(NOT ${VITRAL_REQUIRED})
        message(FATAL_ERROR "${VITRAL_REQUIRED} not found: install svgalib (vga.h, vgagl.h, libvga, libvgagl) and "
                            "the readline development package")
    endif()
endforeach()

# ---- The base library, its file next to the executable (cpp/base writes into cpp/base/build otherwise) ----
if(NOT TARGET vitral_base)
    set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
    set(WITH_JPEG ON CACHE BOOL "Enable JPEG image format support" FORCE)
    set(WITH_PNG ON CACHE BOOL "Enable PNG image format support" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
endif()
set_target_properties(vitral_base PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${CMAKE_RUNTIME_OUTPUT_DIRECTORY}")

# ---- The application classes of the x11_framebuffer variant, without its Xlib main ----
set(SCENE_EDITOR_APPLICATION_SOURCES
    application/framebuffer/FramebufferSceneEditor.cpp
    render/framebuffer/FramebufferDrawingAreaRenderer.cpp
    application/commands/GeometryCreationCommandsExecutor.cpp
    application/commands/GuiEventExecutor.cpp
    gui/DrawingAreaInteractionTechniques.cpp
    gui/PointerCursor.cpp
    gui/SelectedBodyMapToggles.cpp
    gui/history/InteractionEditRecorder.cpp
    gui/history/UndoRedoInteractionTechnique.cpp
    io/FileSuffixFilter.cpp
    io/GuiI18nContextBuilder.cpp
    io/GuiJsonReader.cpp
    model/ApplicationModel.cpp
    model/DrawingArea.cpp
    model/GuiState.cpp
    model/ProjectedViewsDebugPlan.cpp
    model/Scene.cpp
    model/SceneLightFactory.cpp
    model/dialog/SceneHierarchyPresenter.cpp
    model/editor/FunctionalExplicitSurfaceEditor.cpp
    model/history/BodyTransformState.cpp
    model/history/CameraState.cpp
    model/history/CompositeOperation.cpp
    model/history/LightTransformState.cpp
    model/history/SceneElementKind.cpp
    model/history/SceneHistory.cpp
    model/history/SceneMembershipOperation.cpp
    model/history/SceneSelectionOperation.cpp
    model/history/SceneSnapshot.cpp
    model/history/SceneTransformationOperation.cpp
    model/history/UndoQueue.cpp
    model/history/ViewportHistory.cpp
    model/history/ViewportState.cpp
    model/history/ViewportStateOperation.cpp
    model/selection/LightPicker.cpp
    model/selection/ScenePicker.cpp
    model/selection/SceneSelectionEditor.cpp
    model/selection/SelectionSet.cpp
    render/DrawingAreaGizmoPresenter.cpp
    render/FrameCaptureService.cpp
    render/ProjectedViewsDebugger.cpp
)
list(TRANSFORM SCENE_EDITOR_APPLICATION_SOURCES PREPEND "${SCENE_EDITOR_APPLICATION_SOURCE_DIR}/src/")

# SceneEditor.cpp includes "programs/sceneEditor/SceneEditor.h": this folder is published under that name, ahead of
# the kernel's src/main (needed for lib/vgaModes and lib/mouseCursor), which has the original program.
set(SCENE_EDITOR_GENERATED_INCLUDE_DIR "${CMAKE_CURRENT_BINARY_DIR}/include")
file(MAKE_DIRECTORY "${SCENE_EDITOR_GENERATED_INCLUDE_DIR}/programs")
if(NOT EXISTS "${SCENE_EDITOR_GENERATED_INCLUDE_DIR}/programs/sceneEditor")
    file(CREATE_LINK "${CMAKE_CURRENT_LIST_DIR}" "${SCENE_EDITOR_GENERATED_INCLUDE_DIR}/programs/sceneEditor" SYMBOLIC)
endif()

# SceneEditorProgram.cpp is the kernel's entry point (kernel/ArgList.h): not compiled on Linux.
add_executable(sceneEditor
    SceneEditor.cpp
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/vgaModes/VgaModes.cpp"
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/mouseCursor/MouseCursor.cpp"
    "${VITRAL_SVGALIB_DIR}/src/main/vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.cpp"
    "${VITRAL_SVGALIB_DIR}/src/main/vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.cpp"
    ${SCENE_EDITOR_APPLICATION_SOURCES}
)
set_source_files_properties(SceneEditor.cpp PROPERTIES
    COMPILE_DEFINITIONS "SCENE_EDITOR_APPLICATION_DIR=\"${SCENE_EDITOR_APPLICATION_DIR}\"")
# Not the kernel's src/main/lib/svgalib nor src/main/lib/readline/include: those stand for the system headers.
target_include_directories(sceneEditor BEFORE PRIVATE
    "${SCENE_EDITOR_GENERATED_INCLUDE_DIR}"
    "${SCENE_EDITOR_APPLICATION_SOURCE_DIR}/src")
target_include_directories(sceneEditor PRIVATE
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}"
    "${VITRAL_SVGALIB_DIR}/src/main"
    "${VITRAL_BASE_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${SVGALIB_INCLUDE_DIR}"
    "${SVGALIB_MOUSE_INCLUDE_DIR}"
    "${SVGALIB_GL_INCLUDE_DIR}"
    "${READLINE_INCLUDE_DIR}")
target_link_libraries(sceneEditor PRIVATE
    vitral_base
    "${SVGALIB_GL_LIBRARY}"
    "${SVGALIB_LIBRARY}"
    "${READLINE_LIBRARY}")
