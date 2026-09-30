#ifndef __SOLID_TEXTURE_MODEL__
#define __SOLID_TEXTURE_MODEL__

#include <vector>

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/InfinitePlaneGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/media/solidTexture/TextureUtils.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageTexture.h"
#include "vsdk/toolkit/media/solidTexture/procedural/ColorTextureFixture.h"
#include "model/OperationMode.h"
#include "model/SolidTextureExampleColorNames.h"

class ControlledRGBAImageHDRUncompressed;
class Image;
class Intersection;
class Light;

class SolidTextureModel {
private:
    static const int MIN_SOLID_TEXTURE_SIZE = 1;
    static const int MAX_SOLID_TEXTURE_SIZE = 256;
    static const double SMALL_TOLERANCE;

    Camera camera;
    java::ArrayList<Light*> lights;
    SimpleScene scene;
    RendererConfiguration qualitySelection;
    RayGizmo rayGizmo;
    InfinitePlaneGizmo infinitePlaneGizmo;
    java::ArrayList<Image*> texture2DStack;
    TextureUtils textureUtils;
    ColorTextureFixture* colorTextureFixture;
    ImageTexture imageTexture;
    int solidTextureSize;
    std::vector<unsigned char> solidTextureVolumeRgb8;
    long solidTextureRevision;
    SolidTextureExampleColorNames selectedSolidTexture;
    bool animationEnabled;
    bool hudVisible;
    OperationMode operationMode;
    java::String tangibleServiceUrl;

    Intersection* makeIntersectionCallback(const Ray& ray);
    void clearTexture2DStack();
    void rebuildTexture2DStack();
    Image* generateTextureSlice(int sliceIndex);
    ColorRgba colorAt(double x, double y, double z,
                      ControlledRGBAImageHDRUncompressed* imageMap,
                      ControlledRGBAImageHDRUncompressed* materialMap);
    void checkerTextureTexture(double x, double y, double z, ColorRgba* color);
    void materialMapTexture(double x, double y, double z,
                            ControlledRGBAImageHDRUncompressed* materialMap,
                            ColorRgba* color);
    ControlledRGBAImageHDRUncompressed* buildImageMapTexture();
    ControlledRGBAImageHDRUncompressed* buildMaterialMapTexture();
    RGBAColorPalette* defaultPalette() const;
    RGBAColorPalette* woodPalette() const;
    RGBAColorPalette* agatePalette() const;
    RGBAColorPalette* granitePalette() const;
    RGBAColorPalette* leopardPalette() const;

    static double unitCoordinate(int index, int count);
    static unsigned short toChannel8(double value);
    static unsigned char channel16To8(double value);
    static double clamp01(double value);
    void putVolumeColor16As8(int side, int x, int y, int z,
                             const ColorRgba& color);

public:
    SolidTextureModel();
    ~SolidTextureModel();

    Camera* getCamera();
    java::ArrayList<Light*>& getLights();
    SimpleScene* getScene();
    RendererConfiguration* getQualitySelection();
    RayGizmo* getRayGizmo();
    InfinitePlaneGizmo* getInfinitePlaneGizmo();
    java::ArrayList<Image*>& getTexture2DStack();
    const std::vector<unsigned char>& getSolidTextureVolumeRgb8() const;
    long getSolidTextureRevision() const;
    int getSolidTextureSize() const;
    SolidTextureExampleColorNames getSelectedSolidTexture() const;
    bool isAnimationEnabled() const;
    void setAnimationEnabled(bool animationEnabled);
    void toggleAnimationEnabled();
    bool isHudVisible() const;
    void toggleHudVisible();
    void advanceObjectRotationRadians(double deltaRadians);
    void selectNextSolidTexture();
    void selectPreviousSolidTexture();
    void increaseSolidTextureSize();
    void decreaseSolidTextureSize();
    OperationMode getOperationMode() const;
    void rotateOperationMode();
    const java::String& getTangibleServiceUrl() const;
    void setTangibleServiceUrl(const java::String& tangibleServiceUrl);
    void configureInitialViewAndLightToScene();
};

#endif
