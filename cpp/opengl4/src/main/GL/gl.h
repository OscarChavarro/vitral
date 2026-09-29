#ifndef __VITRAL_OPENGL4_GL_COMPAT_H__
#define __VITRAL_OPENGL4_GL_COMPAT_H__

#ifdef __APPLE__
#include <OpenGL/gl.h>
#else
#include_next <GL/gl.h>
#endif

#endif
