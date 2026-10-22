# Makefile of SceneEditorApplication for legacy toolchains without CMake
# (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). It mirrors CMakeLists.txt:
# keep both in sync. Variants: VARIANT=xaw_opengl1 (Athena, the default) or
# VARIANT=xm_opengl1 (Motif, i.e. LessTif).
#
# Links against the static libraries of the port (base, opengl1, xt and
# xaw or xm, built by their own Makefiles), with the options of ../../../legacy.mk
# (i.e. GL_DIR for an OpenGL implementation outside the system folders).
# The executable is written to build/<variant>/, as the CMake build does, so
# runVariant.sh runs it.

VITRAL_DIR = ../../..
include $(VITRAL_DIR)/legacy.mk

VARIANT = xaw_opengl1
ifeq ($(VARIANT),xm_opengl1)
WIDGET_SET = xm
WIDGET_SET_LIBS = -L$(X11_DIR)/lib -lXm
WIDGET_SET_SOURCES = \
    src/gui/xm/XmFileDialog.cpp \
    src/gui/xm/XmUiFactory.cpp
else
WIDGET_SET = xaw
WIDGET_SET_LIBS = -L$(X11_DIR)/lib -lXaw -lXmu
WIDGET_SET_SOURCES = \
    src/gui/xaw/XawFileDialog.cpp \
    src/gui/xaw/XawUiFactory.cpp
endif

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc $(BASE_INCLUDES) -I$(VITRAL_DIR)/opengl1/src/main \
    -I$(VITRAL_DIR)/xt/src/main -I$(VITRAL_DIR)/$(WIDGET_SET)/src/main \
    $(GL_INCLUDES) $(X11_INCLUDES) $(X11_DEFINES) $(DEFINES)

# The technology-independent application classes, the OpenGL rendering
# bridge and the classes of the widget set
SOURCES = \
    src/SceneEditorApplication.cpp \
    src/application/commands/GeometryCreationCommandsExecutor.cpp \
    src/application/commands/GuiEventExecutor.cpp \
    src/application/TangibleInterfaceConnection.cpp \
    src/gui/DrawingAreaInteractionTechniques.cpp \
    src/gui/PointerCursor.cpp \
    src/gui/PopupDismissClickFilter.cpp \
    src/gui/SelectedBodyMapToggles.cpp \
    src/gui/history/InteractionEditRecorder.cpp \
    src/gui/history/UndoRedoInteractionTechnique.cpp \
    src/gui/xt/XlibLabelImageProvider.cpp \
    src/gui/xt/XtCollapsibleSection.cpp \
    src/gui/xt/XtImageControlWindow.cpp \
    src/gui/xt/XtSelectorDialog.cpp \
    src/gui/xt/XtTangibleInterfaceDialog.cpp \
    src/gui/xt/XtModifyPanel.cpp \
    src/gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.cpp \
    src/io/FileSuffixFilter.cpp \
    src/io/FolderListing.cpp \
    src/io/GuiI18nContextBuilder.cpp \
    src/io/GuiJsonReader.cpp \
    src/io/XmlManager.cpp \
    src/model/ApplicationModel.cpp \
    src/model/DrawingArea.cpp \
    src/model/GuiState.cpp \
    src/model/ProjectedViewsDebugPlan.cpp \
    src/model/Scene.cpp \
    src/model/SceneLightFactory.cpp \
    src/model/dialog/SceneHierarchyPresenter.cpp \
    src/model/editor/FunctionalExplicitSurfaceEditor.cpp \
    src/model/history/BodyTransformState.cpp \
    src/model/history/CameraState.cpp \
    src/model/history/CompositeOperation.cpp \
    src/model/history/EntityTransformState.cpp \
    src/model/history/LightTransformState.cpp \
    src/model/history/SceneElementKind.cpp \
    src/model/history/SceneHistory.cpp \
    src/model/history/SceneMembershipOperation.cpp \
    src/model/history/SceneSelectionOperation.cpp \
    src/model/history/SceneSnapshot.cpp \
    src/model/history/SceneTransformationOperation.cpp \
    src/model/history/UndoQueue.cpp \
    src/model/history/UndoableOperation.cpp \
    src/model/history/ViewportHistory.cpp \
    src/model/history/ViewportState.cpp \
    src/model/history/ViewportStateOperation.cpp \
    src/model/selection/LightPicker.cpp \
    src/model/selection/ScenePicker.cpp \
    src/model/selection/SceneSelectionEditor.cpp \
    src/model/selection/EntityListView.cpp \
    src/model/selection/SelectionSet.cpp \
    src/render/DrawingAreaGizmoPresenter.cpp \
    src/render/FrameCaptureService.cpp \
    src/render/ProjectedViewsDebugger.cpp \
    src/application/XtOpenGL1GuiEventExecutor.cpp \
    src/application/mcp/MCPJson.cpp \
    src/application/mcp/MCPSceneTools.cpp \
    src/application/mcp/MCPViewportTools.cpp \
    src/application/mcp/XtOpenGL1MCPApplicationTools.cpp \
    src/application/mcp/XtOpenGL1VitralEditorMCP.cpp \
    src/application/mcp/XtOpenGL1VitralEditorMCPProtocol.cpp \
    src/render/opengl1/OpenGL1DrawingAreaRenderer.cpp \
    src/render/opengl1/OpenGL1FrameBufferSource.cpp \
    src/render/opengl1/OpenGL1ProjectedViewRenderer.cpp \
    src/render/opengl1/OpenGL1RenderPrimitiveRenderer.cpp \
    src/render/opengl1/OpenGL1SceneRenderer.cpp \
    src/render/opengl1/XtOpenGL1SceneBridge.cpp \
    src/render/xlib/XlibDrawingAreaRenderer.cpp \
    $(WIDGET_SET_SOURCES)

# The widget set first, as in CMakeLists.txt (Motif before Xt: its
# VendorShell replaces the one of Xt only if found first)
TOOLKIT_LIBRARIES = \
    $(VITRAL_DIR)/$(WIDGET_SET)/build/libvitral_$(WIDGET_SET).a \
    $(VITRAL_DIR)/xt/build/libvitral_xt.a \
    $(VITRAL_DIR)/opengl1/build/libvitral_opengl1.a \
    $(BASE_LIBRARY)
LIBS = $(TOOLKIT_LIBRARIES) $(GL_LIBS) $(WIDGET_SET_LIBS) $(X11_LIBS) \
    $(SYSTEM_LIBS)

OBJECTS = $(patsubst %.cpp,buildmk/$(VARIANT)/%.o,$(SOURCES))
PROGRAM = build/$(VARIANT)/SceneEditorApplication

all: $(PROGRAM)

$(PROGRAM): $(OBJECTS) $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(OBJECTS) $(LIBS) $(EXTRA_LIBS)

buildmk/$(VARIANT)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -rf buildmk build/xaw_opengl1/SceneEditorApplication \
	    build/xm_opengl1/SceneEditorApplication

.PHONY: all clean
