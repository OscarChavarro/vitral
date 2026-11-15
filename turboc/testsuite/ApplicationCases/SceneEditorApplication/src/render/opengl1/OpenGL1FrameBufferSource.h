#ifndef __SCN_EDTR_GL1_FRM_BFFR_SRC__
#define __SCN_EDTR_GL1_FRM_BFFR_SRC__
#include "render/FrameBufferSource.h"
class OpenGL1FrameBufferSource:public FrameBufferSource{public:RGBImageUncompressed*readColor();ZBuffer*readDepth();};
#endif
