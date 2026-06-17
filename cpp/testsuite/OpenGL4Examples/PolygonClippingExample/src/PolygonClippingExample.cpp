#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <GL/glew.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "java/lang/String.h"
#include "java/io/File.h"
#include "java/util/ArrayList.txx"

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_Polygon2DWA.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_Polygon2DContourWA.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_DoubleLinkedListNode.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_VertexNode2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/WeilerAthertonPolygonClipper.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4Polygon2DRenderer.h"
#include "model/PolygonClippingFixtures.h"
#include "model/PolygonSurfaceTessellationMode.h"
#include "options/CommandLineOptions.h"
#include "render/PolygonClippingHudRenderer.h"
#include "render/PolygonTriangularRenderer.h"

enum Operation { INTERSECTION=0, UNION=1, A_MINUS_B=2, B_MINUS_A=3 };

struct Bounds2D {
    double minX;
    double minY;
    double maxX;
    double maxY;
    Bounds2D() : minX(0), minY(0), maxX(1), maxY(1) {}
    Bounds2D(double a, double b, double c, double d) : minX(a), minY(b), maxX(c), maxY(d) {}
};

struct App {
    GLFWwindow* w;
    GLuint lineProg;
    GLuint constantProg;
    GLuint vao, vboP, vboC;
    int testIndex;
    Operation op;
    PolygonSurfaceTessellationMode tessellationMode;
    bool showRef, showClip, showSubj, showInner, showOuter, showIntersections, showFilled;
    Polygon2D* clipPolygon;
    Polygon2D* subjectPolygon;
    Polygon2D* innerPolygon;
    Polygon2D* outerPolygon;
    WeilerAthertonPolygonClipper* clipper;
    Camera camera;
    CameraControllerOrbiter* cameraController;
    RendererConfiguration quality;
    RendererConfigurationController* qualityController;
    PolygonClippingHudRenderer* hud;

    App() : w(0), lineProg(0), constantProg(0), vao(0), vboP(0), vboC(0), testIndex(0), op(INTERSECTION),
        tessellationMode(PolygonSurfaceTessellationMode::GLU),
        showRef(true), showClip(true), showSubj(true), showInner(true), showOuter(true),
        showIntersections(true), showFilled(true), clipPolygon(0), subjectPolygon(0), innerPolygon(0), outerPolygon(0),
        clipper(0), cameraController(0), qualityController(0), hud(0)
    {
        quality.setWires(true);
        quality.setPoints(true);
        quality.setSurfaces(true);
        camera.setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
        camera.setOrthogonalZoom(camera.getOrthogonalZoom() / 16.0);
        camera.updateViewportResize(1100, 900);
        cameraController = new CameraControllerOrbiter(&camera);
        qualityController = new RendererConfigurationController(&quality);
    }

    ~App() {
        delete clipPolygon;
        delete subjectPolygon;
        delete innerPolygon;
        delete outerPolygon;
        delete clipper;
        delete cameraController;
        delete qualityController;
        if (hud) { hud->dispose(); delete hud; }
    }
};

static GLuint compile(GLenum t, const char* s)
{
    GLuint id = glCreateShader(t);
    glShaderSource(id, 1, &s, 0);
    glCompileShader(id);
    return id;
}

static GLuint buildLineProgram()
{
    const char* vs = "#version 410 core\nlayout(location=0) in vec3 aPos;layout(location=1) in vec3 aCol;uniform mat4 modelViewProjectionLocal;out vec3 c;void main(){c=aCol;gl_Position=modelViewProjectionLocal*vec4(aPos,1.0);}";
    const char* fs = "#version 410 core\nin vec3 c;out vec4 f;void main(){f=vec4(c,1.0);}";
    GLuint v = compile(GL_VERTEX_SHADER, vs);
    GLuint f = compile(GL_FRAGMENT_SHADER, fs);
    GLuint p = glCreateProgram();
    glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
    glDeleteShader(v); glDeleteShader(f);
    return p;
}

