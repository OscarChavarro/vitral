import java.io.File;

import model.ShaderOperationMode;
import model.ShadersModel;
import options.CommandLineOptions;
import render.OpenGlOfflineSphereRenderer;
import render.SoftwareRaycaster;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.shaders.CpuTextureSamplingConfig;

public final class OfflineControl
{
    private OfflineControl()
    {
    }

    public static void run(CommandLineOptions options)
    {
        ShadersModel model = ShadersModel.createDefault();
        model.setShowHud(false);
        model.updateSoftwareViewportAndCamera(options.getWidth(), options.getHeight());
        applyRenderFeatureOverrides(model, options);

        if ( options.getRotationDegrees() != null ) {
            model.setSphereRotationAngleRadians(
                Math.toRadians(options.getRotationDegrees()));
        }
        if ( options.getLightRotationDegrees() != null ) {
            applyOfflineLightRotation(
                model,
                Math.toRadians(options.getLightRotationDegrees().doubleValue()));
        }

        Matrix4x4d modelRotation = new Matrix4x4d().axisRotation(
            model.getSphereRotationAngleRadians(),
            0.0,
            0.0,
            1.0);

        RGBImageUncompressed output;
        if ( options.getMethod() == ShaderOperationMode.SOFTWARE ) {
            SoftwareRaycaster raycaster = new SoftwareRaycaster();
            raycaster.render(model, model.getCamera(), modelRotation);
            output = model.getSoftwareFrameImage();
        }
        else {
            OpenGlOfflineSphereRenderer renderer = new OpenGlOfflineSphereRenderer();
            output = renderer.render(
                model,
                modelRotation,
                options.getWidth(),
                options.getHeight());
        }

        File outputFile = new File(options.getOfflineOutputPath());
        File parent = outputFile.getParentFile();
        if ( parent != null && !parent.exists() ) {
            parent.mkdirs();
        }
        ImagePersistence.exportPNG(outputFile, output);
        System.out.println("Offline render exported: " + outputFile.getAbsolutePath());
    }

    private static void applyOfflineLightRotation(
        ShadersModel model,
        double lightRotationRadians)
    {
        Matrix4x4d rotation = new Matrix4x4d().axisRotation(
            lightRotationRadians,
            0.0,
            -1.0,
            0.0);
        Vector3Dd baseLightPosition = new Vector3Dd(1.0, -3.0, 1.0);
        model.getLight().setPosition(rotation.multiply(baseLightPosition));
    }

    private static void applyRenderFeatureOverrides(
        ShadersModel model,
        CommandLineOptions options)
    {
        if ( options.getWithTexture() != null ) {
            model.getQuality().setTexture(options.getWithTexture().booleanValue());
        }
        if ( options.getWithBumpMap() != null ) {
            model.getQuality().setBumpMap(options.getWithBumpMap().booleanValue());
        }
        if ( options.getShadingType() != null ) {
            model.getQuality().setShadingType(options.getShadingType().intValue());
        }
        if ( options.getMeridians() != null ) {
            model.setSphereMeridians(options.getMeridians().intValue());
        }
        if ( options.getParallels() != null ) {
            model.setSphereParallels(options.getParallels().intValue());
        }
        if ( options.getTextureFilter() != null ) {
            if ( options.getTextureFilter() == CommandLineOptions.TextureFilterOption.NEAREST ) {
                Jogl4ImageRenderer.setTextureFilterMode(
                    Jogl4ImageRenderer.TextureFilterMode.NEAREST);
            }
            else {
                Jogl4ImageRenderer.setTextureFilterMode(
                    Jogl4ImageRenderer.TextureFilterMode.LINEAR);
            }
        }

        if ( options.getCpuTextureOffsetUTexels() != null ||
             options.getCpuTextureOffsetVTexels() != null ) {
            double cpuTextureOffsetU = -0.5;
            double cpuTextureOffsetV = -0.5;
            if ( options.getCpuTextureOffsetUTexels() != null ) {
                cpuTextureOffsetU = options.getCpuTextureOffsetUTexels().doubleValue();
            }
            if ( options.getCpuTextureOffsetVTexels() != null ) {
                cpuTextureOffsetV = options.getCpuTextureOffsetVTexels().doubleValue();
            }
            CpuTextureSamplingConfig.setTextureOffsetTexels(
                cpuTextureOffsetU,
                cpuTextureOffsetV);
        }

        model.setShowHud(options.getShowHud());
    }
}
