#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameCaptureService.h"
#include "render/ProjectedViewsDebugger.h"
#include "render/opengl1/OpenGL1DrawingAreaRenderer.h"
#include "render/opengl1/OpenGL1FrameBufferSource.h"
#include "render/opengl1/OpenGL1ProjectedViewRenderer.h"
#include "render/opengl1/OpenGL1SceneRenderer.h"
#include "vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColorDepthImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Renderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ViewportWindow.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1RotateGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1ScaleGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1TranslateGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LabelImageProvider.h"
OpenGL1DrawingAreaRenderer::OpenGL1DrawingAreaRenderer(ApplicationModel*m,DrawingAreaHost*h,OpenGL1LabelImageProvider*l,const std::function<void(Viewport*)>&f,TranslateGizmo*t,RotateGizmo*r,ScaleGizmo*s):scene(m->getScene()),model(m),drawingArea(m->getDrawingArea()),host(h),selectionEditor(new SceneSelectionEditor(scene)),gizmoPresenter(new DrawingAreaGizmoPresenter(drawingArea,selectionEditor,t,r,s)),gizmoDrawn(DrawingAreaGizmoPresenter::GizmoKind::NONE),frameCapture(new FrameCaptureService(m,h)),projectedDebugger(new ProjectedViewsDebugger(m,h)),viewportRenderer(0),corridor(new OpenGL1SimpleCorridorSample()),selectedListener(f){viewportRenderer=new OpenGL1ViewportSetRenderer(drawingArea->getViewportSet(),l,this);}
OpenGL1DrawingAreaRenderer::~OpenGL1DrawingAreaRenderer(){delete viewportRenderer;delete corridor;delete projectedDebugger;delete frameCapture;delete gizmoPresenter;delete selectionEditor;}
void OpenGL1DrawingAreaRenderer::configureView(OpenGL1ViewportWindow*w){if(selectedListener)selectedListener(w->getViewport());}
void OpenGL1DrawingAreaRenderer::drawGizmos(){gizmoDrawn=gizmoPresenter->prepareForView(scene->activeCamera);glClear(GL_DEPTH_BUFFER_BIT);if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE)OpenGL1TranslateGizmoRenderer::draw(gizmoPresenter->getTranslationGizmo(),scene->activeCamera);else if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::ROTATE)OpenGL1RotateGizmoRenderer::draw(gizmoPresenter->getRotateGizmo(),scene->activeCamera);else if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::SCALE)OpenGL1ScaleGizmoRenderer::draw(gizmoPresenter->getScaleGizmo(),scene->activeCamera);glEnable(GL_DEPTH_TEST);}
void OpenGL1DrawingAreaRenderer::drawView(OpenGL1ViewportWindow*w){if(!w->isActive())return;scene->activeCamera=w->getCamera();scene->qualityTemplate->cloneFrom(*w->getRendererConfiguration());if(w->getRenderMode()==OpenGL1ViewportWindow::RENDER_MODE_ZBUFFER){OpenGL1SceneRenderer::draw(scene,host->getBodyEditFeedbackProvider());if(scene->showCorridor){Matrix4x4d p=scene->activeCamera->calculateProjectionMatrix();float*m=p.exportToFloatArrayColumnOrder();corridor->drawGL(m,p);delete[]m;glDisable(GL_CULL_FACE);}}else{scene->activateSelectedBackground();model->setRaytracedImageWidth(w->getViewportSizeX());model->setRaytracedImageHeight(w->getViewportSizeY());host->raytraceImage();OpenGL1ColorDepthImageRenderer::draw(model->getRaytracedImage(),model->getRaytracedDepth());OpenGL1SceneRenderer::drawEditorOverlays(scene,host->getBodyEditFeedbackProvider());}model->getRayGizmo()->acquireSnapshot();OpenGL1RayGizmoRenderer::draw(model->getRayGizmo(),scene->activeCamera,scene->scene->getLights());w->drawGrid();OpenGL1FrameBufferSource buffer;frameCapture->copyZBufferIfNeeded(&buffer);drawGizmos();frameCapture->copyColorBufferIfNeeded(&buffer,w->isSelected());w->drawReferenceBase();if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE)w->drawLabelsForTranslateGizmo(gizmoPresenter->getTranslationGizmo());if(gizmoDrawn==DrawingAreaGizmoPresenter::GizmoKind::ROTATE)w->drawLabelForRotateGizmoArc(gizmoPresenter->getRotateGizmo());InputGizmo*inputGizmo=gizmoPresenter->getInputGizmo();if(inputGizmo==0&&model->getRayGizmo()->isVisible())inputGizmo=model->getRayGizmo()->getInputGizmo();if(inputGizmo&&w->isSelected())w->drawInputGizmo(inputGizmo);}
void OpenGL1DrawingAreaRenderer::display(int w,int h){host->beforeFrame();OpenGL1ProjectedViewRenderer projected;projectedDebugger->debugIfNeeded(&projected);drawingArea->updateSurfaceSize(w,h);ViewportSet*s=drawingArea->getViewportSet();glViewport(0,0,s->getSizeXInPixels(),s->getSizeYInPixels());glClearColor(.77f,.77f,.77f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glEnable(GL_DEPTH_TEST);viewportRenderer->draw(host->isFullScreenGuiMode());if(frameCapture->isFrameExportPending()){glViewport(0,0,s->getSizeXInPixels(),s->getSizeYInPixels());OpenGL1FrameBufferSource buffer;frameCapture->exportPendingFrame(&buffer);}}
void OpenGL1DrawingAreaRenderer::init(){viewportRenderer->invalidateGlResources();}void OpenGL1DrawingAreaRenderer::dispose(){viewportRenderer->disposeGlResources();corridor->dispose();OpenGL1RayGizmoRenderer::dispose();OpenGL1Renderer::disposeAll();}void OpenGL1DrawingAreaRenderer::reshape(int w,int h){host->beforeFrame();drawingArea->updateSurfaceSize(w,h);}
