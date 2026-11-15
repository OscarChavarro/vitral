#ifndef __SCN_EDTR_GL1_PRJ_VW_RNDRR__
#define __SCN_EDTR_GL1_PRJ_VW_RNDRR__
#include "render/ProjectedViewRenderer.h"
class OpenGL1ProjectedViewRenderer:public ProjectedViewRenderer{public:ZBuffer*renderDepth(SimpleBodyGroup*,Camera*,RendererConfiguration*,int,int);};
#endif
