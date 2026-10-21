#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlutSystem.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixRenderer.h"

/**
C++ counterpart of Java's `Jogl4Examples/CameraExample`, with GLUT and the
OpenGL 1.2 fixed function pipeline. As in Java, the scene is only redrawn
when the camera controller reports a change (or the window is resized /
exposed).
*/
static Camera* camera = NULL;
static CameraController* cameraController = NULL;
static OpenGL1SimpleCorridorSample* corridor = NULL;

static void createModel()
{
    camera = new Camera();
    cameraController = new CameraControllerAquynza(camera);
    corridor = new OpenGL1SimpleCorridorSample();
}

static void drawObjectsGL()
{
    glEnable(GL_DEPTH_TEST);

    Matrix4x4d projection = OpenGL1CameraRenderer::activate(camera);
    float* mvp = projection.exportToFloatArrayColumnOrder();
    corridor->drawGL(mvp, projection);
    OpenGL1MatrixRenderer::draw(mvp, Matrix4x4d::identityMatrix());
    delete[] mvp;
}

static void display()
{
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    drawObjectsGL();
    glutSwapBuffers();
}

static void dispose()
{
    if ( corridor != NULL ) {
        corridor->dispose();
    }
    OpenGL1CameraRenderer::dispose();
    OpenGL1MatrixRenderer::release();
    delete corridor;
    delete cameraController;
    delete camera;
    corridor = NULL;
    cameraController = NULL;
    camera = NULL;
}

static void reshape(int xSize, int ySize)
{
    glViewport(0, 0, xSize, ySize);
    camera->updateViewportResize(xSize, ySize);
    glutPostRedisplay();
}

static void requestClose()
{
    // Classic GLUT never returns from its main loop
    dispose();
    exit(0);
}

static void keyPressed(const KeyEvent& e)
{
    if ( e.keycode == KeyEvent::KEY_ESC ) {
        requestClose();
        return;
    }
    if ( cameraController->processKeyPressedEvent(e) ) {
        glutPostRedisplay();
    }
}

static void keyReleased(const KeyEvent& e)
{
    if ( cameraController->processKeyReleasedEvent(e) ) {
        glutPostRedisplay();
    }
}

static void keyboardCallback(unsigned char key, int /*x*/, int /*y*/)
{
    keyPressed(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void keyboardUpCallback(unsigned char key, int /*x*/, int /*y*/)
{
    keyReleased(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void specialCallback(int key, int /*x*/, int /*y*/)
{
    keyPressed(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void specialUpCallback(int key, int /*x*/, int /*y*/)
{
    keyReleased(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void mouseCallback(int button, int state, int x, int y)
{
    int direction;
    if ( GlutSystem::isWheelButton(button, &direction) ) {
        if ( state == GLUT_DOWN ) {
            MouseEvent e = GlutSystem::glut2vsdkWheelEvent(0, direction, x, y);
            if ( cameraController->processMouseWheelEvent(e) ) {
                glutPostRedisplay();
            }
        }
        return;
    }

    GlutSystem::updateMouseButtonsState(button, state);
    MouseEvent e = GlutSystem::glut2vsdkMouseEvent(button, state, x, y);
    bool changed;
    if ( state == GLUT_DOWN ) {
        changed = cameraController->processMousePressedEvent(e);
    }
    else {
        changed = cameraController->processMouseReleasedEvent(e);
    }
    if ( changed ) {
        glutPostRedisplay();
    }
}

static void motionCallback(int x, int y)
{
    MouseEvent e = GlutSystem::glut2vsdkMotionEvent(x, y);
    e.setModifiers(GlutSystem::getMouseButtonsDownMask());
    if ( cameraController->processMouseDraggedEvent(e) ) {
        glutPostRedisplay();
    }
}

static void passiveMotionCallback(int x, int y)
{
    MouseEvent e = GlutSystem::glut2vsdkMotionEvent(x, y);
    if ( cameraController->processMouseMovedEvent(e) ) {
        glutPostRedisplay();
    }
}

int main(int argc, char** argv)
{
    glutInit(&argc, argv);
    glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH);
    glutInitWindowSize(640, 480);
    glutCreateWindow("VITRAL concept test - OpenGL1 Camera control example");

    if ( !OpenGL1Loader::load() ) {
        printf("Can not start OpenGL/GLUT.\n");
        return 0;
    }

    createModel();

    glutDisplayFunc(display);
    glutReshapeFunc(reshape);
    glutKeyboardFunc(keyboardCallback);
    glutKeyboardUpFunc(keyboardUpCallback);
    glutSpecialFunc(specialCallback);
    glutSpecialUpFunc(specialUpCallback);
    glutMouseFunc(mouseCallback);
    glutMotionFunc(motionCallback);
    glutPassiveMotionFunc(passiveMotionCallback);

    glutMainLoop();
    return 0;
}
