package vsdk.toolkit.render;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.raytracing.ParallelRaytracer;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelRaytracerTest
{
    private static SimpleBody createBody(Geometry geometry, Vector3Dd position)
    {
        SimpleBody body = new SimpleBody();
        SimpleMaterial material = new SimpleMaterial();

        material = material.withDiffuse(new ColorRgb(0.8, 0.5, 0.3));
        body.setGeometry(geometry);
        body.setPosition(position);
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setMaterial(material);
        return body;
    }

    private static SimpleSceneSnapshot createSnapshot(int width, int height)
    {
        SimpleScene scene = new SimpleScene();
        Camera camera = new Camera();
        SimpleBackground background = new SimpleBackground();

        camera.setPosition(new Vector3Dd(-5, -5, 5));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(
            Math.toRadians(45), Math.toRadians(-35), 0));
        background.setColor(0.2, 0.3, 0.4);
        scene.addCamera(camera);
        scene.addBackground(background);
        scene.addBody(createBody(new Sphere(1.0), new Vector3Dd(0, 0, 0)));
        scene.addBody(createBody(new Box(1, 2, 1), new Vector3Dd(1.5, 0, 0)));
        scene.addLight(new PointLight(new Vector3Dd(3, -3, 4), new ColorRgb(1, 1, 1)));
        return scene.exportToSimpleSceneSnapshot(width, height);
    }

    @Test
    void given_scene_when_raytracingInParallel_then_imageEqualsSerialRaytracing()
    {
        // Arrange
        int width = 97;
        int height = 61;
        SimpleSceneSnapshot snapshot = createSnapshot(width, height);
        RendererConfiguration quality = new RendererConfiguration();
        RGBImageUncompressed serialImage = new RGBImageUncompressed();
        RGBImageUncompressed parallelImage = new RGBImageUncompressed();
        ParallelRaytracer parallelRaytracer = new ParallelRaytracer(4);
        serialImage.init(width, height);
        parallelImage.init(width, height);

        // Action
        new SimpleRaytracer().execute(serialImage, quality, snapshot, null);
        parallelRaytracer.execute(parallelImage, quality, snapshot, false);
        // Threads are reused between images
        parallelRaytracer.execute(parallelImage, quality, snapshot, false);
        parallelRaytracer.dispose();

        // Assert
        int differentPixels = 0;
        int objectPixels = 0;
        RGBPixel backgroundPixel = serialImage.getPixelRgb(0, 0);
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                RGBPixel expected = serialImage.getPixelRgb(x, y);
                RGBPixel actual = parallelImage.getPixelRgb(x, y);
                if ( expected.r != actual.r || expected.g != actual.g ||
                     expected.b != actual.b ) {
                    differentPixels++;
                }
                if ( expected.r != backgroundPixel.r || expected.g != backgroundPixel.g ||
                     expected.b != backgroundPixel.b ) {
                    objectPixels++;
                }
            }
        }
        assertThat(objectPixels).isGreaterThan(100);
        assertThat(differentPixels).isZero();
    }

    @Test
    void given_defaultConstructor_when_created_then_usesOneThreadPerProcessor()
    {
        // Arrange / Action
        ParallelRaytracer raytracer = new ParallelRaytracer();

        // Assert
        assertThat(raytracer.getNumberOfThreads())
            .isEqualTo(Runtime.getRuntime().availableProcessors());
    }
}
