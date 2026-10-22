#include <stdio.h>
#include <stdlib.h>


#ifdef VITRAL_WITH_OSMESA
#include <GL/osmesa.h>
#else
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#endif

#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "java/lang/Exception.h"

/**
Offscreen rendering with OpenGL 1.2: draws a white line and exports the
color buffer to `./output.ppm`. Two variants are built from this source:

  - With OSMesa (`VITRAL_WITH_OSMESA`, the `_OpenGL1PbufferExample`
    target): Mesa renders into a buffer of this program. It needs no X11
    display nor window, and every pixel is defined. OSMesa offers its own
    OpenGL implementation, so this variant links `libOSMesa` instead of
    `libGL` (and of the toolkit modules that use it).
  - With GLUT (the `_OpenGL1PbufferExampleGlut` target): GLUT has no
    hidden windows nor pbuffers, so the image is drawn in the back buffer
    of a normal window and read with `glReadPixels` before it is shown.
    It needs a display, and in hardware implementations the pixels of the
    parts of the window that are covered or outside of the screen are
    undefined (the pixel ownership test).

Both use only OpenGL 1.1 calls, and give the same image.
*/

static const int WIDTH = 320;
static const int HEIGHT = 240;

static void drawScene()
{
    static const float vertexData[] = {
        -0.8f, -0.8f, 0.0f,
         0.8f,  0.8f, 0.0f
    };

    glViewport(0, 0, WIDTH, HEIGHT);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    // Constant white color with identity matrices, as the constant GLSL
    // program of the OpenGL 4 version
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    glColor3f(1.0f, 1.0f, 1.0f);

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertexData);
    glDrawArrays(GL_LINES, 0, 2);
    glDisableClientState(GL_VERTEX_ARRAY);
    glFinish();
}

/**
@param rgba pixels as OpenGL gives them: four bytes each, rows from the
bottom up
*/
static void writeRgbaToPpm(const char* outFile, const unsigned char* rgba, int w, int h)
{
    FILE* f = fopen(outFile, "wb");
    if (!f) {
        Logger::reportMessage("OpenGL1PbufferExample", Logger::ERROR, "writeRgbaToPpm", "Could not open output file");
        throw VSDKFatalException("Could not open output file");
    }
    fprintf(f, "P6\n%d %d\n255\n", w, h);

    for (int y = h - 1; y >= 0; y--) {
        const unsigned char* row = rgba + (size_t)y * (size_t)w * 4u;
        for (int x = 0; x < w; x++) {
            fwrite(row + x * 4, 1, 3, f);
        }
    }
    fclose(f);
}

#ifdef VITRAL_WITH_OSMESA

int main()
{
    unsigned char* rgba = new unsigned char[(size_t)WIDTH * (size_t)HEIGHT * 4u];
    OSMesaContext context = OSMesaCreateContextExt(OSMESA_RGBA, 16, 0, 0, NULL);
    if (context == NULL ||
        !OSMesaMakeCurrent(context, rgba, GL_UNSIGNED_BYTE, WIDTH, HEIGHT)) {
        fprintf(stderr, "OSMesa context creation failed\n");
        if (context != NULL) OSMesaDestroyContext(context);
        delete[] rgba;
        return 1;
    }
    printf("OpenGL version string: %s (OSMesa)\n", (const char*)glGetString(GL_VERSION));

    try {
        drawScene();
        // The color buffer is the buffer given to OSMesaMakeCurrent, with
        // the rows from the bottom up as glReadPixels gives them
        writeRgbaToPpm("./output.ppm", rgba, WIDTH, HEIGHT);
        printf("OpenGL1PbufferExample: exported ./output.ppm\n");
    }
    catch (const Exception& e) {
        fprintf(stderr, "%s\n", e.getMessage().c_str());
        OSMesaDestroyContext(context);
        delete[] rgba;
        return 1;
    }

    OSMesaDestroyContext(context);
    delete[] rgba;
    return 0;
}

#else

static void displayCallback()
{
    int status = 0;
    try {
        drawScene();
        unsigned char* rgba = new unsigned char[(size_t)WIDTH * (size_t)HEIGHT * 4u];
        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        writeRgbaToPpm("./output.ppm", rgba, WIDTH, HEIGHT);
        delete[] rgba;
        printf("OpenGL1PbufferExample: exported ./output.ppm\n");
    }
    catch (const Exception& e) {
        fprintf(stderr, "%s\n", e.getMessage().c_str());
        status = 1;
    }
    // Classic GLUT never returns from its main loop
    exit(status);
}

int main(int argc, char** argv)
{
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA);
    glutInitWindowSize(WIDTH, HEIGHT);
    glutCreateWindow("OpenGL1 Pbuffer Example");
    printf("OpenGL version string: %s (GLUT window)\n", (const char*)glGetString(GL_VERSION));

    glutDisplayFunc(displayCallback);
    glutMainLoop();
    return 0;
}

#endif
