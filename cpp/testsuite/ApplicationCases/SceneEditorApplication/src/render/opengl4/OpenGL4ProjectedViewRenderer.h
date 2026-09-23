#ifndef __SCENE_EDITOR_OPEN_GL_4_PROJECTED_VIEW_RENDERER__
#define __SCENE_EDITOR_OPEN_GL_4_PROJECTED_VIEW_RENDERER__
#include "render/ProjectedViewRenderer.h"
class OpenGL4ProjectedViewRenderer:public ProjectedViewRenderer{public:ZBuffer*renderDepth(SimpleBodyGroup*,Camera*,RendererConfiguration*,int,int)override;};
#endif
