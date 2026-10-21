#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"

/**
Smallest OpenGL 1.2 / GLUT program of the toolkit: draws a white line with
the constant (no light) state of `OpenGL1RendererConfigurationStateSelector`,
the counterpart of the constant GLSL program used by the OpenGL 4 version.
The vertices go to OpenGL as a client side vertex array (OpenGL 1.1).
*/
class HelloWorldOpenGL1
{
public:
    static HelloWorldOpenGL1* instance;

    HelloWorldOpenGL1()
        : ready(false)
    {
    }

    ~HelloWorldOpenGL1()
    {
        cleanup();
    }

    void init()
    {
        checkOpenGLVersion();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ready = true;
    }

    void display()
    {
        if (!ready) return;

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        static const float vertexData[] = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        OpenGL1RendererConfigurationStateSelector::activateConstantState(
            Matrix4x4d::identityMatrix(), 1.0f, 1.0f, 1.0f);
        glLineWidth(1.0f);
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(3, GL_FLOAT, 0, vertexData);
        glDrawArrays(GL_LINES, 0, 2);
        glDisableClientState(GL_VERTEX_ARRAY);
        OpenGL1RendererConfigurationStateSelector::deactivateState();

        glutSwapBuffers();
    }

    void reshape(int width, int height)
    {
        glViewport(0, 0, width, height);
    }

    void keyboard(unsigned char key)
    {
        if (key == 27) {
            requestClose();
        }
    }

    void run(int* argc, char** argv)
    {
        glutInit(argc, argv);
        glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA);
        glutInitWindowSize(640, 480);
        glutCreateWindow("VITRAL concept test - OpenGL1 Hello World");

        glutDisplayFunc(displayCallback);
        glutReshapeFunc(reshapeCallback);
        glutKeyboardFunc(keyboardCallback);

        init();

        glutMainLoop();
    }

    void requestClose()
    {
        // Classic GLUT never returns from its main loop
        cleanup();
        exit(0);
    }

private:
    bool ready;

    static void displayCallback()
    {
        instance->display();
    }

    static void reshapeCallback(int width, int height)
    {
        instance->reshape(width, height);
    }

    static void keyboardCallback(unsigned char key, int /*x*/, int /*y*/)
    {
        instance->keyboard(key);
    }

    void checkOpenGLVersion()
    {
        const char* versionStr = (const char*)glGetString(GL_VERSION);
        printf("OpenGL version string: %s\n", versionStr);

        // OpenGL 1.x has no GL_MAJOR_VERSION query: the loader parses the
        // version string
        if (!OpenGL1Loader::load()) {
            fprintf(stderr, "This example requires OpenGL 1.2+\n");
            Logger::reportMessage("HelloWorldOpenGL1", Logger::ERROR, "checkOpenGLVersion", "OpenGL version too old");
            throw VSDKFatalException("OpenGL version too old");
        }
    }

    void cleanup()
    {
        if (ready) {
            OpenGL1RendererConfigurationStateSelector::dispose();
        }
        ready = false;
    }
};

HelloWorldOpenGL1* HelloWorldOpenGL1::instance = NULL;

int main(int argc, char* argv[])
{
    HelloWorldOpenGL1::instance = new HelloWorldOpenGL1();
    HelloWorldOpenGL1::instance->run(&argc, argv);
    return 0;
}
