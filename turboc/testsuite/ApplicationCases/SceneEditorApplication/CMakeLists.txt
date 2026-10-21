cmake_minimum_required(VERSION 2.4)
# C is enabled because FindOpenGL/FindX11 run C checks in CMake 2.4
project(SceneEditorApplication C CXX)

# Variant of the application, chosen when building: the widget set of its
# Xt GUI (each executable links either Motif or Athena, never both: they
# must not share a process). Unlike the cpp port, the turboc port only
# offers the OpenGL 1.2 fixed function drawing area (no OpenGL 4.1, no GTK).
#
# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires the base, opengl1, xt and xaw or xm libraries (see
# scripts/compile.sh).
set(SCENE_EDITOR_VARIANT "xm_opengl1" CACHE STRING
    "Variant: xm_opengl1 or xaw_opengl1")
if(SCENE_EDITOR_VARIANT STREQUAL "xm_opengl1")
    set(VITRAL_XT_WIDGET_SET "xm")
else(SCENE_EDITOR_VARIANT STREQUAL "xm_opengl1")
    if(SCENE_EDITOR_VARIANT STREQUAL "xaw_opengl1")
        set(VITRAL_XT_WIDGET_SET "xaw")
    else(SCENE_EDITOR_VARIANT STREQUAL "xaw_opengl1")
        message(FATAL_ERROR
            "SCENE_EDITOR_VARIANT must be xm_opengl1 or xaw_opengl1, not "
            "'${SCENE_EDITOR_VARIANT}'")
    endif(SCENE_EDITOR_VARIANT STREQUAL "xaw_opengl1")
endif(SCENE_EDITOR_VARIANT STREQUAL "xm_opengl1")
message(STATUS "SceneEditorApplication variant: ${SCENE_EDITOR_VARIANT}")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")
# The `String` type of Xt is renamed `XtString` around its headers (see
# XtIntrinsics.h): resource names as string literals, so their macros do
# not expand to casts to `String` where the vitral class is meant
add_definitions(-DXTSTRINGDEFINES -DXMSTRINGDEFINES)
if(APPLE)
    add_definitions(-DGL_SILENCE_DEPRECATION)
endif(APPLE)

set(EXECUTABLE_OUTPUT_PATH
    "${CMAKE_CURRENT_SOURCE_DIR}/build/${SCENE_EDITOR_VARIANT}")

