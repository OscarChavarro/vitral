#ifndef __OPEN_GL_4_FRAME_BUFFER_READER__
#define __OPEN_GL_4_FRAME_BUFFER_READER__
class RGBImageUncompressed;
class ZBuffer;
class OpenGL4FrameBufferReader {
public:
    static RGBImageUncompressed* readColor();
    static ZBuffer* readDepth();
};
#endif
