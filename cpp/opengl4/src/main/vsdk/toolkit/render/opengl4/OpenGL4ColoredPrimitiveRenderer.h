#ifndef __OPEN_GL_4_COLORED_PRIMITIVE_RENDERER__
#define __OPEN_GL_4_COLORED_PRIMITIVE_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

/** Small core-profile renderer for non-lit, per-vertex coloured primitives. */
class OpenGL4ColoredPrimitiveRenderer {
public:
    static void draw(const Matrix4x4d& mvp, unsigned int primitiveType,
                     const java::ArrayList<float>& positions,
                     const java::ArrayList<float>& colors);
    static void release();

private:
    static unsigned int vao, positionVbo, colorVbo, program;
    static bool initializeIfNeeded();
};

#endif
