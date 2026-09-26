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

#ifndef GL_CLAMP_TO_EDGE
#define GL_CLAMP_TO_EDGE 0x812F
#endif
#ifndef GL_BGRA
#define GL_BGRA 0x80E1
#endif
#ifndef GL_LIGHT_MODEL_COLOR_CONTROL
#define GL_LIGHT_MODEL_COLOR_CONTROL 0x81F8
#endif
#ifndef GL_SINGLE_COLOR
#define GL_SINGLE_COLOR 0x81F9
#endif
#ifndef GL_SEPARATE_SPECULAR_COLOR
#define GL_SEPARATE_SPECULAR_COLOR 0x81FA
#endif
#ifndef GL_RESCALE_NORMAL
#define GL_RESCALE_NORMAL 0x803A
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

#endif
