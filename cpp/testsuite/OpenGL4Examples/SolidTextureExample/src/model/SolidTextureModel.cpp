#include <algorithm>
#include <cmath>

#include "java/util/ArrayList.txx"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/element/Intersection.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/IndexedColorImageHDRUncompressed.h"
#include "vsdk/toolkit/media/RGBAColorPalette.h"
#include "vsdk/toolkit/media/RGBAPixelHDR.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ControlledRGBAImageHDRUncompressed.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureInterpolationTypes.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureProjectionMethods.h"
#include "vsdk/toolkit/media/solidTexture/procedural/ProceduralNoise.h"
#include "model/SolidTextureModel.h"

const double SolidTextureModel::SMALL_TOLERANCE = 1.0e-6;

SolidTextureModel::SolidTextureModel()
    : rayGizmo([this](const Ray& ray) { return makeIntersectionCallback(ray); }, 1),
      colorTextureFixture(0), solidTextureSize(32), solidTextureRevision(0),
      selectedSolidTexture(SolidTextureExampleColorNames::CHECKER_TEXTURE),
      animationEnabled(false), hudVisible(true),
      operationMode(OperationMode::MESH_MODEL),
      tangibleServiceUrl("ws://localhost:8090/v1/values")
{
    rayGizmo.setVisible(false);
    textureUtils.initialize(nullptr);
    textureUtils.getProceduralNoise().initialize();
    colorTextureFixture =
        new ColorTextureFixture(&textureUtils.getProceduralNoise(),
                                &textureUtils);
    rebuildTexture2DStack();

    Light* light0 = new PointLight(Vector3Dd(10, -20, 50), ColorRgb(1, 1, 1));
    light0->setId(0);
    Light* light1 = new PointLight(Vector3Dd(-10, 20, 50), ColorRgb(1, 1, 1));
    light1->setId(1);
    lights.add(light0);
    lights.add(light1);
}

SolidTextureModel::~SolidTextureModel()
{
    clearTexture2DStack();
    for ( long i = 0; i < lights.size(); i++ ) delete lights.get(i);
    delete colorTextureFixture;
}

Camera* SolidTextureModel::getCamera() { return &camera; }
java::ArrayList<Light*>& SolidTextureModel::getLights() { return lights; }
SimpleScene* SolidTextureModel::getScene() { return &scene; }
RendererConfiguration* SolidTextureModel::getQualitySelection() { return &qualitySelection; }
RayGizmo* SolidTextureModel::getRayGizmo() { return &rayGizmo; }
InfinitePlaneGizmo* SolidTextureModel::getInfinitePlaneGizmo() { return &infinitePlaneGizmo; }
java::ArrayList<Image*>& SolidTextureModel::getTexture2DStack() { return texture2DStack; }
const std::vector<unsigned char>& SolidTextureModel::getSolidTextureVolumeRgb8() const { return solidTextureVolumeRgb8; }
long SolidTextureModel::getSolidTextureRevision() const { return solidTextureRevision; }
int SolidTextureModel::getSolidTextureSize() const { return solidTextureSize; }
SolidTextureExampleColorNames SolidTextureModel::getSelectedSolidTexture() const { return selectedSolidTexture; }
bool SolidTextureModel::isAnimationEnabled() const { return animationEnabled; }
void SolidTextureModel::setAnimationEnabled(bool v) { animationEnabled = v; }
void SolidTextureModel::toggleAnimationEnabled() { animationEnabled = !animationEnabled; }
bool SolidTextureModel::isHudVisible() const { return hudVisible; }
void SolidTextureModel::toggleHudVisible() { hudVisible = !hudVisible; }
OperationMode SolidTextureModel::getOperationMode() const { return operationMode; }
const java::String& SolidTextureModel::getTangibleServiceUrl() const { return tangibleServiceUrl; }

void SolidTextureModel::setTangibleServiceUrl(const java::String& url)
{
    if ( !url.empty() ) tangibleServiceUrl = url;
}

void SolidTextureModel::advanceObjectRotationRadians(double deltaRadians)
{
    Matrix4x4d deltaRotation;
    deltaRotation.axisRotation(deltaRadians, 0.0, 0.0, 1.0);
    java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        body->setRotation(deltaRotation.multiply(body->getRotation()));
    }
}

void SolidTextureModel::selectNextSolidTexture()
{
    selectedSolidTexture = nextSolidTextureExampleColorName(selectedSolidTexture);
    rebuildTexture2DStack();
}

