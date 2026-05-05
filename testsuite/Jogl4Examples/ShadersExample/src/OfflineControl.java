import java.io.File;

import model.ShaderOperationMode;
import model.ShadersModel;
import options.CommandLineOptions;
import render.OpenGlOfflineSphereRenderer;
import render.SoftwareRaycaster;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;

public final class OfflineControl
{
    private OfflineControl()
    {
    }

    public static void run(CommandLineOptions options)
    {
        ShadersModel model = ShadersModel.createDefault();
        model.updateSoftwareViewportAndCamera(options.getWidth(), options.getHeight());

        if ( options.getRotationDegrees() != null ) {
            model.setSphereRotationAngleRadians(
                Math.toRadians(options.getRotationDegrees()));
        }

        Matrix4x4 modelRotation = new Matrix4x4().axisRotation(
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
}
