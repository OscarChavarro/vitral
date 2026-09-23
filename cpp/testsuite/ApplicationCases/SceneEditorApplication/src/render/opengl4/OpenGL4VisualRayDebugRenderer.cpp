#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "render/VisualRayDebugGeometry.h"
#include "render/opengl4/OpenGL4RenderPrimitiveRenderer.h"
#include "render/opengl4/OpenGL4VisualRayDebugRenderer.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
OpenGL4VisualRayDebugRenderer::OpenGL4VisualRayDebugRenderer(ApplicationModel*m):scene(m->getScene()),geometry(new VisualRayDebugGeometry(m)){}OpenGL4VisualRayDebugRenderer::~OpenGL4VisualRayDebugRenderer(){delete geometry;}void OpenGL4VisualRayDebugRenderer::draw(){java::ArrayList<RenderPrimitive>p=geometry->buildPrimitives();OpenGL4RenderPrimitiveRenderer::draw(p,scene->activeCamera,&scene->scene->getLights(),geometry->getRendererConfiguration());}
