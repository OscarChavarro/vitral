#include "ShadersModel.h"

#include <algorithm>
#include <cmath>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/java/io/File.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"

ShadersModel ShadersModel::createDefault() { return ShadersModel(); }

ShadersModel::ShadersModel()
    : camera(new Camera()),
      cameraController(0),
      qualityController(0),
      sphere(new Sphere(1.0)),
      light(new Light(LightType::POINT, Vector3Dd(1, -3, 1), ColorRgb(1, 1, 1))),
      textureMap(0), bumpMapHeightRgb(0), bumpNormalMap(0), softwareFrameImage(0),
      renderingMode(ShaderOperationMode::OPENGL_4_1), showHud(true),
      animationEnabled(false), lightAnimationEnabled(false), sphereRotationAngleRadians(0.0),
      sphereMeridians(100), sphereParallels(50)
{
    camera->setPosition(Vector3Dd(0, -4, 0));
    camera->setRotation(Matrix4x4d().eulerAnglesRotation(M_PI / 2.0, 0.0, 0.0));
    camera->setFov(30.0);

    cameraController = new CameraControllerAquynza(camera);
    quality.setTexture(true);
    quality.setBumpMap(true);
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_PHONG);
    qualityController = new RendererConfigurationController(&quality);

    light->setId(0);
    material = material.withAmbient(ColorRgb(0.1, 0.1, 0.1));
    material = material.withDiffuse(ColorRgb(1, 1, 1));
    material = material.withSpecular(ColorRgb(1, 1, 1));
    material = material.withPhongExponent(40);

    try {
        java::File textureFile("../../../../etc/textures/miniearth.png");
        textureMap = ImagePersistence::importRGB(textureFile);
        java::File bumpFile("../../../../etc/bumpmaps/earth.bw");
        IndexedColorImageUncompressed* bump = ImagePersistence::importIndexedColor(bumpFile);
        bumpNormalMap = new NormalMap();
        bumpNormalMap->importBumpMap(bump, Vector3Dd(1.0, 1.0, 1.0));
        bumpMapHeightRgb = bumpNormalMap->exportToRgbImage();
        delete bump;
    }
    catch (...) {
    }

    updateSoftwareViewportAndCamera(1100, 900);
}

ShadersModel::~ShadersModel()
{
    delete qualityController;
    delete cameraController;
    delete light;
    delete sphere;
    delete textureMap;
    delete bumpMapHeightRgb;
    delete bumpNormalMap;
    delete softwareFrameImage;
    delete camera;
}

void ShadersModel::rotateRenderingMode() { renderingMode = nextShaderOperationMode(renderingMode); }
void ShadersModel::toggleShowHud() { showHud = !showHud; }
void ShadersModel::toggleAnimationEnabled() { animationEnabled = !animationEnabled; }
void ShadersModel::toggleLightAnimationEnabled() { lightAnimationEnabled = !lightAnimationEnabled; }
void ShadersModel::changeSphereMeridians(int delta) { sphereMeridians = std::max(12, sphereMeridians + delta); }
void ShadersModel::changeSphereParallels(int delta) { sphereParallels = std::max(8, sphereParallels + delta); }

static double normalizeAngle(double a) {
    const double twoPi = 2.0 * M_PI;
    double n = std::fmod(a, twoPi);
    if (n < 0.0) n += twoPi;
    return n;
}

void ShadersModel::setSphereRotationAngleRadians(double angle) { sphereRotationAngleRadians = normalizeAngle(angle); }
void ShadersModel::advanceSphereRotationRadians(double delta) { setSphereRotationAngleRadians(sphereRotationAngleRadians + delta); }

void ShadersModel::updateSoftwareViewportAndCamera(int width, int height)
{
    const int w = std::max(1, width);
    const int h = std::max(1, height);
    camera->updateViewportResize(w, h);
    if (softwareFrameImage && softwareFrameImage->getXSize() == w && softwareFrameImage->getYSize() == h) return;
    delete softwareFrameImage;
    softwareFrameImage = new RGBImageUncompressed();
    softwareFrameImage->init(w, h);
}

std::string ShadersModel::getCookTorranceMaterialLabel() const { return "Copper"; }
void ShadersModel::cycleCookTorranceMaterial() {}
