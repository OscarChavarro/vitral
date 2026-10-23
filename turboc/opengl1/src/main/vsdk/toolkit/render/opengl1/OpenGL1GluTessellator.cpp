// The GLU 1.2 tessellator over GLU 1.1 (see OpenGL1GluTessellator.h): it
// only exists for GLU libraries older than 1.2. The GLU headers are read
// directly, so gluTessCallback and the others are the GLU 1.1 functions here.
#ifdef __APPLE__
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/gl.h>
#include <GL/glu.h>
#endif

#ifndef GLU_VERSION_1_2

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"

typedef GLUtriangulatorObj GLUtesselator;

GLUtesselator* vitralGluNewTess();
void vitralGluTessCallback(GLUtesselator* tess, GLenum which, void (*fn)());
void vitralGluTessBeginPolygon(GLUtesselator* tess, void* polygonData);
void vitralGluTessBeginContour(GLUtesselator* tess);
void vitralGluTessEndContour(GLUtesselator* tess);
void vitralGluTessEndPolygon(GLUtesselator* tess);
void vitralGluTessNormal(GLUtesselator* tess, GLdouble x, GLdouble y,
    GLdouble z);

// The GLU 1.1 callbacks (GLU_BEGIN ... GLU_EDGE_FLAG) and their GLU 1.2
// variants with the polygon data (6 values later)
// (int, as GLenum is an enumeration in old headers)
static const int FIRST_CALLBACK = 100100;
static const int COMBINE_CALLBACK = 100105;
static const int FIRST_DATA_CALLBACK = 100106;
static const int CALLBACK_COUNT = 12;

typedef void (*BeginCallback)(GLenum);
typedef void (*BeginDataCallback)(GLenum, void*);
typedef void (*VertexCallback)(void*);
typedef void (*VertexDataCallback)(void*, void*);
typedef void (*EndCallback)();
typedef void (*EndDataCallback)(void*);
typedef void (*ErrorCallback)(GLenum);
typedef void (*ErrorDataCallback)(GLenum, void*);
typedef void (*EdgeFlagCallback)(GLboolean);
typedef void (*EdgeFlagDataCallback)(GLboolean, void*);

static void (*callbacks[CALLBACK_COUNT])();
static void* currentPolygonData = NULL;
static int contourCount = 0;

static void (*callbackOf(int which))()
{
    return callbacks[which - FIRST_CALLBACK];
}

static void beginTrampoline(GLenum type)
{
    void (*withData)() = callbackOf(GLU_BEGIN + 6);
    if ( withData != NULL ) {
        ((BeginDataCallback)withData)(type, currentPolygonData);
    }
    else if ( callbackOf(GLU_BEGIN) != NULL ) {
        ((BeginCallback)callbackOf(GLU_BEGIN))(type);
    }
}

static void vertexTrampoline(void* vertex)
{
    void (*withData)() = callbackOf(GLU_VERTEX + 6);
    if ( withData != NULL ) {
        ((VertexDataCallback)withData)(vertex, currentPolygonData);
    }
    else if ( callbackOf(GLU_VERTEX) != NULL ) {
        ((VertexCallback)callbackOf(GLU_VERTEX))(vertex);
    }
}

static void endTrampoline()
{
    void (*withData)() = callbackOf(GLU_END + 6);
    if ( withData != NULL ) {
        ((EndDataCallback)withData)(currentPolygonData);
    }
    else if ( callbackOf(GLU_END) != NULL ) {
        ((EndCallback)callbackOf(GLU_END))();
    }
}

static void errorTrampoline(GLenum error)
{
    void (*withData)() = callbackOf(GLU_ERROR + 6);
    if ( withData != NULL ) {
        ((ErrorDataCallback)withData)(error, currentPolygonData);
    }
    else if ( callbackOf(GLU_ERROR) != NULL ) {
        ((ErrorCallback)callbackOf(GLU_ERROR))(error);
    }
}

static void edgeFlagTrampoline(GLboolean flag)
{
    void (*withData)() = callbackOf(GLU_EDGE_FLAG + 6);
    if ( withData != NULL ) {
        ((EdgeFlagDataCallback)withData)(flag, currentPolygonData);
    }
    else if ( callbackOf(GLU_EDGE_FLAG) != NULL ) {
        ((EdgeFlagCallback)callbackOf(GLU_EDGE_FLAG))(flag);
    }
}

GLUtesselator* vitralGluNewTess()
{
    for ( int i = 0; i < CALLBACK_COUNT; i++ ) {
        callbacks[i] = NULL;
    }
    currentPolygonData = NULL;
    contourCount = 0;
    return gluNewTess();
}

void vitralGluTessCallback(GLUtesselator* tess, GLenum which, void (*fn)())
{
    int callback = (int)which;
    if ( callback < FIRST_CALLBACK ||
         callback >= FIRST_CALLBACK + CALLBACK_COUNT ) {
        return;
    }
    callbacks[callback - FIRST_CALLBACK] = fn;

    // The GLU 1.1 event: combine has none
    int event = callback >= FIRST_DATA_CALLBACK ? callback - 6 : callback;
    if ( event == COMBINE_CALLBACK ) {
        return;
    }
    BOOLEAN used = callbackOf(event) != NULL || callbackOf(event + 6) != NULL;
    void (*trampoline)() = NULL;
    if ( used ) {
        switch ( event ) {
          case GLU_BEGIN:
            trampoline = (void (*)())beginTrampoline;
            break;
          case GLU_VERTEX:
            trampoline = (void (*)())vertexTrampoline;
            break;
          case GLU_END:
            trampoline = (void (*)())endTrampoline;
            break;
          case GLU_ERROR:
            trampoline = (void (*)())errorTrampoline;
            break;
          default:
            trampoline = (void (*)())edgeFlagTrampoline;
            break;
        }
    }
    gluTessCallback(tess, (GLenum)event, trampoline);
}

void vitralGluTessBeginPolygon(GLUtesselator* tess, void* polygonData)
{
    currentPolygonData = polygonData;
    contourCount = 0;
    gluBeginPolygon(tess);
}

void vitralGluTessBeginContour(GLUtesselator* tess)
{
    // gluBeginPolygon starts the first contour
    if ( contourCount > 0 ) {
        gluNextContour(tess, (GLenum)GLU_UNKNOWN);
    }
    contourCount++;
}

void vitralGluTessEndContour(GLUtesselator*)
{
}

void vitralGluTessEndPolygon(GLUtesselator* tess)
{
    gluEndPolygon(tess);
    currentPolygonData = NULL;
}

void vitralGluTessNormal(GLUtesselator*, GLdouble, GLdouble, GLdouble)
{
}

#endif