void SolidTextureModel::selectPreviousSolidTexture()
{
    selectedSolidTexture = previousSolidTextureExampleColorName(selectedSolidTexture);
    rebuildTexture2DStack();
}

void SolidTextureModel::increaseSolidTextureSize()
{
    if ( solidTextureSize >= MAX_SOLID_TEXTURE_SIZE ) return;
    solidTextureSize *= 2;
    rebuildTexture2DStack();
}

void SolidTextureModel::decreaseSolidTextureSize()
{
    if ( solidTextureSize <= MIN_SOLID_TEXTURE_SIZE ) return;
    solidTextureSize /= 2;
    rebuildTexture2DStack();
}

void SolidTextureModel::rotateOperationMode()
{
    operationMode = operationMode == OperationMode::MESH_MODEL ?
        OperationMode::TEXTURE_2D_STACK : OperationMode::MESH_MODEL;
}

void SolidTextureModel::clearTexture2DStack()
{
    for ( long i = 0; i < texture2DStack.size(); i++ ) delete texture2DStack.get(i);
    texture2DStack.clear();
}

void SolidTextureModel::rebuildTexture2DStack()
{
    clearTexture2DStack();
    int side = std::max(1, solidTextureSize);
    solidTextureVolumeRgb8.assign((size_t)side * side * side * 3, 0);
    for ( int i = 0; i < solidTextureSize; i++ ) {
        texture2DStack.add(generateTextureSlice(i));
    }
    solidTextureRevision++;
}

Image* SolidTextureModel::generateTextureSlice(int sliceIndex)
{
    RGBImageUncompressed* image = new RGBImageUncompressed();
    int side = std::max(1, solidTextureSize);
    image->initNoFill(side, side);
    double z = unitCoordinate(sliceIndex, solidTextureSize);
    ControlledRGBAImageHDRUncompressed* imageMap = 0;
    ControlledRGBAImageHDRUncompressed* materialMap = 0;
    if ( selectedSolidTexture == SolidTextureExampleColorNames::IMAGE_MAP_TEXTURE ) {
        imageMap = buildImageMapTexture();
    }
    if ( selectedSolidTexture == SolidTextureExampleColorNames::MATERIAL_MAP_TEXTURE ) {
        materialMap = buildMaterialMapTexture();
    }
    for ( int y = 0; y < side; y++ ) {
        double py = unitCoordinate(y, side);
        for ( int x = 0; x < side; x++ ) {
            double px = unitCoordinate(x, side);
            ColorRgba color = colorAt(px, py, z, imageMap, materialMap);
            image->putPixel(x, y, (char)channel16To8(color.getR()),
                            (char)channel16To8(color.getG()),
                            (char)channel16To8(color.getB()));
            putVolumeColor16As8(side, x, y, sliceIndex, color);
        }
    }
    delete imageMap;
    delete materialMap;
    return image;
}

