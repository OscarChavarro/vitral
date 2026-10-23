#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"

/**
@return false if there is no current context
*/
static BOOLEAN contextVersion(int* major, int* minor)
{
    const char* version = ((const char*)(glGetString(GL_VERSION)));
    *major = 0;
    *minor = 0;
    return version != NULL && sscanf(version, "%d.%d", major, minor) == 2;
}

BOOLEAN OpenGL1Loader::load()
{
    int major = 0;
    int minor = 0;
    if ( !contextVersion(&major, &minor) ) {
        fprintf(stderr, "OpenGL1Loader: no current OpenGL context\n");
        return FALSE;
    }
    if ( major < 1 || (major == 1 && minor < 1) ) {
        fprintf(stderr, "OpenGL1Loader: OpenGL %d.%d found, 1.1 required\n",
                major, minor);
        return FALSE;
    }
    return TRUE;
}

BOOLEAN OpenGL1Loader::offersOpenGL12()
{
    int major = 0;
    int minor = 0;
    return contextVersion(&major, &minor) &&
        (major > 1 || (major == 1 && minor >= 2));
}

BOOLEAN OpenGL1Loader::isExtensionSupported(const char* name)
{
    if ( name == NULL || name[0] == '\0' ) {
        return FALSE;
    }
    const char* extensions = ((const char*)(
        glGetString(GL_EXTENSIONS)));
    if ( extensions == NULL ) {
        return FALSE;
    }
    size_t length = strlen(name);
    const char* start = extensions;
    const char* found;
    while ( (found = strstr(start, name)) != NULL ) {
        const char* end = found + length;
        if ( (found == extensions || found[-1] == ' ') &&
             (*end == ' ' || *end == '\0') ) {
            return TRUE;
        }
        start = end;
    }
    return FALSE;
}
