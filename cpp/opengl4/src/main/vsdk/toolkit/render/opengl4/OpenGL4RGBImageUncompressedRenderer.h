#ifndef __OPENGL4RGBIMAGEUNCOMPRESSEDRENDERER__
#define __OPENGL4RGBIMAGEUNCOMPRESSEDRENDERER__

#include "java/util/HashMap.h"
#include <GL/glew.h>
class RGBImageUncompressed;

class OpenGL4RGBImageUncompressedRenderer {
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
