#include "java/lang/String.h"
#include "java/lang/StringBuilder.h"
#include <cstdio>

#include <glad/gl.h>

#include "vsdk/toolkit/render/opengl4/OpenGL4ShaderProgramUtil.h"

namespace {

/**
@return the source of a file of `etc/glslShaders`, looked for from the usual
working directories of the examples and applications, or an empty string
*/
java::String readShaderSource(const char* fileName)
{
    const char* folders[] = {
        "../../../../etc/glslShaders/",
        "../../../etc/glslShaders/",
        "../../etc/glslShaders/",
        "../etc/glslShaders/",
        "etc/glslShaders/"
    };
    for ( size_t i = 0; i < sizeof(folders) / sizeof(folders[0]); i++ ) {
        java::String path = java::String(folders[i]) + fileName;
        FILE* file = fopen(path.c_str(), "rb");
        if ( file == nullptr ) {
            continue;
        }
        java::StringBuilder text;
        char buffer[4096];
        size_t count;
        while ( (count = fread(buffer, 1, sizeof(buffer), file)) > 0 ) {
            text.append(buffer, (int)count);
        }
        fclose(file);
        java::String source = text.toString();
        if ( !source.isEmpty() ) {
            return source;
        }
    }
    return java::String();
}

GLuint compileShader(GLenum type, const java::String& source, const char* fileName)
{
    GLuint shader = glCreateShader(type);
    const char* text = source.c_str();
    glShaderSource(shader, 1, &text, nullptr);
    glCompileShader(shader);
    GLint ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4ShaderProgramUtil: %s: %s\n",
                fileName, log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

}

unsigned int OpenGL4ShaderProgramUtil::createProgramFromFiles(
    const char* vertexFile, const char* pixelFile)
{
    java::String vertexSource = readShaderSource(vertexFile);
    java::String pixelSource = readShaderSource(pixelFile);
    if ( vertexSource.isEmpty() || pixelSource.isEmpty() ) {
        fprintf(stderr, "OpenGL4ShaderProgramUtil: shader not found: %s / %s\n",
                vertexFile, pixelFile);
        return 0;
    }
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource, vertexFile);
    GLuint pixelShader = compileShader(GL_FRAGMENT_SHADER, pixelSource, pixelFile);
    if ( vertexShader == 0 || pixelShader == 0 ) {
        if ( vertexShader != 0 ) glDeleteShader(vertexShader);
        if ( pixelShader != 0 ) glDeleteShader(pixelShader);
        return 0;
    }

    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, pixelShader);
    glLinkProgram(program);
    glDeleteShader(vertexShader);
    glDeleteShader(pixelShader);

    GLint ok = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetProgramInfoLog(program, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4ShaderProgramUtil: link %s / %s: %s\n",
                vertexFile, pixelFile, log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}
