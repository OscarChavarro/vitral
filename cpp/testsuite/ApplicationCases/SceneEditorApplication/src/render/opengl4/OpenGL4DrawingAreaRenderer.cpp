#include <GL/glew.h>
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameCaptureService.h"
#include "render/ProjectedViewsDebugger.h"
#include "render/opengl4/OpenGL4DrawingAreaRenderer.h"
#include "render/opengl4/OpenGL4FrameBufferSource.h"
#include "render/opengl4/OpenGL4ProjectedViewRenderer.h"
#include "render/opengl4/OpenGL4SceneRenderer.h"
#include "render/opengl4/OpenGL4VisualRayDebugRenderer.h"
#include "vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColorDepthImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4Renderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ViewportWindow.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4RotateGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4ScaleGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4TranslateGizmoRenderer.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"
OpenGL4DrawingAreaRenderer::OpenGL4DrawingAreaRenderer(ApplicationModel*m,DrawingAreaHost*h,OpenGL4LabelImageProvider*l,const std::function<void(Viewport*)>&f,TranslateGizmo*t,RotateGizmo*r,ScaleGizmo*s):scene(m->getScene()),model(m),drawingArea(m->getDrawingArea()),host(h),selectionEditor(new SceneSelectionEditor(scene)),gizmoPresenter(new DrawingAreaGizmoPresenter(drawingArea,selectionEditor,t,r,s)),gizmoDrawn(DrawingAreaGizmoPresenter::GizmoKind::NONE),rayRenderer(new OpenGL4VisualRayDebugRenderer(m)),frameCapture(new FrameCaptureService(m,h)),projectedDebugger(new ProjectedViewsDebugger(m,h)),viewportRenderer(0),corridor(new OpenGL4SimpleCorridorSample()),selectedListener(f),labels(l){viewportRenderer=new OpenGL4ViewportSetRenderer(drawingArea->getViewportSet(),l,this);}
OpenGL4DrawingAreaRenderer::~OpenGL4DrawingAreaRenderer(){delete viewportRenderer;delete corridor;delete projectedDebugger;delete frameCapture;delete rayRenderer;delete gizmoPresenter;delete selectionEditor;}
void OpenGL4DrawingAreaRenderer::configureView(OpenGL4ViewportWindow*w){if(selectedListener)selectedListener(w->getViewport());}
void OpenGL4DrawingAreaRenderer::drawGizmos(){gizmoDrawn=gizmoPresenter->prepareForView(scene->activeCamera);glClear(GL_DEPTH_BUFFER_BIT);if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE)OpenGL4TranslateGizmoRenderer::draw(gizmoPresenter->getTranslationGizmo(),scene->activeCamera);else if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::ROTATE)OpenGL4RotateGizmoRenderer::draw(gizmoPresenter->getRotateGizmo(),scene->activeCamera);else if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::SCALE)OpenGL4ScaleGizmoRenderer::draw(gizmoPresenter->getScaleGizmo(),scene->activeCamera);glEnable(GL_DEPTH_TEST);}
void OpenGL4DrawingAreaRenderer::drawView(OpenGL4ViewportWindow*w){if(!w->isActive())return;scene->activeCamera=w->getCamera();scene->qualityTemplate->cloneFrom(*w->getRendererConfiguration());if(w->getRenderMode()==OpenGL4ViewportWindow::RENDER_MODE_ZBUFFER){OpenGL4SceneRenderer::draw(scene,host->getBodyEditFeedbackProvider());if(scene->showCorridor){Matrix4x4d p=scene->activeCamera->calculateProjectionMatrix();float*m=p.exportToFloatArrayColumnOrder();corridor->drawGL(m,p);delete[]m;glDisable(GL_CULL_FACE);}}else{scene->activateSelectedBackground();model->setRaytracedImageWidth(w->getViewportSizeX());model->setRaytracedImageHeight(w->getViewportSizeY());host->raytraceImage();OpenGL4ColorDepthImageRenderer::draw(model->getRaytracedImage(),model->getRaytracedDepth());OpenGL4SceneRenderer::drawEditorOverlays(scene,host->getBodyEditFeedbackProvider());}rayRenderer->draw();w->drawGrid();OpenGL4FrameBufferSource buffer;frameCapture->copyZBufferIfNeeded(&buffer);drawGizmos();frameCapture->copyColorBufferIfNeeded(&buffer,w->isSelected());w->drawReferenceBase();if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE)w->drawLabelsForTranslateGizmo(gizmoPresenter->getTranslationGizmo());if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::ROTATE)w->drawLabelForRotateGizmoArc(gizmoPresenter->getRotateGizmo());if(gizmoPresenter->getInputGizmo()&&w->isSelected())w->drawInputGizmo(gizmoPresenter->getInputGizmo());drawInteractionModeHud(w);}
void OpenGL4DrawingAreaRenderer::display(int w,int h){host->beforeFrame();OpenGL4ProjectedViewRenderer projected;projectedDebugger->debugIfNeeded(&projected);drawingArea->updateSurfaceSize(w,h);ViewportSet*s=drawingArea->getViewportSet();glViewport(0,0,s->getSizeXInPixels(),s->getSizeYInPixels());glClearColor(.77f,.77f,.77f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glEnable(GL_DEPTH_TEST);viewportRenderer->draw(host->isFullScreenGuiMode());if(frameCapture->isFrameExportPending()){glViewport(0,0,s->getSizeXInPixels(),s->getSizeYInPixels());OpenGL4FrameBufferSource buffer;frameCapture->exportPendingFrame(&buffer);}}
void OpenGL4DrawingAreaRenderer::init(){viewportRenderer->invalidateGlResources();}void OpenGL4DrawingAreaRenderer::dispose(){viewportRenderer->disposeGlResources();corridor->dispose();OpenGL4Renderer::disposeAll();}void OpenGL4DrawingAreaRenderer::reshape(int w,int h){host->beforeFrame();drawingArea->updateSurfaceSize(w,h);}

void OpenGL4DrawingAreaRenderer::drawInteractionModeHud(OpenGL4ViewportWindow*w)
{
    Widget* context = model->getI18nContext();
    if ( labels == nullptr || context == nullptr ) {
        return;
    }
    const char* commandId = "IDC_TOOLS_CAMERA";
    switch ( drawingArea->getInteractionMode() ) {
      case InteractionMode::SELECT: commandId = "IDC_TOOLS_SELECT"; break;
      case InteractionMode::TRANSLATE: commandId = "IDC_TOOLS_TRANSLATE"; break;
      case InteractionMode::ROTATE: commandId = "IDC_TOOLS_ROTATE"; break;
      case InteractionMode::SCALE: commandId = "IDC_TOOLS_SCALE"; break;
      default: break;
    }
    WidgetCommand* command = context->getCommandByName(commandId);
    if ( command == nullptr ) {
        return;
    }
    ViewportSet* set = drawingArea->getViewportSet();
    ViewportElementScaler* scaler = set->getElementScaler();
    RGBAImageUncompressed* image = labels->createLabelImage(
        command->getName(), set->getTitleColorFor(w->getViewport()),
        scaler->scaleSize(OpenGL4LabelImageProvider::DEFAULT_FONT_SIZE));
    if ( image == nullptr ) {
        return;
    }
    // Second line of the HUD: just below the title (see
    // OpenGL4ViewportWindow::drawTitle), with the same left margin
    int gap = scaler->scaleSize(2);
    int x = w->getViewportStartX() + 4;
    int y = w->getViewportStartY() + w->getViewportSizeY() - 1 -
        w->getViewport()->getTitleAreaSizeY() - gap - image->getYSize();
    if ( w->getViewport()->getTitleAreaSizeY() <= 0 ) {
        y -= image->getYSize() + gap;
    }
    w->drawLabel(image, x, y);
    OpenGL4ImageRenderer::unload(image);
    delete image;
}
