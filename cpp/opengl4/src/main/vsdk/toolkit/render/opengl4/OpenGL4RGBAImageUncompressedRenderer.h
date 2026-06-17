#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBAIMAGEUNCOMPRESSEDRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBAIMAGEUNCOMPRESSEDRENDERER_H__

#include "java/util/HashMap.h"
#include <GL/glew.h>
class RGBAImageUncompressed;

class OpenGL4RGBAImageUncompressedRenderer {
public:
    static int activate(RGBAImageUncompressed* img);
    static void deactivate(RGBAImageUncompressed* img);
    static void unload(RGBAImageUncompressed* img);
    static void draw(RGBAImageUncompressed* img);

    static void disposeAll();

private:
    static java::HashMap<RGBAImageUncompressed*, GLuint> compiledImages;
    static GLuint upload(RGBAImageUncompressed* img);
};

#endif // __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBAIMAGEUNCOMPRESSEDRENDERER_H__