static GLuint buildConstantProgram()
{
    const char* vs =
        "#version 410 core\n"
        "layout(location=0) in vec4 aPos;\n"
        "uniform mat4 modelViewProjectionLocal;\n"
        "void main(){gl_Position=modelViewProjectionLocal*aPos;}";
    const char* fs =
        "#version 410 core\n"
        "uniform vec3 diffuseColor;\n"
        "uniform int withTexture;\n"
        "uniform int withVertexColors;\n"
        "out vec4 f;\n"
        "void main(){f=vec4(diffuseColor,1.0);}";
    GLuint v = compile(GL_VERTEX_SHADER, vs);
    GLuint f = compile(GL_FRAGMENT_SHADER, fs);
    GLuint p = glCreateProgram();
    glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
    glDeleteShader(v); glDeleteShader(f);
    return p;
}

static Polygon2D* buildPolygonFromEncoded(const char* loopsEncoded)
{
    Polygon2D* p = new Polygon2D();
    for (long int i = 0; i < p->loops.size(); ++i) delete p->loops[i];
    p->loops.clear();
    if (!loopsEncoded || !(*loopsEncoded)) { p->nextLoop(); return p; }

    long int len = (long int)std::strlen(loopsEncoded);
    char* tmp = new char[len + 1];
    std::memcpy(tmp, loopsEncoded, len + 1);

    char* cursor = tmp;
    bool firstLoop = true;
    while (*cursor) {
        char* nextSep = std::strchr(cursor, ';');
        if (nextSep) *nextSep = '\0';

        if (firstLoop) {
            p->nextLoop();
            firstLoop = false;
        }
        else {
            p->nextLoop();
        }

        double values[1024];
        int vcount = 0;
        char* s = cursor;
        while (*s && vcount < 1024) {
            while (*s == ' ' || *s == ',') s++;
            if (!*s) break;
            char* endp = 0;
            double val = std::strtod(s, &endp);
            if (endp == s) break;
            values[vcount++] = val;
            s = endp;
            while (*s == ' ' || *s == ',') s++;
        }
        for (int i = 0; i + 1 < vcount; i += 2) p->addVertex(values[i], values[i+1]);

        if (!nextSep) break;
        cursor = nextSep + 1;
    }

    delete[] tmp;
    return p;
}

static Polygon2D* buildPolygonFromEncoded(const char* loopsEncoded, double yOffset)
{
    Polygon2D* p = buildPolygonFromEncoded(loopsEncoded);
    for (long int i = 0; i < p->loops.size(); ++i) {
        _Polygon2DContour* c = p->loops[i];
        for (long int j = 0; j < c->vertices.size(); ++j) c->vertices[j].y += yOffset;
    }
    return p;
}

static void rebuildScene(App* a)
{
    if (a->clipPolygon) delete a->clipPolygon;
    if (a->subjectPolygon) delete a->subjectPolygon;
    if (a->innerPolygon) delete a->innerPolygon;
    if (a->outerPolygon) delete a->outerPolygon;
    if (a->clipper) delete a->clipper;

    const PolygonClippingTestCase& t = PolygonClippingFixtures::CASES[a->testIndex];
    a->clipPolygon = buildPolygonFromEncoded(t.clipLoops, -1.0);
    a->subjectPolygon = buildPolygonFromEncoded(t.subjectLoops, 0.0);
    a->innerPolygon = new Polygon2D();
    a->outerPolygon = new Polygon2D();
    a->clipper = new WeilerAthertonPolygonClipper();
    if (a->op == INTERSECTION) {
        a->clipper->clipPolygons(a->clipPolygon, a->subjectPolygon, a->innerPolygon, a->outerPolygon);
    }
    else if (a->op == UNION) {
        a->clipper->unionPolygons(a->clipPolygon, a->subjectPolygon, a->innerPolygon);
        for (long int i = 0; i < a->outerPolygon->loops.size(); ++i) delete a->outerPolygon->loops[i];
        a->outerPolygon->loops.clear();
        a->outerPolygon->nextLoop();
    }
    else if (a->op == A_MINUS_B) {
        Polygon2D scratch;
        a->clipper->clipPolygons(a->subjectPolygon, a->clipPolygon, &scratch, a->innerPolygon);
    }
    else {
        Polygon2D scratch;
        a->clipper->clipPolygons(a->clipPolygon, a->subjectPolygon, &scratch, a->innerPolygon);
    }
}

