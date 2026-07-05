#ifndef __OPENGL4RGBAIMAGECOMPRESSEDRENDERER__
#define __OPENGL4RGBAIMAGECOMPRESSEDRENDERER__

#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include <GL/glew.h>
class RGBAImageCompressed;

class OpenGL4RGBAImageCompressedRenderer {
public:
    static int activate(RGBAImageCompressed* img);
    static void deactivate(RGBAImageCompressed* img);
    static void unload(RGBAImageCompressed* img);
    static void draw(RGBAImageCompressed* img);

    static void disposeAll();

private:
    static java::HashMap<RGBAImageCompressed*, GLuint> compiledImages;
    static GLuint upload(RGBAImageCompressed* img);

    static java::ArrayList<unsigned char> decompressToRGBA(const RGBAImageCompressed* img);
    static int toOpenGlInternalFormat(int compressionFormat);

    static int readUShort(const unsigned char* data, int offset);
    static int readInt(const unsigned char* data, int offset);
    static unsigned long long readAlphaBits(const unsigned char* data, int offset);
    static void decodeRgb565(int packed, int& r, int& g, int& b);
    static void buildAlphaTable(int a0, int a1, int outTable[8]);
};

#endif
