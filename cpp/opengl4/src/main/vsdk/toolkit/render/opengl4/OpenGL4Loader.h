#ifndef __OPENGL4_LOADER__
#define __OPENGL4_LOADER__

#include <glad/gl.h>

/**
Loads the OpenGL 4.1 core functions used by the toolkit (with the glad
loader generated in `opengl4/src/glad`), and answers about the extensions
of the current context.

Each application calls `load` once, after making its first context current,
with the function lookup of its windowing system: `glfwGetProcAddress`
(GLFW), `glXGetProcAddressARB` (GLX, i.e. Xt), `wglGetProcAddress`... The
functions are the same for every context of the same driver. The toolkit
does not depend on any windowing system: the application gives the lookup.
*/
class OpenGL4Loader {
public:
    /**
    @param getProcAddress lookup of the OpenGL functions by name
    @return false if the context does not offer OpenGL 4.1 core (reported
    on the standard error)
    */
    static bool load(GLADloadfunc getProcAddress);

    /**
    @param name name of an extension (i.e.
    "GL_EXT_texture_compression_s3tc")
    @return true if the current context offers it
    */
    static bool isExtensionSupported(const char* name);

private:
    OpenGL4Loader();
};

#endif
