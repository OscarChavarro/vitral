#include "ShadersModel.h"

#include <algorithm>
#include <cmath>
#include <fstream>
#include <sstream>
#include <vector>

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
      cookTorranceMaterial(0),
      cookTorranceMaterialIndex(0),
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
    cookTorranceMaterial = new MicroFacetedMaterial(
        "../../../../etc/materials/microFacetMAterials.csv",
        "Copper");

    std::ifstream csv("../../../../etc/materials/microFacetMAterials.csv");
    if ( csv.good() ) {
        std::string line;
        if ( std::getline(csv, line) ) {
            while ( std::getline(csv, line) ) {
                if ( line.empty() ) continue;
                std::stringstream ss(line);
                std::string token;
                std::vector<std::string> cols;
                while ( std::getline(ss, token, ',') ) cols.push_back(token);
                if ( cols.size() < 16 ) continue;
                const std::string& name = cols[0];
                if ( name.empty() ) continue;
                cookTorranceMaterialNames.push_back(name);
                if ( name == "Copper" ) {
                    cookTorranceMaterialIndex = (int)cookTorranceMaterialNames.size() - 1;
                }
            }
        }
    }

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
    delete cookTorranceMaterial;
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
const SimpleMaterial& ShadersModel::getActiveMaterialForCurrentShading() const
{
    if ( quality.getShadingType() == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE &&
         cookTorranceMaterial != 0 ) {
        return *cookTorranceMaterial;
    }
    return material;
}

SimpleMaterial* ShadersModel::createActiveMaterialCopy() const
{
    if ( quality.getShadingType() == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE &&
         cookTorranceMaterial != 0 ) {
        return new MicroFacetedMaterial(*cookTorranceMaterial);
    }
    return new SimpleMaterial(material);
}

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

std::string ShadersModel::getCookTorranceMaterialLabel() const
{
    if ( cookTorranceMaterial == 0 || cookTorranceMaterial->getName().empty() ) return "Copper";
    return cookTorranceMaterial->getName();
}

void ShadersModel::cycleCookTorranceMaterial()
{
    if ( cookTorranceMaterialNames.empty() ) return;
    cookTorranceMaterialIndex = (cookTorranceMaterialIndex + 1) % (int)cookTorranceMaterialNames.size();
    const std::string& name = cookTorranceMaterialNames[(size_t)cookTorranceMaterialIndex];
    delete cookTorranceMaterial;
    cookTorranceMaterial = new MicroFacetedMaterial(
        "../../../../etc/materials/microFacetMAterials.csv",
        name);
}
