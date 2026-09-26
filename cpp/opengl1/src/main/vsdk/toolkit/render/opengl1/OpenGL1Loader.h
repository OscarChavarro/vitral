#ifndef __OPENGL1_LOADER__
#define __OPENGL1_LOADER__

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

/**
Checks that the current context offers the OpenGL 1.2 fixed function
pipeline used by the toolkit, and answers about its extensions. Unlike
`OpenGL4Loader`, nothing has to be loaded: the functions used are exported
by the system OpenGL library (see `OpenGL1Api.h`).

Each application calls `load` once, after making its first context current.
*/
class OpenGL1Loader {
public:
    /**
    @return false if the context does not offer OpenGL 1.2 (reported on the
    standard error)
    */
    static bool load();

    /**
    @param name name of an extension (i.e.
    "GL_EXT_texture_compression_s3tc")
    @return true if the current context offers it
    */
    static bool isExtensionSupported(const char* name);

private:
    OpenGL1Loader();
};

#endif
