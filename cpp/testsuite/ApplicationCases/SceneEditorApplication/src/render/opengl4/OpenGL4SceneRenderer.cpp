#include <glad/gl.h>
#include "java/util/ArrayList.txx"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
#include "render/BodyEditFeedbackProvider.h"
#include "render/opengl4/OpenGL4RenderPrimitiveRenderer.h"
#include "render/opengl4/OpenGL4SceneRenderer.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4BackgroundRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"
void OpenGL4SceneRenderer::drawBody(SimpleBody*b,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){drawBody(b,Matrix4x4d::identityMatrix(),c,l,q);}void OpenGL4SceneRenderer::drawBody(SimpleBody*b,const Matrix4x4d&p,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){if(!b)return;Matrix4x4d transform=p.multiply(b->getTransformationMatrix());PolyhedralBoundedSolid*solid=dynamic_cast<PolyhedralBoundedSolid*>(b->getGeometry());if(solid){drawPolyhedralBoundedSolid(solid,b,transform,c,l,q);return;}OpenGL4GeometryRenderer::draw(b->getGeometry(),c,l,b->getMaterial(),q,dynamic_cast<RGBImageUncompressed*>(b->getTexture()),b->getNormalMapRgb(),transform);}

/**
Draws a brep with `OpenGL4PolyhedralBoundedSolidRenderer`, that takes the
material and the lights from the active ones instead of as parameters, so
they are activated only while the solid is drawn. Textures and normal maps
are not supported by that renderer.
*/
void OpenGL4SceneRenderer::drawPolyhedralBoundedSolid(
    PolyhedralBoundedSolid* solid, SimpleBody* body,
    const Matrix4x4d& transform, Camera* camera,
    const java::ArrayList<Light*>* lights, RendererConfiguration* quality)
{
    OpenGL4SimpleMaterialRenderer::activate(body->getMaterial());
    OpenGL4LightRenderer::deactivateAll();
    if ( lights != nullptr ) {
        for ( long i = 0; i < lights->size(); i++ ) {
            OpenGL4LightRenderer::activate(lights->get(i));
        }
    }

    OpenGL4PolyhedralBoundedSolidRenderer::draw(solid, camera, quality,
        transform);

    OpenGL4LightRenderer::deactivateAll();
    OpenGL4SimpleMaterialRenderer::activate(nullptr);
}
void OpenGL4SceneRenderer::drawBodyGroup(SimpleBodyGroup*g,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){if(!g||!q)return;RendererConfiguration m=q->clone();m.setSelectionCorners(false);m.setBoundingVolume(false);for(long i=0;i<g->getBodies().size();i++)drawBody(g->getBodies()[i],g->getTransformationMatrix(),c,l,&m);if(q->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(g->getMinMax(),c,g->getTransformationMatrix());if(q->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(g->getMinMax(),c,g->getTransformationMatrix());}
void OpenGL4SceneRenderer::draw(Scene*s,BodyEditFeedbackProvider*e){if(!s)return;s->activateSelectedBackground();OpenGL4BackgroundRenderer::draw(s->scene->getActiveBackground());glEnable(GL_DEPTH_TEST);glDepthMask(GL_TRUE);java::ArrayList<Light*>&l=s->scene->getLights();for(long i=0;i<s->scene->getSimpleBodies().size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setSelectionCorners(s->selectedThings->isSelected(i));SimpleBody*b=s->scene->getSimpleBodies()[i];drawBody(b,s->activeCamera,&l,&q);if(e&&e->getTarget()==b)OpenGL4RenderPrimitiveRenderer::draw(e->buildEditFeedback(),s->activeCamera,&l,&q);}drawLightsAndDebugEntities(s);}
void OpenGL4SceneRenderer::drawEditorOverlays(Scene*s,BodyEditFeedbackProvider*e){if(!s)return;java::ArrayList<Light*>&l=s->scene->getLights();glEnable(GL_DEPTH_TEST);for(long i=0;i<s->scene->getSimpleBodies().size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setSurfaces(false);q.setWires(false);q.setPoints(false);q.setSelectionCorners(s->selectedThings->isSelected(i));SimpleBody*b=s->scene->getSimpleBodies()[i];if(q.isSelectionCornersSet()||q.isBoundingVolumeSet()||q.isNormalsSet()||q.isTrianglesNormalsSet())drawBody(b,s->activeCamera,&l,&q);if(e&&e->getTarget()==b)OpenGL4RenderPrimitiveRenderer::draw(e->buildEditFeedback(),s->activeCamera,&l,&q);}drawLightsAndDebugEntities(s);}
void OpenGL4SceneRenderer::drawLightsAndDebugEntities(Scene*s){s->selectedLights->sync();OpenGL4LightRenderer::setScale(s->getLightGizmoScale());for(long i=0;i<s->scene->getLights().size();i++)OpenGL4LightRenderer::draw(s->scene->getLights()[i],s->activeCamera,LightGizmoStyle::OMNI_BILLBOARD,s->selectedLights->isSelected(i));java::ArrayList<Light*>&l=s->scene->getLights();for(long i=0;i<s->debugThingGroups.size();i++){RendererConfiguration q=s->qualityTemplate->clone();q.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);q.setSelectionCorners(s->selectedDebugThingGroups->isSelected(i));drawBodyGroup(s->debugThingGroups[i],s->activeCamera,&l,&q);}}
