#ifndef __OPEN_GL_4_RENDERER_CONFIGURATION_SHADER_SELECTOR__
#define __OPEN_GL_4_RENDERER_CONFIGURATION_SHADER_SELECTOR__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class RendererConfiguration;

/**
Selects the GLSL program (from the files of `etc/glslShaders`) that presents
the shading type of a `RendererConfiguration`: constant (no light), flat,
Gouraud, Phong or Cook-Torrance, with texture and bump map variants. The
programs are built the first time they are needed, and are the same ones the
Java GL4 pipeline uses.

C++ counterpart of Java's `Jogl4RendererConfigurationShaderSelector`.
*/
class OpenGL4RendererConfigurationShaderSelector {
public:
    /**
    @param quality configuration to present, or null for a constant color
    @param hasTexture true if a diffuse texture is available
    @param hasNormalMap true if a normal (bump) map is available
    @return the program, or 0 if its shader files could not be built
    */
    static unsigned int selectSurfaceShaderProgram(
        const RendererConfiguration* quality, bool hasTexture,
        bool hasNormalMap);

    /**
    Uses a program, setting the uniforms common to every program: the
    projection, the diffuse color, the texture and vertex colors switches and
    the texture units of the samplers.
    */
    static void activateShader(unsigned int programId,
                               const Matrix4x4d& modelViewProjection,
                               const RendererConfiguration* quality,
                               float diffuseR, float diffuseG, float diffuseB);

    static void deactivateShader();

    /**
    Releases the programs. PRE: the context that built them is current.
    */
    static void dispose();

private:
    static unsigned int constantProgramId;
    static unsigned int texturedProgramId;
    static unsigned int flatProgramId;
    static unsigned int flatTexturedProgramId;
    static unsigned int gouraudProgramId;
    static unsigned int phongProgramId;
    static unsigned int phongBumpProgramId;
    static unsigned int cookProgramId;
    static unsigned int cookBumpProgramId;

    static void ensurePrograms();
    static unsigned int createProgramFromFiles(const char* vertexFile,
                                               const char* pixelFile);

    OpenGL4RendererConfigurationShaderSelector() {}
};

#endif
