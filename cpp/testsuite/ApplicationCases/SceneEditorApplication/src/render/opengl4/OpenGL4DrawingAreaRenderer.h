#ifndef __SCENE_EDITOR_OPEN_GL_4_DRAWING_AREA_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_4_DRAWING_AREA_RENDERER__
#include <functional>
#include "render/DrawingAreaGizmoPresenter.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ViewportSetRenderer.h"
class ApplicationModel;class DrawingArea;class DrawingAreaHost;class FrameCaptureService;class OpenGL4LabelImageProvider;class OpenGL4SimpleCorridorSample;class OpenGL4VisualRayDebugRenderer;class ProjectedViewsDebugger;class RotateGizmo;class ScaleGizmo;class Scene;class SceneSelectionEditor;class TranslateGizmo;class Viewport;
class OpenGL4DrawingAreaRenderer:private OpenGL4ViewportSetRenderer::ViewRenderer{Scene*scene;ApplicationModel*model;DrawingArea*drawingArea;DrawingAreaHost*host;SceneSelectionEditor*selectionEditor;DrawingAreaGizmoPresenter*gizmoPresenter;DrawingAreaGizmoPresenter::GizmoKind gizmoDrawn;OpenGL4VisualRayDebugRenderer*rayRenderer;FrameCaptureService*frameCapture;ProjectedViewsDebugger*projectedDebugger;OpenGL4ViewportSetRenderer*viewportRenderer;OpenGL4SimpleCorridorSample*corridor;std::function<void(Viewport*)>selectedListener;void configureView(OpenGL4ViewportWindow*)override;void drawView(OpenGL4ViewportWindow*)override;void drawGizmos();public:OpenGL4DrawingAreaRenderer(ApplicationModel*,DrawingAreaHost*,OpenGL4LabelImageProvider*,const std::function<void(Viewport*)>&,TranslateGizmo*,RotateGizmo*,ScaleGizmo*);~OpenGL4DrawingAreaRenderer();void display(int surfaceWidth,int surfaceHeight);void init();void dispose();void reshape(int width,int height);};
#endif
