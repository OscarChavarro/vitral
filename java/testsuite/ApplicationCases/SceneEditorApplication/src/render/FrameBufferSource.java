package render;

import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;

/**
Access to the frame buffers of the rendering technology in use, so the
services that capture frames (see `FrameCaptureService`) do not depend on it.
Reads take the area currently set for drawing in the frame buffer.
*/
public interface FrameBufferSource
{
    /**
    @return the content of the color buffer
    */
    RGBImageUncompressed readColor();

    /**
    @return the content of the depth buffer
    */
    ZBuffer readDepth();
}
