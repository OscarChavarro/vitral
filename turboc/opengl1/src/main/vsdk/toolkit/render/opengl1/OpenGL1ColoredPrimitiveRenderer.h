#ifndef __OPN_GL_1_CLRD_PRMTV_RNDRR__
#define __OPN_GL_1_CLRD_PRMTV_RNDRR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

/** Small fixed function renderer for non-lit, per-vertex coloured primitives. */
class OpenGL1ColoredPrimitiveRenderer {
public:
    static void draw(const Matrix4x4d& mvp, unsigned int primitiveType,
                     const ArrayList<float>& positions,
                     const ArrayList<float>& colors);
    static void release();
};

#endif
