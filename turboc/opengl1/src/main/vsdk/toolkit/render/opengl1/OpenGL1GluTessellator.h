#ifndef __OPENGL1_GLU_TESSELLATOR__
#define __OPENGL1_GLU_TESSELLATOR__

/**
The GLU 1.2 polygon tessellator interface used by the toolkit, implemented
over the GLU 1.1 one, for the GLU libraries of the time (i.e. Mesa 2.2 on
Debian 1.1). `OpenGL1Api.h` includes it only when `<GL/glu.h>` is older than
GLU 1.2; the GLU 1.2 names are then macros to these functions.

Limitations of GLU 1.1: one tessellation at a time (the polygon data and the
callbacks are kept by these functions, from `gluNewTess` on), no
`GLU_TESS_COMBINE` (GLU 1.1 cannot split intersecting contours: it reports
an error instead) and no `gluTessNormal` (GLU 1.1 finds the plane of the
polygon from its vertices).
*/

typedef GLUtriangulatorObj GLUtesselator;

// GLU 1.2 callback names (the first five are the GLU 1.1 ones), cast as
// GLenum is an enumeration in old headers
#define GLU_TESS_BEGIN ((GLenum)100100)
#define GLU_TESS_VERTEX ((GLenum)100101)
#define GLU_TESS_END ((GLenum)100102)
#define GLU_TESS_ERROR ((GLenum)100103)
#define GLU_TESS_EDGE_FLAG ((GLenum)100104)
#define GLU_TESS_COMBINE ((GLenum)100105)
#define GLU_TESS_BEGIN_DATA ((GLenum)100106)
#define GLU_TESS_VERTEX_DATA ((GLenum)100107)
#define GLU_TESS_END_DATA ((GLenum)100108)
#define GLU_TESS_ERROR_DATA ((GLenum)100109)
#define GLU_TESS_EDGE_FLAG_DATA ((GLenum)100110)
#define GLU_TESS_COMBINE_DATA ((GLenum)100111)

GLUtesselator* vitralGluNewTess();
void vitralGluTessCallback(GLUtesselator* tess, GLenum which, void (*fn)());
void vitralGluTessBeginPolygon(GLUtesselator* tess, void* polygonData);
void vitralGluTessBeginContour(GLUtesselator* tess);
void vitralGluTessEndContour(GLUtesselator* tess);
void vitralGluTessEndPolygon(GLUtesselator* tess);
void vitralGluTessNormal(GLUtesselator* tess, GLdouble x, GLdouble y,
    GLdouble z);

#define gluNewTess vitralGluNewTess
#define gluTessCallback vitralGluTessCallback
#define gluTessBeginPolygon vitralGluTessBeginPolygon
#define gluTessBeginContour vitralGluTessBeginContour
#define gluTessEndContour vitralGluTessEndContour
#define gluTessEndPolygon vitralGluTessEndPolygon
#define gluTessNormal vitralGluTessNormal

#endif
