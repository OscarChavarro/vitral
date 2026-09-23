#include "java/util/ArrayList.txx"
#include "render/RenderPrimitive.h"
#include "render/opengl4/OpenGL4RenderPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
void OpenGL4RenderPrimitiveRenderer::draw(const RenderPrimitive&p,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){OpenGL4GeometryRenderer::draw(p.getGeometry(),c,l,&p.getMaterial(),q,0,0,p.getTransform());}void OpenGL4RenderPrimitiveRenderer::draw(const java::ArrayList<RenderPrimitive>&p,Camera*c,const java::ArrayList<Light*>*l,RendererConfiguration*q){for(long i=0;i<p.size();i++)draw(p[i],c,l,q);}
