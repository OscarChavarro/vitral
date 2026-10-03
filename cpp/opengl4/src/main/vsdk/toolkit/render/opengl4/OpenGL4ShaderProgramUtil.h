#ifndef __OPEN_GL_4_SHADER_PROGRAM_UTIL__
#define __OPEN_GL_4_SHADER_PROGRAM_UTIL__

/**
Builds GLSL programs from the shader files of `etc/glslShaders`, looked for
from the usual working directories of the examples and applications.

C++ counterpart of Java's `Jogl4ShaderProgramUtil` (only the creation from
files is ported). C++ port note: errors are reported on the standard error
and give a 0 program, where Java throws `IllegalStateException`.
*/
class OpenGL4ShaderProgramUtil {
public:
    /**
    PRE: an OpenGL 4 context is current.
    @param vertexShaderFile name of the vertex shader file
    @param fragmentShaderFile name of the fragment (pixel) shader file
    @return the linked program, or 0 if it could not be built
    */
    static unsigned int createProgramFromFiles(const char* vertexShaderFile,
                                               const char* fragmentShaderFile);

private:
    OpenGL4ShaderProgramUtil() {}
};

#endif
