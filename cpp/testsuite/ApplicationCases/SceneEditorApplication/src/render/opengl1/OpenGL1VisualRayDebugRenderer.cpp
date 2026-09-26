#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "render/VisualRayDebugGeometry.h"
#include "render/opengl1/OpenGL1RenderPrimitiveRenderer.h"
#include "render/opengl1/OpenGL1VisualRayDebugRenderer.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
OpenGL1VisualRayDebugRenderer::OpenGL1VisualRayDebugRenderer(ApplicationModel*m):scene(m->getScene()),geometry(new VisualRayDebugGeometry(m)){}OpenGL1VisualRayDebugRenderer::~OpenGL1VisualRayDebugRenderer(){delete geometry;}void OpenGL1VisualRayDebugRenderer::draw(){java::ArrayList<RenderPrimitive>p=geometry->buildPrimitives();OpenGL1RenderPrimitiveRenderer::draw(p,scene->activeCamera,&scene->scene->getLights(),geometry->getRendererConfiguration());}