ColorRgba SolidTextureModel::colorAt(
    double x, double y, double z,
    ControlledRGBAImageHDRUncompressed* imageMap,
    ControlledRGBAImageHDRUncompressed* materialMap)
{
    ColorRgba color;
    RGBAColorPalette* palette = defaultPalette();
    switch ( selectedSolidTexture ) {
      case SolidTextureExampleColorNames::NO_TEXTURE:
        color = ColorRgba(0.18, 0.18, 0.18, 1.0); break;
      case SolidTextureExampleColorNames::COLOUR_TEXTURE:
        color = ColorRgba(0.72, 0.38, 0.18, 1.0); break;
      case SolidTextureExampleColorNames::BOZO_TEXTURE:
        colorTextureFixture->bozo(x * 6.0, y * 6.0, z * 6.0, 1.25, 7, palette, &color); break;
      case SolidTextureExampleColorNames::MARBLE_TEXTURE:
        colorTextureFixture->marble(x * 8.0, y * 8.0, z * 8.0, 1.65, 7, palette, &color); break;
      case SolidTextureExampleColorNames::WOOD_TEXTURE: {
        RGBAColorPalette* p = woodPalette();
        colorTextureFixture->wood(x * 9.0, y * 9.0, z * 9.0, 2.2, 6, p, &color);
        delete p; break;
      }
      case SolidTextureExampleColorNames::CHECKER_TEXTURE:
      {
        ColorRgba c1(0.95, 0.95, 0.95, 1.0), c2(0.05, 0.08, 0.12, 1.0);
        colorTextureFixture->checker(x * 8.0, y * 8.0, z * 8.0, &color,
            &c1, &c2,
            SMALL_TOLERANCE); break;
      }
      case SolidTextureExampleColorNames::CHECKER_TEXTURE_TEXTURE:
        checkerTextureTexture(x, y, z, &color); break;
      case SolidTextureExampleColorNames::SPOTTED_TEXTURE:
        colorTextureFixture->spotted(x * 7.0, y * 7.0, z * 7.0, palette, &color); break;
      case SolidTextureExampleColorNames::AGATE_TEXTURE: {
        RGBAColorPalette* p = agatePalette();
        colorTextureFixture->agate(x * 8.0, y * 8.0, z * 8.0, 7, p, &color);
        delete p; break;
      }
      case SolidTextureExampleColorNames::GRANITE_TEXTURE: {
        RGBAColorPalette* p = granitePalette();
        colorTextureFixture->granite(x * 6.0, y * 6.0, z * 6.0, p, &color);
        delete p; break;
      }
      case SolidTextureExampleColorNames::GRADIENT_TEXTURE:
        colorTextureFixture->gradient(x * 4.0, y * 4.0, z * 4.0, 0.45,
            palette, Vector3Dd(1.0, 1.0, 1.0), 5, &color); break;
      case SolidTextureExampleColorNames::IMAGE_MAP_TEXTURE:
        imageTexture.imageMap(x, y, z, imageMap, &color, SMALL_TOLERANCE); break;
      case SolidTextureExampleColorNames::ONION_TEXTURE:
        colorTextureFixture->onion(x * 10.0, y * 10.0, z * 10.0, 0.65, 6, palette, &color); break;
      case SolidTextureExampleColorNames::LEOPARD_TEXTURE: {
        RGBAColorPalette* p = leopardPalette();
        colorTextureFixture->leopard(x * 12.0, y * 12.0, z * 12.0, 1.0, 6, p, &color);
        delete p; break;
      }
      case SolidTextureExampleColorNames::BRICK_TEXTURE:
      {
        ColorRgba c1(0.78, 0.78, 0.72, 1.0), c2(0.58, 0.12, 0.07, 1.0);
        colorTextureFixture->brick(x * 10.0, y * 7.0, z * 5.0, &color,
            &c1, &c2, 0.08); break;
      }
      case SolidTextureExampleColorNames::MATERIAL_MAP_TEXTURE:
        materialMapTexture(x, y, z, materialMap, &color); break;
    }
    delete palette;
    if ( color.getA() == 0.0 ) color.setA(1.0);
    return color;
}

void SolidTextureModel::checkerTextureTexture(double x, double y, double z, ColorRgba* color)
{
    int index = (int)(TextureUtils::floorInline(x * 8.0 + SMALL_TOLERANCE) +
        TextureUtils::floorInline(y * 8.0 + SMALL_TOLERANCE) +
        TextureUtils::floorInline(z * 8.0 + SMALL_TOLERANCE));
    if ( index & 1 ) {
        RGBAColorPalette* p = woodPalette();
        colorTextureFixture->wood(x * 9.0, y * 9.0, z * 9.0, 1.7, 6, p, color);
        delete p;
    }
    else {
        RGBAColorPalette* p = defaultPalette();
        colorTextureFixture->marble(x * 8.0, y * 8.0, z * 8.0, 1.25, 6, p, color);
        delete p;
    }
}

void SolidTextureModel::materialMapTexture(
    double x, double y, double z, ControlledRGBAImageHDRUncompressed* materialMap,
    ColorRgba* color)
{
    Vector3Dd pnt(x, y, z);
    int material = imageTexture.materialMap(&pnt, 0, materialMap, 4, SMALL_TOLERANCE);
    if ( material == 0 ) {
        RGBAColorPalette* p = woodPalette();
        colorTextureFixture->wood(x * 9.0, y * 9.0, z * 9.0, 1.6, 5, p, color);
        delete p;
    }
    else if ( material == 1 ) {
        RGBAColorPalette* p = granitePalette();
        colorTextureFixture->granite(x * 6.0, y * 6.0, z * 6.0, p, color);
        delete p;
    }
    else if ( material == 2 ) {
        RGBAColorPalette* p = leopardPalette();
        colorTextureFixture->leopard(x * 12.0, y * 12.0, z * 12.0, 0.9, 5, p, color);
        delete p;
    }
    else {
        ColorRgba c1(0.12, 0.25, 0.65, 1.0), c2(0.95, 0.92, 0.4, 1.0);
        colorTextureFixture->checker(x * 8.0, y * 8.0, z * 8.0, color, &c1, &c2, SMALL_TOLERANCE);
    }
}

