#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBIMAGEUNCOMPRESSEDRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBIMAGEUNCOMPRESSEDRENDERER_H__

#include <GL/glew.h>
#include <map>

class RGBImageUncompressed;

class OpenGL4RGBImageUncompressedRenderer {
public:
    static int activate(RGBImageUncompressed* img);
    static void deactivate(RGBImageUncompressed* img);
    static void unload(RGBImageUncompressed* img);
    static void draw(RGBImageUncompressed* img);

    static void disposeAll();

private:
    static std::map<RGBImageUncompressed*, GLuint> compiledImages;
    static GLuint upload(RGBImageUncompressed* img);
};

#endif // __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4RGBIMAGEUNCOMPRESSEDRENDERER_H__
