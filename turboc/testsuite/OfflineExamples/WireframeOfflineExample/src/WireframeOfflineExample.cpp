#include <cmath>
#include <cstdio>

#include "java/io/File.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/hiddenLine/WireframeRenderer.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"
static void
appendLineSet(Calligraphic2DBuffer* destination, const Calligraphic2DBuffer* source)
{
    if (destination == 0 || source == 0) {
        return;
    }

    for (int j = 0; j < source->getNumLines(); j++) {
        Vector3Dd* e0 = source->get2DLinePoint0(j);
        Vector3Dd* e1 = source->get2DLinePoint1(j);
        if (e0 != 0 && e1 != 0) {
            destination->add2DLine(*e0, *e1);
        }
        delete e0;
        delete e1;
    }
}

static void
rasterOutput(Camera* camera, Calligraphic2DBuffer* lineSet, const java::String& outputFile)
{
    RGBImageUncompressed outputImage;
    outputImage.init((int)camera->getViewportXSize(), (int)camera->getViewportYSize());

    RGBPixel white((char)255, (char)255, (char)255);
    const double xt = outputImage.getXSize();
    const double yt = outputImage.getYSize();

    for (int j = 0; j < lineSet->getNumLines(); j++) {
        Vector3Dd* e0 = lineSet->get2DLinePoint0(j);
        Vector3Dd* e1 = lineSet->get2DLinePoint1(j);
        if (e0 == 0 || e1 == 0) {
            delete e0;
            delete e1;
            continue;
        }

        int x0 = (int)((xt - 1) * ((e0->x() + 1.0) / 2.0));
        int y0 = (int)((yt - 1) * (1.0 - ((e0->y() + 1.0) / 2.0)));
        int x1 = (int)((xt - 1) * ((e1->x() + 1.0) / 2.0));
        int y1 = (int)((yt - 1) * (1.0 - ((e1->y() + 1.0) / 2.0)));
        Rasterizer2D::drawLine(&outputImage, x0, y0, x1, y1, white);

        delete e0;
        delete e1;
    }

    ImagePersistence::exportPNG(java::File(outputFile.c_str()), &outputImage);
}

int
main(int argc, char** argv)
{
    java::String sceneFile = "../../../../etc/geometry/cow.obj";
    java::String outputFile = "output.png";
    if (argc > 1 && argv[1] != 0 && java::String(argv[1]).size() > 0) {
        outputFile = argv[1];
    }

    Camera camera;
    Matrix4x4d R;
    const double pi = std::acos(-1.0);
    camera.setPosition(Vector3Dd(7, -4, 4));
    R = R.eulerAnglesRotation(140.0 * pi / 180.0, -30.0 * pi / 180.0, 0);
    camera.setNearPlaneDistance(0.001);
    camera.setFarPlaneDistance(100);
    camera.setRotation(R);
    camera.updateViewportResize(1024, 768);
    SimpleScene scene;
    EnvironmentPersistence::importEnvironment(java::File(sceneFile.c_str()), &scene);
    if (scene.getSimpleBodies().size() == 0) {
        std::fprintf(stderr, "Failed to load scene file: %s\n", sceneFile.c_str());
        return 1;
    }

    SimpleBody* arrowBody = new SimpleBody();
    arrowBody->setGeometry(new Arrow(1.0, 0.5, 0.15, 0.3));
    arrowBody->setRotation(Matrix4x4d().axisRotation(-30.0 * pi / 180.0, 1.0, 0.0, 0.0));
    arrowBody->setPosition(Vector3Dd(1, 2, 1.2));
    arrowBody->setScale(Vector3Dd(2, 2, 2));
    scene.addBody(arrowBody);

    java::ArrayList<SimpleBody*> wireframeBodies;
    java::ArrayList<SimpleBody*>& sceneBodies = scene.getSimpleBodies();
    for (long int i = 0; i < sceneBodies.size(); i++) {
        SimpleBody* body = sceneBodies.get(i);
        if (body != 0 && body != arrowBody) {
            wireframeBodies.add(body);
        }
    }

    java::ArrayList<SimpleBody*> hiddenLineBodies;
    hiddenLineBodies.add(arrowBody);

    Calligraphic2DBuffer cowWireframe;
    Calligraphic2DBuffer arrowVisibleContour;
    Calligraphic2DBuffer arrowVisibleNonContour;
    Calligraphic2DBuffer arrowHidden;
    Calligraphic2DBuffer finalLineSet;

    WireframeRenderer::execute(&cowWireframe, wireframeBodies, &camera);
    HiddenLineRenderer::executeAppelAlgorithm(
        hiddenLineBodies,
        &camera,
        &arrowVisibleContour,
        &arrowVisibleNonContour,
        &arrowHidden);

    appendLineSet(&finalLineSet, &cowWireframe);
    appendLineSet(&finalLineSet, &arrowVisibleContour);
    appendLineSet(&finalLineSet, &arrowVisibleNonContour);
    rasterOutput(&camera, &finalLineSet, outputFile);

    std::printf("Resulting image has been written to \"%s\"\n", outputFile.c_str());
    return 0;
}
