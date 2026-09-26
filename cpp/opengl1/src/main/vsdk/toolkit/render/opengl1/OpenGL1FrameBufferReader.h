#ifndef __OPEN_GL_1_FRAME_BUFFER_READER__
#define __OPEN_GL_1_FRAME_BUFFER_READER__
class RGBImageUncompressed;
class ZBuffer;
class OpenGL1FrameBufferReader {
public:
    static RGBImageUncompressed* readColor();
    static ZBuffer* readDepth();
};
#endif