ControlledRGBAImageHDRUncompressed* SolidTextureModel::buildImageMapTexture()
{
    ControlledRGBAImageHDRUncompressed* image = new ControlledRGBAImageHDRUncompressed();
    int side = std::max(1, solidTextureSize);
    image->allocate(side, side);
    image->setMapType(ImageToSolidTextureProjectionMethods::PLANAR_MAP);
    image->setInterpolationType(ImageToSolidTextureInterpolationTypes::BI_LINEAR);
    image->setImageGradient(Vector3Dd(1.0, -1.0, 0.0));
    for ( int y = 0; y < side; y++ ) for ( int x = 0; x < side; x++ ) {
        double u = unitCoordinate(x, side), v = unitCoordinate(y, side);
        RGBAPixelHDR pixel;
        pixel.r = toChannel8(u); pixel.g = toChannel8(v);
        pixel.b = toChannel8(std::fmod((x / 8.0) + (y / 8.0), 2.0) == 0.0 ? 0.85 : 0.2);
        pixel.a = toChannel8(1.0);
        image->setPixel(x, y, pixel);
    }
    return image;
}

ControlledRGBAImageHDRUncompressed* SolidTextureModel::buildMaterialMapTexture()
{
    ControlledRGBAImageHDRUncompressed* image = new ControlledRGBAImageHDRUncompressed();
    int side = std::max(1, solidTextureSize);
    image->allocate(side, side);
    image->setMapType(ImageToSolidTextureProjectionMethods::PLANAR_MAP);
    image->setInterpolationType(ImageToSolidTextureInterpolationTypes::NO_INTERPOLATION);
    image->setImageGradient(Vector3Dd(1.0, -1.0, 0.0));
    image->setUseColorFlag(false);
    IndexedColorImageHDRUncompressed* indexed = new IndexedColorImageHDRUncompressed();
    indexed->allocate(side, side);
    indexed->setColorMapSize(4);
    RGBAPixelHDR* table = new RGBAPixelHDR[4];
    for ( int i = 0; i < 4; i++ ) {
        table[i].r = toChannel8(i / 3.0);
        table[i].g = toChannel8(1.0 - i / 3.0);
        table[i].b = toChannel8((i & 1) == 0 ? 0.2 : 0.85);
        table[i].a = toChannel8(1.0);
    }
    indexed->setColorTable(table);
    int cell = std::max(1, side / 5);
    for ( int y = 0; y < side; y++ )
        for ( int x = 0; x < side; x++ )
            indexed->setPixel(x, y, (unsigned char)(((x / cell) + (y / cell)) & 3));
    image->setIndexedData(indexed);
    return image;
}

RGBAColorPalette* SolidTextureModel::defaultPalette() const
{
    RGBAColorPalette* p = new RGBAColorPalette();
    p->addColor(0.05, 0.12, 0.28, 1.0); p->addColor(0.15, 0.55, 0.75, 1.0);
    p->addColor(0.95, 0.78, 0.26, 1.0); p->addColor(0.88, 0.18, 0.14, 1.0);
    return p;
}

RGBAColorPalette* SolidTextureModel::woodPalette() const
{
    RGBAColorPalette* p = new RGBAColorPalette();
    p->addColor(0.25, 0.11, 0.04, 1.0); p->addColor(0.64, 0.34, 0.13, 1.0);
    p->addColor(0.88, 0.61, 0.28, 1.0); p->addColor(0.33, 0.16, 0.07, 1.0);
    return p;
}

RGBAColorPalette* SolidTextureModel::agatePalette() const
{
    RGBAColorPalette* p = new RGBAColorPalette();
    p->addColor(0.98, 0.92, 0.72, 1.0); p->addColor(0.72, 0.36, 0.18, 1.0);
    p->addColor(0.28, 0.12, 0.08, 1.0); p->addColor(0.95, 0.78, 0.5, 1.0);
    return p;
}

RGBAColorPalette* SolidTextureModel::granitePalette() const
{
    RGBAColorPalette* p = new RGBAColorPalette();
    p->addColor(0.08, 0.08, 0.09, 1.0); p->addColor(0.32, 0.32, 0.34, 1.0);
    p->addColor(0.7, 0.68, 0.64, 1.0); p->addColor(0.16, 0.14, 0.13, 1.0);
    return p;
}

