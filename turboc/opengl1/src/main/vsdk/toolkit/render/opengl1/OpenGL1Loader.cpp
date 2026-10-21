#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"

bool OpenGL1Loader::load()
{
    const char* version = ((const char*)(glGetString(GL_VERSION)));
    int major = 0;
    int minor = 0;
    if ( version == NULL || sscanf(version, "%d.%d", &major, &minor) != 2 ) {
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
    if ( name == NULL || name[0] == '\0' ) {
        return false;
    }
    const char* extensions = ((const char*)(
        glGetString(GL_EXTENSIONS)));
    if ( extensions == NULL ) {
        return false;
    }
    size_t length = strlen(name);
    const char* start = extensions;
    const char* found;
    while ( (found = strstr(start, name)) != NULL ) {
        const char* end = found + length;
        if ( (found == extensions || found[-1] == ' ') &&
             (*end == ' ' || *end == '\0') ) {
            return true;
        }
        start = end;
    }
    return false;
}
