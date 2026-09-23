#ifndef __SCENE_EDITOR_OPEN_GL_4_FRAME_BUFFER_SOURCE__
#define __SCENE_EDITOR_OPEN_GL_4_FRAME_BUFFER_SOURCE__
#include "render/FrameBufferSource.h"
class OpenGL4FrameBufferSource:public FrameBufferSource{public:RGBImageUncompressed*readColor()override;ZBuffer*readDepth()override;};
#endif
