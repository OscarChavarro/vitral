#ifndef __SCENE_EDITOR_OPEN_GL_4_RENDER_PRIMITIVE_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_4_RENDER_PRIMITIVE_RENDERER__
#include "java/util/ArrayList.h"
class Camera;class Light;class RendererConfiguration;class RenderPrimitive;
class OpenGL4RenderPrimitiveRenderer{public:static void draw(const RenderPrimitive&,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);static void draw(const java::ArrayList<RenderPrimitive>&,Camera*,const java::ArrayList<Light*>*,RendererConfiguration*);};
#endif
