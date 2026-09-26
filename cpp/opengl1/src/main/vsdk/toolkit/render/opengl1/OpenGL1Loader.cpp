#include <cstdio>
#include <cstring>

#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"

bool OpenGL1Loader::load()
{
    const char* version = reinterpret_cast<const char*>(glGetString(GL_VERSION));
    int major = 0;
    int minor = 0;
    if ( version == nullptr || sscanf(version, "%d.%d", &major, &minor) != 2 ) {
        fprintf(stderr, "OpenGL1Loader: no current OpenGL context\n");
        return false;
    }
    if ( major < 1 || (major == 1 && minor < 2) ) {
        fprintf(stderr, "OpenGL1Loader: OpenGL %d.%d found, 1.2 required\n",
                major, minor);
        return false;
    }
    return true;
}

bool OpenGL1Loader::isExtensionSupported(const char* name)
{
    if ( name == nullptr || name[0] == '\0' ) {
        return false;
    }
    const char* extensions = reinterpret_cast<const char*>(
        glGetString(GL_EXTENSIONS));
    if ( extensions == nullptr ) {
        return false;
    }
    size_t length = std::strlen(name);
    const char* start = extensions;
    const char* found;
    while ( (found = std::strstr(start, name)) != nullptr ) {
        const char* end = found + length;
        if ( (found == extensions || found[-1] == ' ') &&
             (*end == ' ' || *end == '\0') ) {
            return true;
        }
        start = end;
    }
    return false;
}
