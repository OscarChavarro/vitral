#ifndef SHADERSEXAMPLE_MODEL_SHADERSMODEL_H
#define SHADERSEXAMPLE_MODEL_SHADERSMODEL_H

#include <string>
#include "ShaderOperationMode.h"

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"

class ShadersModel {
public:
    static ShadersModel createDefault();

    vsdk::toolkit::environment::camera::Camera* camera;
    vsdk::toolkit::gui::CameraControllerAquynza* cameraController;
    RendererConfiguration quality;
    vsdk::toolkit::gui::RendererConfigurationController* qualityController;
    Sphere* sphere;
    Light* light;
    SimpleMaterial material;
    RGBImageUncompressed* textureMap;
    RGBImageUncompressed* bumpMapHeightRgb;
    NormalMap* bumpNormalMap;
    RGBImageUncompressed* softwareFrameImage;
    ShaderOperationMode renderingMode;
    bool showHud;
    bool animationEnabled;
    bool lightAnimationEnabled;
    double sphereRotationAngleRadians;
    int sphereMeridians;
    int sphereParallels;

    ShadersModel();
    ~ShadersModel();

    void rotateRenderingMode();
    void toggleShowHud();
    void toggleAnimationEnabled();
    void toggleLightAnimationEnabled();
    void updateSoftwareViewportAndCamera(int width, int height);
    void changeSphereMeridians(int delta);
    void changeSphereParallels(int delta);
    void setSphereRotationAngleRadians(double angle);
    void advanceSphereRotationRadians(double delta);
    std::string getCookTorranceMaterialLabel() const;
    void cycleCookTorranceMaterial();
};

#endif
