#ifndef __VITRAL_OPENGL4_GLU_COMPAT_H__
#define __VITRAL_OPENGL4_GLU_COMPAT_H__

#ifdef __APPLE__
#include <OpenGL/glu.h>
#else
#include_next <GL/glu.h>
#endif

#endif