static void includePoly(Polygon2D* p, double& minX, double& minY, double& maxX, double& maxY, bool& init)
{
    if (!p) return;
    for (long int i = 0; i < p->loops.size(); ++i) {
        _Polygon2DContour* c = p->loops[i];
        for (long int j = 0; j < c->vertices.size(); ++j) {
            Vertex2D v = c->vertices[j];
            if (!init) { minX=maxX=v.x; minY=maxY=v.y; init=true; }
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y;
        }
    }
}

static Bounds2D calculateBounds(App* a)
{
    double minX=0,minY=0,maxX=1,maxY=1; bool init=false;
    includePoly(a->clipPolygon, minX,minY,maxX,maxY,init);
    includePoly(a->subjectPolygon, minX,minY,maxX,maxY,init);
    includePoly(a->innerPolygon, minX,minY,maxX,maxY,init);
    includePoly(a->outerPolygon, minX,minY,maxX,maxY,init);
    if (!init) { minX=0; minY=0; maxX=1; maxY=1; }
    return Bounds2D(minX, minY, maxX, maxY);
}

static void focusCameraOnCurrentScene(App* a)
{
    Bounds2D b = calculateBounds(a);
    double cx = 0.5 * (b.minX + b.maxX);
    double cy = 0.5 * (b.minY + b.maxY);
    Vector3Dd center(cx, 0.0, cy);

    Vector3Dd eye = a->camera.getPosition();
    Vector3Dd focus = a->camera.getFocusedPosition();
    double distance = eye.subtract(focus).length();
    if (distance < 1e-9) distance = 20.0;
    a->camera.setPosition(Vector3Dd(center.x(), center.y() - distance, center.z()));
    a->camera.setFocusedPositionMaintainingOrthogonality(center);
    a->camera.setUpMaintainingOrthogonality(Vector3Dd(0, 0, 1));
    if (a->cameraController) a->cameraController->setPointOfInterest(center);
}

static void drawReference(App* a, const Matrix4x4d& mvp)
{
    Polygon2D axis;
    for (long int i = 0; i < axis.loops.size(); ++i) delete axis.loops[i];
    axis.loops.clear();
    axis.nextLoop(); axis.addVertex(0,0); axis.addVertex(3,0);
    OpenGL4Polygon2DRenderer::draw(mvp, &axis, &a->quality, 0.4f,0.1f,0.1f, 1.0f,0.2f,0.2f, a->lineProg, a->constantProg, a->vao, a->vboP, a->vboC);
}

static void drawResultPolygon(App* a, const Matrix4x4d& mvp, Polygon2D* polygon,
    RendererConfiguration* polygonQuality,
    float fillR, float fillG, float fillB,
    float lineR, float lineG, float lineB)
{
    if (a->tessellationMode == PolygonSurfaceTessellationMode::MONOTONE_DECOMPOSITION) {
        PolygonTriangularRenderer::fillPolygonSurface(
            mvp, polygon, polygonQuality,
            fillR, fillG, fillB, lineR, lineG, lineB,
            a->lineProg, a->constantProg, a->vao, a->vboP, a->vboC);
    } else {
        OpenGL4Polygon2DRenderer::draw(mvp, polygon, polygonQuality,
            fillR, fillG, fillB, lineR, lineG, lineB,
            a->lineProg, a->constantProg, a->vao, a->vboP, a->vboC);
    }
}

static java::String onOff(bool v)
{
    return v ? java::String("ON") : java::String("OFF");
}

static java::String operationName(Operation op)
{
    if (op == INTERSECTION) return java::String("INTERSECTION");
    if (op == UNION) return java::String("UNION");
    if (op == A_MINUS_B) return java::String("A_MINUS_B");
    return java::String("B_MINUS_A");
}

static int loopCount(Polygon2D* p)
{
    if (!p) return 0;
    return (int)p->loops.size();
}

static java::String buildHudLine1(const App* a)
{
    const PolygonClippingTestCase& t = PolygonClippingFixtures::CASES[a->testIndex];
    java::String line = java::String("Test [1,2]: ") + java::String(t.name)
        + " (" + java::String::valueOf(a->testIndex + 1) + "/" + java::String::valueOf((int)PolygonClippingFixtures::COUNT) + ")";
    line += "  Op [3]: " + operationName(a->op);
    return line;
}

static java::String buildHudLine2(const App* a)
{
    java::String line = java::String("Clip [C]: ")
        + onOff(a->showClip)
        + "  Subject [S]: " + onOff(a->showSubj)
        + "  Points [P]: " + onOff(a->showIntersections);
    return line;
}