RGBAColorPalette* SolidTextureModel::leopardPalette() const
{
    RGBAColorPalette* p = new RGBAColorPalette();
    p->addColor(0.05, 0.03, 0.015, 1.0); p->addColor(0.86, 0.53, 0.12, 1.0);
    p->addColor(0.96, 0.76, 0.26, 1.0); p->addColor(0.12, 0.06, 0.02, 1.0);
    return p;
}

double SolidTextureModel::unitCoordinate(int index, int count)
{
    return count <= 1 ? 0.0 : index / (double)(count - 1);
}

unsigned short SolidTextureModel::toChannel8(double value)
{
    int channel = (int)std::round(clamp01(value) * 255.0);
    return (unsigned short)(channel & 0xff);
}

unsigned char SolidTextureModel::channel16To8(double value)
{
    int channel16 = (int)std::round(clamp01(value) * 65535.0);
    return (unsigned char)((channel16 >> 8) & 0xff);
}

double SolidTextureModel::clamp01(double value)
{
    if ( value < 0.0 ) return 0.0;
    return std::min(value, 1.0);
}

void SolidTextureModel::putVolumeColor16As8(
    int side, int x, int y, int z, const ColorRgba& color)
{
    size_t base = ((z * side * side) + (y * side) + x) * 3;
    solidTextureVolumeRgb8[base] = channel16To8(color.getR());
    solidTextureVolumeRgb8[base + 1] = channel16To8(color.getG());
    solidTextureVolumeRgb8[base + 2] = channel16To8(color.getB());
}

Intersection* SolidTextureModel::makeIntersectionCallback(const Ray& ray)
{
    Intersection* closest = 0;
    double closestT = 1e308;
    java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL);
        if ( bodies.get(i)->doIntersectionFirstHit(ray, &hit) &&
             hit.hasHitDistance() ) {
            double t = hit.getHitDistance();
            if ( t > VSDK::EPSILON && t < closestT ) {
                closestT = t;
                delete closest;
                closest = new Intersection(t, hit.point, hit.normal);
            }
        }
    }
    return closest;
}

void SolidTextureModel::configureInitialViewAndLightToScene()
{
    java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    if ( bodies.size() == 0 ) return;
    SimpleBodyGroup group;
    for ( long i = 0; i < bodies.size(); i++ ) group.getBodies().add(bodies.get(i));
    double* minMax = group.getMinMax();
    if ( minMax == 0 ) return;
    Vector3Dd min(minMax[0], minMax[1], minMax[2]);
    Vector3Dd max(minMax[3], minMax[4], minMax[5]);
    delete[] minMax;
    Vector3Dd center = min.add(max).multiply(0.5);
    double radius = max.subtract(min).length() * 0.5;
    if ( radius < 0.001 ) radius = 1.0;
    double fovRad = camera.getFov() * M_PI / 180.0;
    double viewDistance = (radius / std::tan(fovRad * 0.5)) * 1.35;
    if ( viewDistance < radius * 1.5 ) viewDistance = radius * 1.5;
    Vector3Dd eyeDirection = Vector3Dd(0, -1, 0.35).normalized();
    camera.setPosition(center.add(eyeDirection.multiply(viewDistance)));
    camera.setUpMaintainingOrthogonality(Vector3Dd(0, 0, 1));
    camera.setFocusedPositionMaintainingOrthogonality(center);
    double nearPlane = std::max(0.01, viewDistance - (radius * 2.2));
    double farPlane = std::max(nearPlane + 1.0, viewDistance + (radius * 4.0));
    camera.setNearPlaneDistance(nearPlane);
    camera.setFarPlaneDistance(farPlane);
    camera.updateVectors();
    Vector3Dd lightDirection = Vector3Dd(1, -1, 1).normalized();
    Vector3Dd lightPos0 = center.add(lightDirection.multiply(radius * 3.0));
    Vector3Dd lightPos1 = center.add(Vector3Dd(-lightDirection.x(), -lightDirection.y(), lightDirection.z()).normalized().multiply(radius * 3.0));
    if ( lights.size() > 0 ) lights.get(0)->setPosition(lightPos0);
    if ( lights.size() > 1 ) lights.get(1)->setPosition(lightPos1);
}
