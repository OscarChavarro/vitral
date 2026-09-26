#ifndef __SCENE_EDITOR_OPEN_GL_1_DRAWING_AREA_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_1_DRAWING_AREA_RENDERER__
#include <functional>
#include "render/DrawingAreaGizmoPresenter.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ViewportSetRenderer.h"
class ApplicationModel;class DrawingArea;class DrawingAreaHost;class FrameCaptureService;class OpenGL1LabelImageProvider;class OpenGL1SimpleCorridorSample;class OpenGL1VisualRayDebugRenderer;class ProjectedViewsDebugger;class RotateGizmo;class ScaleGizmo;class Scene;class SceneSelectionEditor;class TranslateGizmo;class Viewport;
class OpenGL1DrawingAreaRenderer:private OpenGL1ViewportSetRenderer::ViewRenderer{Scene*scene;ApplicationModel*model;DrawingArea*drawingArea;DrawingAreaHost*host;SceneSelectionEditor*selectionEditor;DrawingAreaGizmoPresenter*gizmoPresenter;DrawingAreaGizmoPresenter::GizmoKind gizmoDrawn;OpenGL1VisualRayDebugRenderer*rayRenderer;FrameCaptureService*frameCapture;ProjectedViewsDebugger*projectedDebugger;OpenGL1ViewportSetRenderer*viewportRenderer;OpenGL1SimpleCorridorSample*corridor;std::function<void(Viewport*)>selectedListener;void configureView(OpenGL1ViewportWindow*)override;void drawView(OpenGL1ViewportWindow*)override;void drawGizmos();public:OpenGL1DrawingAreaRenderer(ApplicationModel*,DrawingAreaHost*,OpenGL1LabelImageProvider*,const std::function<void(Viewport*)>&,TranslateGizmo*,RotateGizmo*,ScaleGizmo*);~OpenGL1DrawingAreaRenderer();void display(int surfaceWidth,int surfaceHeight);void init();void dispose();void reshape(int width,int height);};
#endif
