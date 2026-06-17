#include <cmath>
#include <cstdio>

#include "java/io/File.h"
#include "OfflineControl.h"
#include "model/ShadersModel.h"
#include "render/OpenGlOfflineSphereRenderer.h"
#include "render/SoftwareRaycaster.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "options/CommandLineOptions.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/shaders/CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
int OfflineControl::run(const CommandLineOptions& options)
{
    ShadersModel model = ShadersModel::createDefault();
    model.showHud = false;
    model.updateSoftwareViewportAndCamera(options.width, options.height);

    if (options.hasWithTexture) model.quality.setTexture(options.withTexture);
    if (options.hasWithBumpMap) model.quality.setBumpMap(options.withBumpMap);
    if (options.hasShadingType) model.quality.setShadingType(options.shadingType);
    if (options.hasMeridians) model.sphereMeridians = options.meridians;
    if (options.hasParallels) model.sphereParallels = options.parallels;
    if (options.hasTextureFilter) {
        OpenGL4ImageRenderer::setTextureFilterMode(
            options.textureFilter == CommandLineOptions::TextureFilterOption::NEAREST
                ? OpenGL4ImageRenderer::TextureFilterMode::NEAREST
                : OpenGL4ImageRenderer::TextureFilterMode::LINEAR);
    }
    if (options.hasCpuTextureOffsetU || options.hasCpuTextureOffsetV) {
        const double u = options.hasCpuTextureOffsetU ? options.cpuTextureOffsetUTexels : -0.5;
        const double v = options.hasCpuTextureOffsetV ? options.cpuTextureOffsetVTexels : -0.5;
        CpuTextureSamplingConfig::setTextureOffsetTexels(u, v);
    }
    if (options.hasRotation) model.setSphereRotationAngleRadians(options.rotationDegrees * M_PI / 180.0);
    if (options.hasLightRotation) {
        Matrix4x4d rot = Matrix4x4d().axisRotation(options.lightRotationDegrees * M_PI / 180.0, 0.0, -1.0, 0.0);
        model.light->setPosition(rot.multiply(Vector3Dd(1.0, -3.0, 1.0)));
    }

    Matrix4x4d modelRotation = Matrix4x4d().axisRotation(model.sphereRotationAngleRadians, 0.0, 0.0, 1.0);
    RGBImageUncompressed* output = 0;

    if (options.method == ShaderOperationMode::SOFTWARE) {
        SoftwareRaycaster raycaster;
        raycaster.render(&model, model.camera, modelRotation);
        output = model.softwareFrameImage ? model.softwareFrameImage->clone() : 0;
    }
    else {
        OpenGlOfflineSphereRenderer renderer;
        output = renderer.render(&model, modelRotation, options.width, options.height);
    }

    if (!output) return 1;
    java::File outputFile(options.offlineOutputPath.c_str());
    bool ok = ImagePersistence::exportPNG(outputFile, output);
    delete output;
    if (!ok) return 1;

    std::printf("Offline render exported: %s\n", options.offlineOutputPath.c_str());
    return 0;
}