set(VITRAL_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../..")

find_package(OpenGL)
if(NOT OPENGL_FOUND)
    message(FATAL_ERROR "OpenGL library not found")
endif(NOT OPENGL_FOUND)
find_package(X11)
if(NOT X11_FOUND)
    message(FATAL_ERROR "X11 development files not found")
endif(NOT X11_FOUND)
# CMake 2.4 FindX11 does not look for the X toolkit libraries
find_library(XT_LIBRARY Xt ${X11_LIB_SEARCH_PATH} /usr/X11R6/lib /usr/lib)
if(NOT XT_LIBRARY)
    message(FATAL_ERROR "libXt not found (i.e. libxt-dev)")
endif(NOT XT_LIBRARY)

# The GUI classes of the chosen widget set; each set defines createUiFactory
if(VITRAL_XT_WIDGET_SET STREQUAL "xm")
    find_package(Motif)
    if(NOT MOTIF_FOUND)
        message(FATAL_ERROR "Motif not found (i.e. libmotif-dev or lesstif2-dev)")
    endif(NOT MOTIF_FOUND)
    include_directories(${MOTIF_INCLUDE_DIR})
    # Motif before Xt: its VendorShell replaces the one of Xt only if the
    # dynamic linker finds it first
    set(SCENE_EDITOR_WIDGET_SET_LIBRARIES ${MOTIF_LIBRARIES} vitral_xm)
    set(SCENE_EDITOR_WIDGET_SET_SOURCES
        src/gui/xm/XmFileDialog.cpp
        src/gui/xm/XmUiFactory.cpp
    )
else(VITRAL_XT_WIDGET_SET STREQUAL "xm")
    find_library(XAW_LIBRARY NAMES Xaw7 Xaw PATHS ${X11_LIB_SEARCH_PATH} /usr/X11R6/lib /usr/lib)
    find_library(XMU_LIBRARY Xmu ${X11_LIB_SEARCH_PATH} /usr/X11R6/lib /usr/lib)
    if(NOT XAW_LIBRARY OR NOT XMU_LIBRARY)
        message(FATAL_ERROR "libXaw and libXmu not found (i.e. libxaw7-dev)")
    endif(NOT XAW_LIBRARY OR NOT XMU_LIBRARY)
    set(SCENE_EDITOR_WIDGET_SET_LIBRARIES vitral_xaw ${XAW_LIBRARY} ${XMU_LIBRARY})
    set(SCENE_EDITOR_WIDGET_SET_SOURCES
        src/gui/xaw/XawFileDialog.cpp
        src/gui/xaw/XawUiFactory.cpp
    )
endif(VITRAL_XT_WIDGET_SET STREQUAL "xm")

# The application classes and the OpenGL 1.2 rendering bridge (it defines
# the XtOpenGL1 classes the shared ones use, see XtSceneEditorOpenGLVariant.h)
set(SCENE_EDITOR_OPENGL_SOURCES
        src/application/XtOpenGL1GuiEventExecutor.cpp
        src/application/mcp/MCPJson.cpp
        src/application/mcp/MCPSceneTools.cpp
        src/application/mcp/MCPViewportTools.cpp
        src/application/mcp/XtOpenGL1MCPApplicationTools.cpp
        src/application/mcp/XtOpenGL1VitralEditorMCP.cpp
        src/application/mcp/XtOpenGL1VitralEditorMCPProtocol.cpp
        src/render/opengl1/OpenGL1DrawingAreaRenderer.cpp
        src/render/opengl1/OpenGL1FrameBufferSource.cpp
        src/render/opengl1/OpenGL1ProjectedViewRenderer.cpp
        src/render/opengl1/OpenGL1RenderPrimitiveRenderer.cpp
        src/render/opengl1/OpenGL1SceneRenderer.cpp
        src/render/opengl1/XtOpenGL1SceneBridge.cpp
        src/render/xlib/XlibDrawingAreaRenderer.cpp
    )

# The technology-independent application classes, the OpenGL rendering
# bridge ported from the Java application and its Xt GUI, built over the
# Xt layer of the toolkit (vitral_xt, as the Java one over `awt`), the
# widget set chosen (vitral_xm or vitral_xaw) and vitral_opengl1.
set(SCENE_EDITOR_SOURCES
    src/SceneEditorApplication.cpp
    src/application/commands/GeometryCreationCommandsExecutor.cpp
    src/application/commands/GuiEventExecutor.cpp
    src/application/TangibleInterfaceConnection.cpp
    src/gui/DrawingAreaInteractionTechniques.cpp
    src/gui/PointerCursor.cpp
    src/gui/PopupDismissClickFilter.cpp
    src/gui/SelectedBodyMapToggles.cpp
    src/gui/history/InteractionEditRecorder.cpp
    src/gui/history/UndoRedoInteractionTechnique.cpp
    src/gui/xt/XlibLabelImageProvider.cpp
    src/gui/xt/XtCollapsibleSection.cpp
    src/gui/xt/XtImageControlWindow.cpp
    src/gui/xt/XtSelectorDialog.cpp
    src/gui/xt/XtTangibleInterfaceDialog.cpp
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
    src/model/dialog/SceneHierarchyPresenter.cpp
    src/model/editor/FunctionalExplicitSurfaceEditor.cpp
    src/model/history/BodyTransformState.cpp
    src/model/history/CameraState.cpp
    src/model/history/CompositeOperation.cpp
    src/model/history/LightTransformState.cpp
    src/model/history/SceneElementKind.cpp
    src/model/history/SceneHistory.cpp
    src/model/history/SceneMembershipOperation.cpp
    src/model/history/SceneSelectionOperation.cpp
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
    ${SCENE_EDITOR_OPENGL_SOURCES}
    ${SCENE_EDITOR_WIDGET_SET_SOURCES}
)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${VITRAL_DIR}/base/src ${VITRAL_DIR}/base/src/main
    ${VITRAL_DIR}/opengl1/src/main
    ${VITRAL_DIR}/xt/src/main
    ${VITRAL_DIR}/${VITRAL_XT_WIDGET_SET}/src/main
    ${OPENGL_INCLUDE_DIR} ${X11_INCLUDE_DIR})
link_directories(
    ${VITRAL_DIR}/base/build ${VITRAL_DIR}/opengl1/build
    ${VITRAL_DIR}/xt/build ${VITRAL_DIR}/${VITRAL_XT_WIDGET_SET}/build)

add_executable(SceneEditorApplication ${SCENE_EDITOR_SOURCES})

# The widget set first (see above)
target_link_libraries(SceneEditorApplication
    ${SCENE_EDITOR_WIDGET_SET_LIBRARIES}
    vitral_xt
    vitral_opengl1
    vitral_base
    ${OPENGL_glu_LIBRARY} ${OPENGL_gl_LIBRARY}
    ${XT_LIBRARY} ${X11_LIBRARIES}
    pthread)
