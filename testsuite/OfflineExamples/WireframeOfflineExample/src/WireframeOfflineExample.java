// Java classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;     // Model elements
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Calligraphic2DBuffer;         // I/O artifacts
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.geometry.EnvironmentPersistence; // Persistence elements
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.WireframeRenderer;           // Processing elements

/**
This example program is the most fundamental computer graphics example in
VitralSDK that does not depend on any external libraries to generate an image
from a 3D scene. Note that it is based on:
  - A wireframe model imported from an external .obj file (requires a
    running platform with support to file systems, networked or "inline in
    code" reader).
  - Simple camera model and calligraphic renderer (100% java / Vitral SDK
    implementation)
  - Raster output using Vitral SDK image and Bresenham's line algorithm
  - This particular program exports the resulting image in a file (requires
    a platform supporting I/O to files).
*/
public class WireframeOfflineExample {
    private Camera camera;
    private SimpleScene scene;

    /**
    Note the starting simplicity of this constructor. Compare it against
    the Swing+JOGL version of this same example (VSDKExamples/WireframeExample)
    and recall that the only difference is that the offline example must
    update the virtual viewport space to the camera (in the interactive
    version this is done on a Swing/JOGL controlled callback function).
    */
    public WireframeOfflineExample() {
        createModel();
        camera.updateViewportResize(640, 480);
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
        Matrix4x4 R = new Matrix4x4();

        camera.setPosition(new Vector3D(7, -4, 4));
        R.eulerAnglesRotation(Math.toRadians(140), Math.toRadians(-30), 0);
        camera.setNearPlaneDistance(0.001);
        camera.setFarPlaneDistance(100);
        camera.setRotation(R);

        //-----------------------------------------------------------------
        String sceneFile = "../../../etc/geometry/cow.obj";
        scene = new SimpleScene();

        try {
            EnvironmentPersistence.importEnvironment(new File(sceneFile), scene);
        }
        catch ( Exception ex ) {
            System.err.println("Failed to read file");
            ex.printStackTrace();
        }

        //-----------------------------------------------------------------
        SimpleBody b;
        Box box;

        b = new SimpleBody();
        box = new Box(1, 1, 1);
        b.setGeometry(box);
        b.setPosition(new Vector3D(1, 2, 3));
        scene.addBody(b);
    }

    public void rasterOutput(Calligraphic2DBuffer lineSet) {
        RGBImage outputImageRasterViewport;

        //- (1/2) line rasterization in to output image -------------------
        double xt = camera.getViewportXSize();
        double yt = camera.getViewportYSize();

        outputImageRasterViewport = new RGBImage();
        outputImageRasterViewport.init((int)xt, (int)yt);

        lineSet.exportRgbImage(outputImageRasterViewport);

        lineSet.init(); // leaves buffer ready for next frame

        //- (2/2) Image result transfer to output file --------------------
        ImagePersistence.exportJPG(
            new File("output.jpg"), outputImageRasterViewport);
    }

    public static void main (String[] args) {
        WireframeOfflineExample instance = new WireframeOfflineExample();
        Calligraphic2DBuffer lineSet;

        lineSet = new Calligraphic2DBuffer();
        WireframeRenderer.execute(
            lineSet, instance.scene.getSimpleBodies(), instance.camera);
        instance.rasterOutput(lineSet);
    }

}
