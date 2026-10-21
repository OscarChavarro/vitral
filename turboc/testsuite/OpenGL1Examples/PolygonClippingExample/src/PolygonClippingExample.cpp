#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

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
#include "vsdk/toolkit/gui/GlutSystem.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Polygon2DRenderer.h"
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
    int testIndex;
    Operation operation;
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

    App() : testIndex(0), operation(INTERSECTION),
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

static Polygon2D* buildPolygonFromEncoded(const char* loopsEncoded)
{
    Polygon2D* p = new Polygon2D();
    for (long int i = 0; i < p->loops.size(); ++i) delete p->loops[i];
    p->loops.clear();
    if (!loopsEncoded || !(*loopsEncoded)) { p->nextLoop(); return p; }

    long int len = (long int)strlen(loopsEncoded);
    char* tmp = new char[len + 1];
    memcpy(tmp, loopsEncoded, len + 1);

    char* cursor = tmp;
    bool firstLoop = true;
    while (*cursor) {
        char* nextSep = strchr(cursor, ';');
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
            double val = strtod(s, &endp);
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
    if (a->operation == INTERSECTION) {
        a->clipper->clipPolygons(a->clipPolygon, a->subjectPolygon, a->innerPolygon, a->outerPolygon);
    }
    else if (a->operation == UNION) {
        a->clipper->unionPolygons(a->clipPolygon, a->subjectPolygon, a->innerPolygon);
        for (long int i = 0; i < a->outerPolygon->loops.size(); ++i) delete a->outerPolygon->loops[i];
        a->outerPolygon->loops.clear();
        a->outerPolygon->nextLoop();
    }
    else if (a->operation == A_MINUS_B) {
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
    OpenGL1Polygon2DRenderer::draw(mvp, &axis, &a->quality, 0.4f,0.1f,0.1f, 1.0f,0.2f,0.2f);
}

static void drawResultPolygon(App* a, const Matrix4x4d& mvp, Polygon2D* polygon,
    RendererConfiguration* polygonQuality,
    float fillR, float fillG, float fillB,
    float lineR, float lineG, float lineB)
{
    if (a->tessellationMode == PolygonSurfaceTessellationMode::MONOTONE_DECOMPOSITION) {
        PolygonTriangularRenderer::fillPolygonSurface(
            mvp, polygon, polygonQuality,
            fillR, fillG, fillB, lineR, lineG, lineB);
    } else {
        OpenGL1Polygon2DRenderer::draw(mvp, polygon, polygonQuality,
            fillR, fillG, fillB, lineR, lineG, lineB);
    }
}

static String onOff(bool v)
{
    return v ? String("ON") : String("OFF");
}

static String operationName(Operation op)
{
    if (op == INTERSECTION) return String("INTERSECTION");
    if (op == UNION) return String("UNION");
    if (op == A_MINUS_B) return String("A_MINUS_B");
    return String("B_MINUS_A");
}

static int loopCount(Polygon2D* p)
{
    if (!p) return 0;
    return (int)p->loops.size();
}

static String buildHudLine1(const App* a)
{
    const PolygonClippingTestCase& t = PolygonClippingFixtures::CASES[a->testIndex];
    String line = String("Test [1,2]: ") + String(t.name)
        + " (" + String::valueOf(a->testIndex + 1) + "/" + String::valueOf((int)PolygonClippingFixtures::COUNT) + ")";
    line += "  Op [3]: " + operationName(a->operation);
    return line;
}

static String buildHudLine2(const App* a)
{
    String line = String("Clip [C]: ")
        + onOff(a->showClip)
        + "  Subject [S]: " + onOff(a->showSubj)
        + "  Points [P]: " + onOff(a->showIntersections);
    return line;
}

static String buildHudLine3(const App* a)
{
    String base;
    if (a->operation == INTERSECTION) {
        base = String("Inner [I]: ") + onOff(a->showInner)
            + "  Outer [O]: " + onOff(a->showOuter);
    } else {
        base = String("Result [I]: ") + onOff(a->showInner)
            + "  Secondary [O]: " + onOff(a->showOuter);
    }
    return base
        + "  Fill [t]: " + onOff(a->showFilled)
        + "  Tess [T]: " + String(tessellationModeDisplayName(a->tessellationMode));
}

static String buildHudLine4(const App* a)
{
    return String("Reference frame [Space]: ") + onOff(a->showRef);
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

static void push3(ArrayList<float>& a, float x, float y, float z) { a.add(x); a.add(y); a.add(z); }

static void drawPolygonWA(App* a, const Matrix4x4d& mvp, _Polygon2DWA* polygon,
    float lineR, float lineG, float lineB, float pointR, float pointG, float tx, float tz)
{
    if (!polygon) return;
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContourWA* contour = polygon->loops.get(i);
        if (!contour || !contour->vertices.getHead()) continue;
        ArrayList<float> linePos, lineCol, pointPos, pointCol;
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

        glLineWidth(2.0f);
        PolygonTriangularRenderer::drawColoredArrays(mvp, GL_LINES,
            linePos.data(), lineCol.data(), (int)(linePos.size() / 3));
        glLineWidth(1.0f);

        if (a->showIntersections) {
            glPointSize(8.0f);
            PolygonTriangularRenderer::drawColoredArrays(mvp, GL_POINTS,
                pointPos.data(), pointCol.data(), (int)(pointPos.size() / 3));
            glPointSize(1.0f);
        }
    }
}

static App* app = 0;

static void releaseGlResources(App& app);

static void requestClose()
{
    // Classic GLUT never returns from its main loop
    releaseGlResources(*app);
    delete app;
    app = 0;
    exit(0);
}

/**
Shortcuts of the example; GLUT gives the characters, so `T` and `t` are
told apart directly.
@return true if the key was a letter shortcut (not given to the camera)
*/
static bool processShortcut(App* a, unsigned char key, bool& rebuild)
{
    switch (key) {
      case 27: requestClose(); return true;
      case '1': a->testIndex = (a->testIndex + PolygonClippingFixtures::COUNT - 1) % PolygonClippingFixtures::COUNT; rebuild = true; return false;
      case '2': a->testIndex = (a->testIndex + 1) % PolygonClippingFixtures::COUNT; rebuild = true; return false;
      case '3': a->operation = (Operation)(((int)a->operation + 1) % 4); rebuild = true; return false;
      case ' ': a->showRef = !a->showRef; return false;
      case 'c': case 'C': a->showClip = !a->showClip; return true;
      case 's': case 'S': a->showSubj = !a->showSubj; return true;
      case 'i': case 'I': a->showInner = !a->showInner; return true;
      case 'o': case 'O': a->showOuter = !a->showOuter; return true;
      case 'p': case 'P': a->showIntersections = !a->showIntersections; return true;
      // Uppercase T: cycle surface tessellation mode (GLU vs monotone decomposition)
      case 'T': a->tessellationMode = nextTessellationMode(a->tessellationMode); return true;
      // Lowercase t: toggle whether polygon surfaces are filled
      case 't': a->showFilled = !a->showFilled; return true;
      case 'g': case 'G': a->showFilled = !a->showFilled; return true;
      case 'f': case 'F': return true;
      case 'h': case 'H': return true;
      default: return false;
    }
}

static void keyPressed(const KeyEvent& event, bool handledLetterShortcut)
{
    if (!handledLetterShortcut && app->cameraController) {
        app->cameraController->processKeyPressedEvent(event);
    }
    if (app->qualityController) {
        app->qualityController->processKeyPressedEvent(event);
    }
    glutPostRedisplay();
}

static void keyReleased(const KeyEvent& event)
{
    if (app->cameraController) {
        app->cameraController->processKeyReleasedEvent(event);
    }
    if (app->qualityController) {
        app->qualityController->processKeyReleasedEvent(event);
    }
    glutPostRedisplay();
}

static void keyboardCb(unsigned char key, int, int)
{
    bool rebuild = false;
    bool handledLetterShortcut = processShortcut(app, key, rebuild);
    keyPressed(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()), handledLetterShortcut);
    if (rebuild) rebuildScene(app);
}

static void keyboardUpCb(unsigned char key, int, int)
{
    keyReleased(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void specialCb(int key, int, int)
{
    keyPressed(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()), false);
}

static void specialUpCb(int key, int, int)
{
    keyReleased(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void reshapeCb(int width, int height)
{
    glViewport(0, 0, width, height);
    app->camera.updateViewportResize(width, height);
    glutPostRedisplay();
}

static void applyOptions(App& app, const CommandLineOptions& options)
{
    if (options.hasFixtureIndex) app.testIndex = options.fixtureIndex;
    if (options.hasWires) app.quality.setWires(options.wires);
    if (options.hasSurfaces) app.quality.setSurfaces(options.surfaces);
    if (options.hasPoints) app.quality.setPoints(options.points);
    if (options.hasTessellationMode) app.tessellationMode = options.tessellationMode;
}

static bool initGlut(int* argc, char** argv, int width, int height, bool shrinkToScreen)
{
    glutInit(argc, argv);
    glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH);
    if (shrinkToScreen) {
        // At most the screen, leaving room for the window manager
        // decorations (i.e. on 800x600 displays)
        int screenWidth = glutGet(GLUT_SCREEN_WIDTH);
        int screenHeight = glutGet(GLUT_SCREEN_HEIGHT);
        if (screenWidth > 0 && width > screenWidth - 32) width = screenWidth - 32;
        if (screenHeight > 0 && height > screenHeight - 64) height = screenHeight - 64;
    }
    glutInitWindowSize(width, height);
    glutCreateWindow("PolygonClippingExample");
    return OpenGL1Loader::load();
}

static void releaseGlResources(App& app)
{
    if (app.hud) app.hud->dispose();
    OpenGL1CameraRenderer::dispose();
    OpenGL1ImageRenderer::dispose();
}

static void drawScene(App& app)
{
    glEnable(GL_DEPTH_TEST);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    Matrix4x4d mvp = OpenGL1CameraRenderer::activate(&app.camera);
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
}

static void drawHud(App& app)
{
    int vp[4] = {0,0,1,1};
    glGetIntegerv(GL_VIEWPORT, vp);
    String line1 = buildHudLine1(&app);
    String line2 = buildHudLine2(&app);
    String line3 = buildHudLine3(&app);
    String line4 = buildHudLine4(&app);
    String line5 = String("");
    String right1 = String("Loops C/S/I/O: ")
        + String::valueOf(loopCount(app.clipPolygon)) + "/"
        + String::valueOf(loopCount(app.subjectPolygon)) + "/"
        + String::valueOf(loopCount(app.innerPolygon)) + "/"
        + String::valueOf(loopCount(app.outerPolygon));
    String right2 = String("Intersections: ")
        + String::valueOf(countPairedVertices(app.clipper ? app.clipper->getSubjectPolyWA() : 0));
    String right3 = String("Fullscreen [F]  Snapshot [H]  Quality [F1/F2/F3]");
    app.hud->draw(vp[0], vp[1], vp[2], vp[3], line1, line2, line3, line4, line5, right1, right2, right3);
}

static const int OFFLINE_WIDTH = 1280;
static const int OFFLINE_HEIGHT = 800;

/**
Draws the scene once in the back buffer, exports it to output.png and ends:
GLUT has no hidden windows, so a normal one gives the context.
*/
static void offlineDisplayCb()
{
    int width = OFFLINE_WIDTH;
    int height = OFFLINE_HEIGHT;
    glViewport(0, 0, width, height);
    app->camera.updateViewportResize(width, height);
    drawScene(*app);
    glFinish();

    // Capture framebuffer
    RGBImageUncompressed image;
    image.init(width, height);
    unsigned char* buf = new unsigned char[(size_t)width * (size_t)height * 3u];
    glReadBuffer(GL_BACK);
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

    releaseGlResources(*app);

    bool ok = ImagePersistence::exportPNG(File("output.png"), &image);
    if (!ok) {
        fprintf(stderr, "[PolygonClippingExample] Failed to write output.png\n");
        exit(1);
    }
    printf("[PolygonClippingExample] Exported output.png\n");
    exit(0);
}

static void displayCb()
{
    drawScene(*app);
    drawHud(*app);
    glutSwapBuffers();
}

int main(int argc, char** argv)
{
    CommandLineOptions options;
    if (!CommandLineOptions::parse(argc, argv, options)) {
        return 1;
    }

    int width = options.offlineMode ? OFFLINE_WIDTH : 1100;
    int height = options.offlineMode ? OFFLINE_HEIGHT : 900;
    if (!initGlut(&argc, argv, width, height, !options.offlineMode)) {
        fprintf(stderr, "[PolygonClippingExample] Could not create OpenGL context\n");
        return 1;
    }

    app = new App();
    applyOptions(*app, options);
    rebuildScene(app);
    focusCameraOnCurrentScene(app);

    if (options.offlineMode) {
        glutDisplayFunc(offlineDisplayCb);
    }
    else {
        app->hud = new PolygonClippingHudRenderer();
        glutDisplayFunc(displayCb);
        glutReshapeFunc(reshapeCb);
        glutKeyboardFunc(keyboardCb);
        glutKeyboardUpFunc(keyboardUpCb);
        glutSpecialFunc(specialCb);
        glutSpecialUpFunc(specialUpCb);
    }

    glutMainLoop();
    return 0;
}
