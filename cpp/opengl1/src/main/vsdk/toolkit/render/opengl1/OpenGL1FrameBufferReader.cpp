#include <cstddef>
#include <vector>
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1FrameBufferReader.h"

RGBImageUncompressed* OpenGL1FrameBufferReader::readColor()
{
    GLint v[4]; glGetIntegerv(GL_VIEWPORT, v);
    int w = v[2], h = v[3];
    std::vector<unsigned char> pixels((size_t)w*h*3);
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(v[0], v[1], w, h, GL_RGB, GL_UNSIGNED_BYTE, pixels.data());
    RGBImageUncompressed* result = new RGBImageUncompressed(); result->initNoFill(w, h);
    for ( int y=0; y<h; ++y ) for ( int x=0; x<w; ++x ) {
        size_t i = ((size_t)y*w+x)*3;
        result->putPixel(x, h-1-y, (char)pixels[i], (char)pixels[i+1], (char)pixels[i+2]);
    }
    return result;
}

ZBuffer* OpenGL1FrameBufferReader::readDepth()
{
    GLint v[4]; glGetIntegerv(GL_VIEWPORT, v);
    int w = v[2], h = v[3];
    std::vector<float> pixels((size_t)w*h);
    glReadPixels(v[0], v[1], w, h, GL_DEPTH_COMPONENT, GL_FLOAT, pixels.data());
    ZBuffer* result = new ZBuffer(w, h);
    for ( int y=0; y<h; ++y ) for ( int x=0; x<w; ++x ) result->setDepth(x, h-1-y, pixels[(size_t)y*w+x]);
    return result;
}
