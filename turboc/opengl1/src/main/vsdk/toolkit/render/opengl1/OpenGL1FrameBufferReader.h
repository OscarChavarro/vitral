#ifndef __OPN_GL_1_FRM_BFFR_RDR__
#define __OPN_GL_1_FRM_BFFR_RDR__
class RGBImageUncompressed;
class ZBuffer;
class OpenGL1FrameBufferReader {
public:
    static RGBImageUncompressed* readColor();
    static ZBuffer* readDepth();
};
#endif
