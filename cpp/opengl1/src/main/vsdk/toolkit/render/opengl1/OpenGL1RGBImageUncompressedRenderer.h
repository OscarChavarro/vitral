#ifndef __OPEN_GL_1_RGB_IMAGE_UNCOMPRESSED_RENDERER__
#define __OPEN_GL_1_RGB_IMAGE_UNCOMPRESSED_RENDERER__

#include "java/util/HashMap.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
class RGBImageUncompressed;

class OpenGL1RGBImageUncompressedRenderer {
public:
    static int activate(RGBImageUncompressed* img);
    static void deactivate(RGBImageUncompressed* img);
    static void unload(RGBImageUncompressed* img);
    static void draw(RGBImageUncompressed* img);

    static void disposeAll();

private:
    static java::HashMap<RGBImageUncompressed*, GLuint> compiledImages;
    static GLuint upload(RGBImageUncompressed* img);
};

#endif
