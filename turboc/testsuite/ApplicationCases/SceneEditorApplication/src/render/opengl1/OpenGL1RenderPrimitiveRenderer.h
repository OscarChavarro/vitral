#ifndef __SCN_EDTR_GL1_RNDR_PRM_RNDRR__
#define __SCN_EDTR_GL1_RNDR_PRM_RNDRR__
#include "java/util/ArrayList.h"
class Camera;class Light;class RendererConfiguration;class RenderPrimitive;
class OpenGL1RenderPrimitiveRenderer{public:static void draw(const RenderPrimitive&,Camera*,const ArrayList<Light*>*,RendererConfiguration*);static void draw(const ArrayList<RenderPrimitive>&,Camera*,const ArrayList<Light*>*,RendererConfiguration*);};
#endif
