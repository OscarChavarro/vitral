#ifndef __OPN_GL_1_RGB_IMG_UNCMP_RNDR__
#define __OPN_GL_1_RGB_IMG_UNCMP_RNDR__

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
    static HashMap<RGBImageUncompressed*, GLuint> compiledImages;
    static GLuint upload(RGBImageUncompressed* img);
};

#endif
