#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LINERENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LINERENDERER_H__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

class OpenGL4LineRenderer {
public:
    static void drawLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth);

    static void drawLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        float depthBiasNdc);

    static void release();

private:
    static unsigned int vao_;
    static unsigned int positionVbo_;
    static unsigned int colorVbo_;
    static unsigned int program_;
    static bool initialized_;

    static bool initializeIfNeeded();
    static java::String readShaderFile(const java::String& filename);
    static unsigned int compileShader(const java::String& source, unsigned int type);
    static void drawThinLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        float depthBiasNdc);
    static void drawPrimitives(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        unsigned int primitiveType,
        float depthBiasNdc,
        float lineWidth);
    static void buildThickLineMesh(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        java::ArrayList<float>& trianglePositions,
        java::ArrayList<float>& triangleColors);
};

}}}}

#endif
