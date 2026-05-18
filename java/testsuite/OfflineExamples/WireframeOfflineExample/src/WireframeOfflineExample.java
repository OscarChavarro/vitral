// Java classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;     // Model elements
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Calligraphic2DBuffer;         // I/O artifacts
import vsdk.toolkit.media.RGBImageUncompressed;
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
        Matrix4x4d R = new Matrix4x4d();

        camera.setPosition(new Vector3Dd(7, -4, 4));
        R = R.eulerAnglesRotation(Math.toRadians(140), Math.toRadians(-30), 0);
        camera.setNearPlaneDistance(0.001);
        camera.setFarPlaneDistance(100);
        camera.setRotation(R);

        //-----------------------------------------------------------------
        String sceneFile = "../../../../etc/geometry/cow.obj";
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
        b.setPosition(new Vector3Dd(1, 2, 3));
        scene.addBody(b);
    }


    public void rasterOutput(Calligraphic2DBuffer lineSet) {
        RGBImageUncompressed outputImageRasterViewport;

        //- (1/2) line rasterization in to output image -------------------
        double xt = camera.getViewportXSize();
        double yt = camera.getViewportYSize();

        outputImageRasterViewport = new RGBImageUncompressed();
        outputImageRasterViewport.init((int)xt, (int)yt);

        lineSet.exportRgbImage(outputImageRasterViewport);

        lineSet.init(); // leaves buffer ready for next frame

        //- (2/2) Image result transfer to output file --------------------
        ImagePersistence.exportPNG(
            new File("output.png"), outputImageRasterViewport);
    }

    private static void reportLineSetStats(Calligraphic2DBuffer lineSet) {
        int inside = 0;
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for ( int i = 0; i < lineSet.getNumLines(); i++ ) {
            Vector3Dd[] seg = lineSet.get2DLine(i);
            Vector3Dd p0 = seg[0];
            Vector3Dd p1 = seg[1];
            minx = Math.min(minx, Math.min(p0.x(), p1.x()));
            miny = Math.min(miny, Math.min(p0.y(), p1.y()));
            maxx = Math.max(maxx, Math.max(p0.x(), p1.x()));
            maxy = Math.max(maxy, Math.max(p0.y(), p1.y()));
            if ( p0.x() >= -1 && p0.x() <= 1 && p0.y() >= -1 && p0.y() <= 1 &&
                 p1.x() >= -1 && p1.x() <= 1 && p1.y() >= -1 && p1.y() <= 1 ) {
                inside++;
            }
        }
        System.out.println("[WireframeOfflineExample] minx=" + minx + " maxx=" + maxx +
            " miny=" + miny + " maxy=" + maxy + " insideSegments=" + inside);
    }

    public static void main (String[] args) {
        WireframeOfflineExample instance = new WireframeOfflineExample();
        Calligraphic2DBuffer lineSet;

        lineSet = new Calligraphic2DBuffer();
        WireframeRenderer.execute(
            lineSet, instance.scene.getSimpleBodies(), instance.camera);
        reportLineSetStats(lineSet);
        instance.rasterOutput(lineSet);
    }

}
