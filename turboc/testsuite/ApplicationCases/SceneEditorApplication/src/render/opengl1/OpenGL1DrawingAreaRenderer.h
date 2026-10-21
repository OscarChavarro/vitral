#ifndef __SCENE_EDITOR_OPEN_GL_1_DRAWING_AREA_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_1_DRAWING_AREA_RENDERER__
#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"
#include "render/DrawingAreaGizmoPresenter.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ViewportSetRenderer.h"
class ApplicationModel;class DrawingArea;class DrawingAreaHost;class FrameCaptureService;class OpenGL1LabelImageProvider;class OpenGL1SimpleCorridorSample;class ProjectedViewsDebugger;class RotateGizmo;class ScaleGizmo;class Scene;class SceneSelectionEditor;class TranslateGizmo;class Viewport;
class OpenGL1DrawingAreaRenderer:private OpenGL1ViewportSetRenderer::ViewRenderer{Scene*scene;ApplicationModel*model;DrawingArea*drawingArea;DrawingAreaHost*host;SceneSelectionEditor*selectionEditor;DrawingAreaGizmoPresenter*gizmoPresenter;DrawingAreaGizmoPresenter::GizmoKind gizmoDrawn;FrameCaptureService*frameCapture;ProjectedViewsDebugger*projectedDebugger;OpenGL1ViewportSetRenderer*viewportRenderer;OpenGL1SimpleCorridorSample*corridor;Consumer<Viewport*>*selectedListener;void configureView(OpenGL1ViewportWindow*);void drawView(OpenGL1ViewportWindow*);void drawGizmos();public:OpenGL1DrawingAreaRenderer(ApplicationModel*,DrawingAreaHost*,OpenGL1LabelImageProvider*,Consumer<Viewport*>*,TranslateGizmo*,RotateGizmo*,ScaleGizmo*);~OpenGL1DrawingAreaRenderer();void display(int surfaceWidth,int surfaceHeight);void init();void dispose();void reshape(int width,int height);};
#endif
