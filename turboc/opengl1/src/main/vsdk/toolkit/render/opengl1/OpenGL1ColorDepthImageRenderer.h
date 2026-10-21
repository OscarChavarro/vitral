#ifndef __OPEN_GL_1_COLOR_DEPTH_IMAGE_RENDERER__
#define __OPEN_GL_1_COLOR_DEPTH_IMAGE_RENDERER__

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

class RGBImageUncompressed;
class ZBuffer;

/**
Draws an image computed outside OpenGL (i.e. by `ParallelRaytracer`)
together with its depth buffer: the image goes to the color buffer and the
depth values to the OpenGL depth buffer, over the lower left corner of the
current viewport, one pixel of the image per pixel of the viewport (with
`glDrawPixels`, as there are no programs that could write the depth of a
fragment in OpenGL 1.2). Geometry rasterized afterwards (grids,
gizmos, editor feedback, or any body not raytraced) is then depth tested
against the image, which allows mixing raytraced and rasterized objects in one
view.

The depth buffer must hold window space depth values for the camera used to
rasterize the rest of the view (see `DepthBufferMode::OPENGL_DEPTH`). Its row
0 is the top row of the image, as the rows of `RGBImageUncompressed::getPixel`.

The flipped depth buffer is reused among frames while the image size does not
change.

OpenGL 1.2 counterpart of `OpenGL4ColorDepthImageRenderer`.
*/
class OpenGL1ColorDepthImageRenderer {
public:
    /**
    Draws the image in the color buffer and its depth in the depth buffer.
    Depth test, depth function and face culling are left as `GL_LESS`
    with depth test enabled and culling disabled.
    @param image color of each pixel
    @param depth window space depth of each pixel, of the size of `image`,
    or null to draw only the color (without touching the depth buffer)
    */
    static void draw(RGBImageUncompressed* image, ZBuffer* depth);

    /**
    Releases the buffers of this renderer.
    */
    static void dispose();

private:
    static float* depthUploadBuffer;
    static int depthUploadCapacity;

    static void flipDepthRows(ZBuffer* depth);
    static void moveRasterToLowerLeftCorner();
};

#endif
