#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "java/util/ArrayList.txx"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "render/BodyEditFeedbackProvider.h"
#include "render/opengl1/OpenGL1RenderPrimitiveRenderer.h"
#include "render/opengl1/OpenGL1SceneRenderer.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1BackgroundRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LightRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.h"
void OpenGL1SceneRenderer::drawBody(SimpleBody*b,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){drawBody(b,Matrix4x4d::identityMatrix(),c,l,q);}void OpenGL1SceneRenderer::drawBody(SimpleBody*b,const Matrix4x4d&p,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){if(!b)return;OpenGL1GeometryRenderer::draw(b->getGeometry(),c,l,b->getMaterial(),q,dynamic_cast<RGBImageUncompressed*>(b->getTexture()),b->getNormalMapRgb(),p.multiply(b->getTransformationMatrix()));}
void OpenGL1SceneRenderer::drawBodyGroup(SimpleBodyGroup*g,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){if(!g||!q)return;RendererConfiguration m=q->clone();m.setSelectionCorners(false);m.setBoundingVolume(false);for(long i=0;i<g->getBodies().size();i++)drawBody(g->getBodies()[i],g->getTransformationMatrix(),c,l,&m);if(q->isBoundingVolumeSet())OpenGL1MinMaxRenderer::draw(g->getMinMax(),c,g->getTransformationMatrix());if(q->isSelectionCornersSet())OpenGL1SelectionCornersRenderer::draw(g->getMinMax(),c,g->getTransformationMatrix());}
void OpenGL1SceneRenderer::draw(Scene*s,BodyEditFeedbackProvider*e){if(!s)return;s->activateSelectedBackground();OpenGL1BackgroundRenderer::draw(s->scene->getActiveBackground());glEnable(GL_DEPTH_TEST);glDepthMask(GL_TRUE);java::ArrayList<Light*>&l=s->scene->getLights();for(long i=0;i<s->scene->getSimpleBodies().size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setSelectionCorners(s->selectedThings->isSelected(i));SimpleBody*b=s->scene->getSimpleBodies()[i];drawBody(b,s->activeCamera,&l,&q);if(e&&e->getTarget()==b)OpenGL1RenderPrimitiveRenderer::draw(e->buildEditFeedback(),s->activeCamera,&l,&q);}drawLightsAndDebugEntities(s);}
void OpenGL1SceneRenderer::drawEditorOverlays(Scene*s,BodyEditFeedbackProvider*e){if(!s)return;java::ArrayList<Light*>&l=s->scene->getLights();glEnable(GL_DEPTH_TEST);for(long i=0;i<s->scene->getSimpleBodies().size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setSurfaces(false);q.setWires(false);q.setPoints(false);q.setSelectionCorners(s->selectedThings->isSelected(i));SimpleBody*b=s->scene->getSimpleBodies()[i];if(q.isSelectionCornersSet()||q.isBoundingVolumeSet()||q.isNormalsSet()||q.isTrianglesNormalsSet())drawBody(b,s->activeCamera,&l,&q);if(e&&e->getTarget()==b)OpenGL1RenderPrimitiveRenderer::draw(e->buildEditFeedback(),s->activeCamera,&l,&q);}drawLightsAndDebugEntities(s);}
void OpenGL1SceneRenderer::drawLightsAndDebugEntities(Scene*s){s->selectedLights->sync();OpenGL1LightRenderer::setScale(s->getLightGizmoScale());for(long i=0;i<s->scene->getLights().size();i++)OpenGL1LightRenderer::draw(s->scene->getLights()[i],s->activeCamera,LightGizmoStyle::OMNI_BILLBOARD,s->selectedLights->isSelected(i));java::ArrayList<Light*>&l=s->scene->getLights();for(long i=0;i<s->debugThingGroups.size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);q.setSelectionCorners(s->selectedDebugThingGroups->isSelected(i));drawBodyGroup(s->debugThingGroups[i],s->activeCamera,&l,&q);}}
