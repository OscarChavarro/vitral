#ifndef __FRAME_BUFFER_SOURCE__
#define __FRAME_BUFFER_SOURCE__

class RGBImageUncompressed;
class ZBuffer;

/**
Access to the frame buffers of the rendering technology in use, so the
services that capture frames (see `FrameCaptureService`) do not depend on it.
Reads take the area currently set for drawing in the frame buffer.
*/
class FrameBufferSource {
public:
    virtual ~FrameBufferSource() {}

    /**
    @return the content of the color buffer, owned by the caller
    */
    virtual RGBImageUncompressed* readColor() = 0;

    /**
    @return the content of the depth buffer, owned by the caller
    */
    virtual ZBuffer* readDepth() = 0;
};

#endif