static java::String buildHudLine3(const App* a)
{
    java::String base;
    if (a->op == INTERSECTION) {
        base = java::String("Inner [I]: ") + onOff(a->showInner)
            + "  Outer [O]: " + onOff(a->showOuter);
    } else {
        base = java::String("Result [I]: ") + onOff(a->showInner)
            + "  Secondary [O]: " + onOff(a->showOuter);
    }
    return base
        + "  Fill [t]: " + onOff(a->showFilled)
        + "  Tess [T]: " + java::String(tessellationModeDisplayName(a->tessellationMode));
}

static java::String buildHudLine4(const App* a)
{
    return java::String("Reference frame [Space]: ") + onOff(a->showRef);
}

static int countPairedVertices(_Polygon2DWA* polygon)
{
    if (!polygon) return 0;
    int paired = 0;
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContourWA* loop = polygon->loops.get(i);
        if (!loop || !loop->vertices.getHead()) continue;
        _DoubleLinkedListNode<_VertexNode2D>* head = loop->vertices.getHead();
        _DoubleLinkedListNode<_VertexNode2D>* cursor = head;
        int guard = 0;
        do {
            if (cursor->data.pairNode != 0) paired++;
            cursor = cursor->next;
            guard++;
        } while (cursor != head && guard <= loop->vertices.size() + 1);
    }
    return paired / 2;
}

static void push3(java::ArrayList<float>& a, float x, float y, float z) { a.add(x); a.add(y); a.add(z); }

