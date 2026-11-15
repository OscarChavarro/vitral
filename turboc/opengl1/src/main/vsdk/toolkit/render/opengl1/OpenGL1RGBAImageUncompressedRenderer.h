#ifndef __OPN_GL_1_RGBA_IMG_UNCM_RNDR__
#define __OPN_GL_1_RGBA_IMG_UNCM_RNDR__

#include "java/util/HashMap.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
class RGBAImageUncompressed;

class OpenGL1RGBAImageUncompressedRenderer {
public:
    static int activate(RGBAImageUncompressed* img);
    static void deactivate(RGBAImageUncompressed* img);
    static void unload(RGBAImageUncompressed* img);
    static void draw(RGBAImageUncompressed* img);

    static void disposeAll();

private:
    static HashMap<RGBAImageUncompressed*, GLuint> compiledImages;
    static GLuint upload(RGBAImageUncompressed* img);
};

#endif
