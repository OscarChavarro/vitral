#ifndef __OPEN_GL_4_COLOR_DEPTH_IMAGE_RENDERER__
#define __OPEN_GL_4_COLOR_DEPTH_IMAGE_RENDERER__

#include <glad/gl.h>

class RGBImageUncompressed;
class ZBuffer;

/**
Draws an image computed outside OpenGL (i.e. by `ParallelRaytracer`)
together with its depth buffer: the image goes to the color buffer and the
depth values to the OpenGL depth buffer, over the lower left corner of the
current viewport, one texel per pixel. Geometry rasterized afterwards (grids,
gizmos, editor feedback, or any body not raytraced) is then depth tested
against the image, which allows mixing raytraced and rasterized objects in one
view.

The depth buffer must hold window space depth values for the camera used to
rasterize the rest of the view (see `DepthBufferMode::OPENGL_DEPTH`). Its row
0 is the top row of the image, as the rows of `RGBImageUncompressed::getPixel`.

The textures are reused among frames while the image size does not change.
Shaders are read from the base path of `OpenGL4ImageRenderer`.

C++ counterpart of Java's `vsdk.toolkit.render.jogl.Jogl4ColorDepthImageRenderer`.
*/
class OpenGL4ColorDepthImageRenderer {
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
    Releases the shader program, buffers and textures of this renderer.
    PRE: the OpenGL context is current.
    */
    static void dispose();

private:
    static GLuint programId;
    static GLint colorTextureLocation;
    static GLint depthTextureLocation;
    static GLuint vaoId;
    static GLuint positionVboId;
    static GLuint uvVboId;
    static GLuint colorTextureId;
    static GLuint depthTextureId;
    static int textureXSize;
    static int textureYSize;
    static float* depthUploadBuffer;
    static int depthUploadCapacity;

    static bool ensureInitialized();
    static void uploadTextures(RGBImageUncompressed* image, ZBuffer* depth);
    static void drawLowerLeftQuad(int width, int height);
    static GLuint createTexture();
};

#endif