static void drawPolygonWA(App* a, const Matrix4x4d& mvp, _Polygon2DWA* polygon,
    float lineR, float lineG, float lineB, float pointR, float pointG, float tx, float tz)
{
    if (!polygon) return;
    glUseProgram(a->lineProg);
    GLint mvpLoc = glGetUniformLocation(a->lineProg, "modelViewProjectionLocal");
    if (mvpLoc >= 0) {
        float mm[16];
        int k = 0; for (int c = 0; c < 4; ++c) for (int r = 0; r < 4; ++r) mm[k++] = (float)mvp.get(r, c);
        glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mm);
    }
    glBindVertexArray(a->vao);
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContourWA* contour = polygon->loops.get(i);
        if (!contour || !contour->vertices.getHead()) continue;
        java::ArrayList<float> linePos, lineCol, pointPos, pointCol;
        _DoubleLinkedListNode<_VertexNode2D>* head = contour->vertices.getHead();
        _DoubleLinkedListNode<_VertexNode2D>* cursor = head;
        do {
            _DoubleLinkedListNode<_VertexNode2D>* next = cursor->next;
            push3(linePos, (float)(cursor->data.x + tx), 0.0f, (float)(cursor->data.y + tz));
            push3(lineCol, lineR, lineG, lineB);
            push3(linePos, (float)(next->data.x + tx), 0.0f, (float)(next->data.y + tz));
            push3(lineCol, lineR, lineG, lineB);

            float pr = (cursor->data.pairNode == 0) ? pointR : 0.15f;
            float pg = (cursor->data.pairNode == 0) ? pointG : 0.85f;
            float pb = (cursor->data.pairNode == 0) ? 0.45f : 0.25f;
            push3(pointPos, (float)(cursor->data.x + tx), 0.0f, (float)(cursor->data.y + tz));
            push3(pointCol, pr, pg, pb);
            cursor = cursor->next;
        } while (cursor != head);

        glBindBuffer(GL_ARRAY_BUFFER, a->vboP);
        glBufferData(GL_ARRAY_BUFFER, linePos.size() * sizeof(float), &linePos[0], GL_STREAM_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
        glBindBuffer(GL_ARRAY_BUFFER, a->vboC);
        glBufferData(GL_ARRAY_BUFFER, lineCol.size() * sizeof(float), &lineCol[0], GL_STREAM_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
        glLineWidth(2.0f);
        glDrawArrays(GL_LINES, 0, linePos.size() / 3);

        if (a->showIntersections) {
            glBindBuffer(GL_ARRAY_BUFFER, a->vboP);
            glBufferData(GL_ARRAY_BUFFER, pointPos.size() * sizeof(float), &pointPos[0], GL_STREAM_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glBindBuffer(GL_ARRAY_BUFFER, a->vboC);
            glBufferData(GL_ARRAY_BUFFER, pointCol.size() * sizeof(float), &pointCol[0], GL_STREAM_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glPointSize(8.0f);
            glDrawArrays(GL_POINTS, 0, pointPos.size() / 3);
        }
    }
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
}

static void keyCb(GLFWwindow* w, int key, int, int action, int)
{
    App* a = (App*)glfwGetWindowUserPointer(w);
    if (!a) return;
    int mods = 0;
    if (glfwGetKey(w, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(w, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS) mods |= KeyEvent::MASK_SHIFT;
    if (glfwGetKey(w, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(w, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS) mods |= KeyEvent::MASK_CTRL;
    if (glfwGetKey(w, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(w, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS) mods |= KeyEvent::MASK_ALT;

    bool rebuild = false;
    bool handledLetterShortcut = false;

    if (action == GLFW_PRESS) {
        if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(w, GLFW_TRUE);
        else if (key == GLFW_KEY_1) { a->testIndex = (a->testIndex + PolygonClippingFixtures::COUNT - 1) % PolygonClippingFixtures::COUNT; rebuild = true; }
        else if (key == GLFW_KEY_2) { a->testIndex = (a->testIndex + 1) % PolygonClippingFixtures::COUNT; rebuild = true; }
        else if (key == GLFW_KEY_3) { a->op = (Operation)(((int)a->op + 1) % 4); rebuild = true; }
        else if (key == GLFW_KEY_SPACE) a->showRef = !a->showRef;
        else if (key == GLFW_KEY_C) { a->showClip = !a->showClip; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_S) { a->showSubj = !a->showSubj; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_I) { a->showInner = !a->showInner; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_O) { a->showOuter = !a->showOuter; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_P) { a->showIntersections = !a->showIntersections; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_T) {
            if (mods & KeyEvent::MASK_SHIFT) {
                // Uppercase T: cycle surface tessellation mode (GLU vs monotone decomposition)
                a->tessellationMode = nextTessellationMode(a->tessellationMode);
            } else {
                // Lowercase t: toggle whether polygon surfaces are filled
                a->showFilled = !a->showFilled;
            }
            handledLetterShortcut = true;
        }
        else if (key == GLFW_KEY_G) { a->showFilled = !a->showFilled; handledLetterShortcut = true; }
        else if (key == GLFW_KEY_F) { handledLetterShortcut = true; }
        else if (key == GLFW_KEY_H) { handledLetterShortcut = true; }

        KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        if (!handledLetterShortcut && a->cameraController) {
            a->cameraController->processKeyPressedEvent(event);
        }
        if (a->qualityController) {
            a->qualityController->processKeyPressedEvent(event);
        }
    }
    else if (action == GLFW_RELEASE) {
        KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        if (a->cameraController) {
            a->cameraController->processKeyReleasedEvent(event);
        }
        if (a->qualityController) {
            a->qualityController->processKeyReleasedEvent(event);
        }
    }

    if (rebuild) rebuildScene(a);
}

static void framebufferSizeCb(GLFWwindow* w, int width, int height)
{
    App* a = (App*)glfwGetWindowUserPointer(w);
    if (!a) return;
    glViewport(0, 0, width, height);
    a->camera.updateViewportResize(width, height);
}

static void applyOptions(App& app, const CommandLineOptions& options)
{
    if (options.hasFixtureIndex) app.testIndex = options.fixtureIndex;
    if (options.hasWires) app.quality.setWires(options.wires);
    if (options.hasSurfaces) app.quality.setSurfaces(options.surfaces);
    if (options.hasPoints) app.quality.setPoints(options.points);
    if (options.hasTessellationMode) app.tessellationMode = options.tessellationMode;
}

static int initGlfw(int width, int height, bool visible, GLFWwindow*& outWindow)
{
#ifdef __APPLE__
#ifdef GLFW_COCOA_CHDIR_RESOURCES
    glfwInitHint(GLFW_COCOA_CHDIR_RESOURCES, GLFW_FALSE);
#endif
#endif
    if (!glfwInit()) return 1;
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
#ifdef __APPLE__
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
#endif
    if (!visible) {
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
    }
    outWindow = glfwCreateWindow(width, height, "PolygonClippingExample", nullptr, nullptr);
    if (!outWindow) { glfwTerminate(); return 1; }
    glfwMakeContextCurrent(outWindow);
    if (visible) glfwSwapInterval(1);
    glewExperimental = GL_TRUE;
    if (glewInit() != GLEW_OK) { glfwDestroyWindow(outWindow); glfwTerminate(); return 1; }
    return 0;
}

static void setupGlResources(App& app)
{
    app.lineProg = buildLineProgram();
    app.constantProg = buildConstantProgram();
    glGenVertexArrays(1, &app.vao);
    glGenBuffers(1, &app.vboP);
    glGenBuffers(1, &app.vboC);
}

static void releaseGlResources(App& app)
{
    if (app.vboP) { glDeleteBuffers(1, &app.vboP); app.vboP = 0; }
    if (app.vboC) { glDeleteBuffers(1, &app.vboC); app.vboC = 0; }
    if (app.vao)  { glDeleteVertexArrays(1, &app.vao); app.vao = 0; }
    if (app.lineProg)     { glDeleteProgram(app.lineProg); app.lineProg = 0; }
    if (app.constantProg) { glDeleteProgram(app.constantProg); app.constantProg = 0; }
}

static int runOffline(const CommandLineOptions& options)
{
    const int width = 1280;
    const int height = 800;

    GLFWwindow* w = nullptr;
    if (initGlfw(width, height, false, w) != 0) {
        std::fprintf(stderr, "[PolygonClippingExample] Could not create offline OpenGL context\n");
        return 1;
    }

    App app;
    applyOptions(app, options);
    app.w = w;
    setupGlResources(app);
    glViewport(0, 0, width, height);
    app.camera.updateViewportResize(width, height);

    rebuildScene(&app);
    focusCameraOnCurrentScene(&app);

    glEnable(GL_DEPTH_TEST);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    Matrix4x4d mvp = app.camera.calculateProjectionMatrix();
    RendererConfiguration polygonQuality = app.quality.clone();
    polygonQuality.setSurfaces(polygonQuality.isSurfacesSet() && app.showFilled);

    if (app.showRef) drawReference(&app, mvp);
    if (app.showClip) drawPolygonWA(&app, mvp, app.clipper ? app.clipper->getClipPolyWA() : 0, 0.20f, 0.75f, 0.25f, 0.70f, 0.20f, 0.0f, 0.0f);
    if (app.showSubj) drawPolygonWA(&app, mvp, app.clipper ? app.clipper->getSubjectPolyWA() : 0, 0.80f, 0.74f, 0.20f, 0.82f, 0.56f, 0.0f, 0.0f);

    Bounds2D b = calculateBounds(&app);
    double panelWidth = b.maxX - b.minX; if (panelWidth < 6.0) panelWidth = 6.0;
    double panelDepth = b.maxY - b.minY; if (panelDepth < 6.0) panelDepth = 6.0;
    Matrix4x4d innerTransform = Matrix4x4d().translation(0.0, 0.0, -panelDepth * 1.25);
    Matrix4x4d outerTransform = Matrix4x4d().translation(panelWidth * 1.25, 0.0, 0.0);
    if (app.showInner) drawResultPolygon(&app, mvp.multiply(innerTransform), app.innerPolygon, &polygonQuality, 0.65f,0.65f,0.70f, 0.82f,0.58f,0.36f);
    if (app.showOuter) drawResultPolygon(&app, mvp.multiply(outerTransform), app.outerPolygon, &polygonQuality, 0.68f,0.78f,0.68f, 0.18f,0.72f,0.24f);

    glFinish();

    // Capture framebuffer
    RGBImageUncompressed image;
    image.init(width, height);
    unsigned char* buf = new unsigned char[(size_t)width * (size_t)height * 3u];
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, buf);

    // OpenGL pixel rows are bottom-to-top; flip to top-to-bottom for the image
    int pos = 0;
    for (int row = 0; row < height; row++) {
        int imageY = height - 1 - row;
        for (int x = 0; x < width; x++) {
            image.putPixel(x, imageY, (char)buf[pos], (char)buf[pos+1], (char)buf[pos+2]);
            pos += 3;
        }
    }
    delete[] buf;

    releaseGlResources(app);
    glfwDestroyWindow(w);
    glfwTerminate();

    bool ok = ImagePersistence::exportPNG(java::File("output.png"), &image);
    if (!ok) {
        std::fprintf(stderr, "[PolygonClippingExample] Failed to write output.png\n");
        return 1;
    }
    std::printf("[PolygonClippingExample] Exported output.png\n");
    return 0;
}

int main(int argc, char** argv)
{
    CommandLineOptions options;
    if (!CommandLineOptions::parse(argc, argv, options)) {
        return 1;
    }

    if (options.offlineMode) {
        return runOffline(options);
    }

    GLFWwindow* w = nullptr;
    if (initGlfw(1100, 900, true, w) != 0) return 1;

    App app;
    applyOptions(app, options);
    app.w = w;
    glfwSetWindowUserPointer(w, &app);
    glfwSetKeyCallback(w, keyCb);
    glfwSetFramebufferSizeCallback(w, framebufferSizeCb);

    setupGlResources(app);
    app.hud = new PolygonClippingHudRenderer();

    rebuildScene(&app);
    focusCameraOnCurrentScene(&app);

    while (!glfwWindowShouldClose(w)) {
        glEnable(GL_DEPTH_TEST);
        glClearColor(0,0,0,1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        int fbw = 1100;
        int fbh = 900;
        glfwGetFramebufferSize(w, &fbw, &fbh);
        app.camera.updateViewportResize(fbw, fbh);
        Matrix4x4d mvp = app.camera.calculateProjectionMatrix();
        RendererConfiguration polygonQuality = app.quality.clone();
        polygonQuality.setSurfaces(polygonQuality.isSurfacesSet() && app.showFilled);

        if (app.showRef) drawReference(&app, mvp);
        if (app.showClip) drawPolygonWA(&app, mvp, app.clipper ? app.clipper->getClipPolyWA() : 0, 0.20f, 0.75f, 0.25f, 0.70f, 0.20f, 0.0f, 0.0f);
        if (app.showSubj) drawPolygonWA(&app, mvp, app.clipper ? app.clipper->getSubjectPolyWA() : 0, 0.80f, 0.74f, 0.20f, 0.82f, 0.56f, 0.0f, 0.0f);
        Bounds2D b = calculateBounds(&app);
        double panelWidth = b.maxX - b.minX; if (panelWidth < 6.0) panelWidth = 6.0;
        double panelDepth = b.maxY - b.minY; if (panelDepth < 6.0) panelDepth = 6.0;
        Matrix4x4d innerTransform = Matrix4x4d().translation(0.0, 0.0, -panelDepth * 1.25);
        Matrix4x4d outerTransform = Matrix4x4d().translation(panelWidth * 1.25, 0.0, 0.0);
        if (app.showInner) drawResultPolygon(&app, mvp.multiply(innerTransform), app.innerPolygon, &polygonQuality, 0.65f,0.65f,0.70f, 0.82f,0.58f,0.36f);
        if (app.showOuter) drawResultPolygon(&app, mvp.multiply(outerTransform), app.outerPolygon, &polygonQuality, 0.68f,0.78f,0.68f, 0.18f,0.72f,0.24f);

        int vp[4] = {0,0,1,1};
        glGetIntegerv(GL_VIEWPORT, vp);
        java::String line1 = buildHudLine1(&app);
        java::String line2 = buildHudLine2(&app);
        java::String line3 = buildHudLine3(&app);
        java::String line4 = buildHudLine4(&app);
        java::String line5 = java::String("");
        java::String right1 = java::String("Loops C/S/I/O: ")
            + java::String::valueOf(loopCount(app.clipPolygon)) + "/"
            + java::String::valueOf(loopCount(app.subjectPolygon)) + "/"
            + java::String::valueOf(loopCount(app.innerPolygon)) + "/"
            + java::String::valueOf(loopCount(app.outerPolygon));
        java::String right2 = java::String("Intersections: ")
            + java::String::valueOf(countPairedVertices(app.clipper ? app.clipper->getSubjectPolyWA() : 0));
        java::String right3 = java::String("Fullscreen [F]  Snapshot [H]  Quality [F1/F2/F3]");
        app.hud->draw(vp[0], vp[1], vp[2], vp[3], line1, line2, line3, line4, line5, right1, right2, right3);

        glfwSwapBuffers(w);
        glfwPollEvents();
    }

    releaseGlResources(app);
    glfwDestroyWindow(w);
    glfwTerminate();
    return 0;
}
