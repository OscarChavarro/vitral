#ifndef __OPEN_GL_1_API__
#define __OPEN_GL_1_API__

/**
Declarations of the OpenGL 1.2 fixed function pipeline and of GLU, as offered
by each platform. Every function used by the toolkit is part of OpenGL 1.1,
exported directly by the system libraries (even `opengl32.dll` on Windows),
so no loader is needed; OpenGL 1.2 only adds enumerants (i.e.
`GL_CLAMP_TO_EDGE`, `GL_BGRA`, `GL_SEPARATE_SPECULAR_COLOR`), defined here
when the platform headers are older.

The counterpart of `glad/gl.h` in the OpenGL 4 port.
*/

#ifdef _WIN32
#include <windows.h>
#endif

#ifdef __APPLE__
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/gl.h>
#include <GL/glu.h>
#endif

// Cast: GLenum is an enumeration in old headers (i.e. Mesa 2.2)
#ifndef GL_CLAMP_TO_EDGE
#define GL_CLAMP_TO_EDGE ((GLenum)0x812F)
#endif
#ifndef GL_BGRA
#define GL_BGRA ((GLenum)0x80E1)
#endif
#ifndef GL_LIGHT_MODEL_COLOR_CONTROL
#define GL_LIGHT_MODEL_COLOR_CONTROL ((GLenum)0x81F8)
#endif
#ifndef GL_SINGLE_COLOR
#define GL_SINGLE_COLOR ((GLenum)0x81F9)
#endif
#ifndef GL_SEPARATE_SPECULAR_COLOR
#define GL_SEPARATE_SPECULAR_COLOR ((GLenum)0x81FA)
#endif
#ifndef GL_RESCALE_NORMAL
#define GL_RESCALE_NORMAL ((GLenum)0x803A)
#endif

// Mesa 2.x exports the OpenGL 1.1 client state functions, but its <GL/gl.h>
// does not declare them
#if !defined(GL_VERSION_1_2) && !defined(_WIN32) && !defined(__APPLE__)
extern "C" {
void glEnableClientState(GLenum array);
void glDisableClientState(GLenum array);
}
#endif

// Calling convention of GL/GLU callbacks: defined by Mesa and Windows
// headers, but not by the macOS ones
#ifndef GLAPIENTRY
#ifdef _WIN32
#define GLAPIENTRY APIENTRY
#else
#define GLAPIENTRY
#endif
#endif

/// Type of the callbacks given to `gluTessCallback` (`_GLUfuncptr` in Mesa,
/// `GLvoid (*)()` in macOS)
typedef void (GLAPIENTRY *OpenGL1GluCallback)();

// The GLU 1.2 tessellator over older GLU libraries (i.e. Mesa 2.2)
#ifndef GLU_VERSION_1_2
#include "vsdk/toolkit/render/opengl1/OpenGL1GluTessellator.h"
#endif

#endif
