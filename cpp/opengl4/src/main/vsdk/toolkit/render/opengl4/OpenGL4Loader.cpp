#include <cstdio>
#include <cstring>

#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"

bool OpenGL4Loader::load(GLADloadfunc getProcAddress)
{
    int version = getProcAddress != nullptr ? gladLoadGL(getProcAddress) : 0;
    if ( version == 0 ) {
        fprintf(stderr, "OpenGL4Loader: the OpenGL functions could not be loaded\n");
        return false;
    }
    if ( GLAD_VERSION_MAJOR(version) < 4 ||
         (GLAD_VERSION_MAJOR(version) == 4 && GLAD_VERSION_MINOR(version) < 1) ) {
        fprintf(stderr, "OpenGL4Loader: OpenGL %d.%d found, 4.1 required\n",
                GLAD_VERSION_MAJOR(version), GLAD_VERSION_MINOR(version));
        return false;
    }
    return true;
}

bool OpenGL4Loader::isExtensionSupported(const char* name)
{
    if ( name == nullptr || glGetStringi == nullptr ) {
        return false;
    }
    GLint count = 0;
    glGetIntegerv(GL_NUM_EXTENSIONS, &count);
    for ( GLint i = 0; i < count; i++ ) {
        const char* extension = reinterpret_cast<const char*>(
            glGetStringi(GL_EXTENSIONS, static_cast<GLuint>(i)));
        if ( extension != nullptr && std::strcmp(extension, name) == 0 ) {
            return true;
        }
    }
    return false;
}
