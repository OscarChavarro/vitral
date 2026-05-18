#include <cstdio>
#include <cmath>
#include <string>

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/java/io/File.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/Rasterizer2D.h"
#include "vsdk/toolkit/render/WireframeRenderer.h"

static void rasterOutput(Camera* camera, Calligraphic2DBuffer* lineSet, const std::string& outputFile)
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

int main(int argc, char** argv)
{
    std::string sceneFile = "../../../../etc/geometry/cow.obj";
    std::string outputFile = "output.png";
    if (argc > 1 && argv[1] != 0 && std::string(argv[1]).size() > 0) {
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
    camera.updateViewportResize(640, 480);

    SimpleScene scene;
    EnvironmentPersistence::importEnvironment(java::File(sceneFile.c_str()), &scene);
    if (scene.getSimpleBodies().empty()) {
        std::fprintf(stderr, "Failed to load scene file: %s\n", sceneFile.c_str());
        return 1;
    }

    SimpleBody* boxBody = new SimpleBody();
    boxBody->setGeometry(new Box(Vector3Dd(1, 1, 1)));
    boxBody->setPosition(Vector3Dd(1, 2, 3));
    scene.addBody(boxBody);

    Calligraphic2DBuffer lineSet;
    WireframeRenderer::execute(&lineSet, scene.getSimpleBodies(), &camera);
    rasterOutput(&camera, &lineSet, outputFile);

    std::printf("Resulting image has been written to \"%s\"\n", outputFile.c_str());
    return 0;
}
