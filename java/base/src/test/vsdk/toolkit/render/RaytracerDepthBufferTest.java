package vsdk.toolkit.render;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.render.raytracing.DepthBufferEncoder;
import vsdk.toolkit.render.raytracing.DepthBufferMode;
import vsdk.toolkit.render.raytracing.ParallelRaytracer;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Checks the depth buffers exported by the raytracers against the OpenGL
projection of the same camera (`Camera.calculateProjectionMatrix`): an
OpenGL depth taken from the raytracer, unprojected with the inverse of that
matrix at the pixel center, must land on the surface that the ray hit.
*/
class RaytracerDepthBufferTest
{
    private static final int WIDTH = 81;
    private static final int HEIGHT = 57;

    private Camera camera;

    private SimpleSceneSnapshot createSnapshot(int projectionMode)
    {
        SimpleScene scene = new SimpleScene();
        SimpleBackground background = new SimpleBackground();
        SimpleBody body = new SimpleBody();

        camera = new Camera();
        camera.setPosition(new Vector3Dd(-5, -5, 5));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(
            Math.toRadians(45), Math.toRadians(-35), 0));
        camera.setNearPlaneDistance(0.5);
        camera.setFarPlaneDistance(30);
        camera.setProjectionMode(projectionMode);
        camera.setOrthogonalZoom(0.6);
        background.setColor(0.2, 0.3, 0.4);

        body.setGeometry(new Sphere(1.0));
        body.setPosition(new Vector3Dd(0, 0, 0));
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setMaterial(new SimpleMaterial().withDiffuse(new ColorRgb(0.8, 0.5, 0.3)));

        scene.addCamera(camera);
        scene.addBackground(background);
        scene.addBody(body);
        scene.addLight(new PointLight(new Vector3Dd(3, -3, 4), new ColorRgb(1, 1, 1)));
        return scene.exportToSimpleSceneSnapshot(WIDTH, HEIGHT);
    }

    private static ZBuffer raytrace(SimpleSceneSnapshot snapshot, DepthBufferMode mode)
    {
        RGBImageUncompressed image = new RGBImageUncompressed();
        ZBuffer depth = new ZBuffer(WIDTH, HEIGHT);

        image.init(WIDTH, HEIGHT);
        new SimpleRaytracer().execute(image, new RendererConfiguration(), snapshot,
            null, depth, mode, 0, 0, WIDTH, HEIGHT);
        return depth;
    }

    /**
    @return world point of the pixel (x, y), row 0 at the top, at the given
    OpenGL window depth in [0, 1]
    */
    private Vector3Dd unproject(Matrix4x4d inverseProjection, int x, int y, double windowDepth)
    {
        double ndcX = 2.0 * (x + 0.5) / WIDTH - 1.0;
        double ndcY = 1.0 - 2.0 * (y + 0.5) / HEIGHT;
        double ndcZ = 2.0 * windowDepth - 1.0;
        Vector4Dd p = inverseProjection.multiply(new Vector4Dd(ndcX, ndcY, ndcZ, 1.0))
            .dividedByW();
        return new Vector3Dd(p.x(), p.y(), p.z());
    }

    private void checkOpenGlDepthMatchesProjection(int projectionMode)
    {
        // Arrange
        SimpleSceneSnapshot snapshot = createSnapshot(projectionMode);
        Matrix4x4d inverseProjection = camera.calculateProjectionMatrix().invert();
        CameraSnapshot cameraSnapshot = snapshot.getCameraSnapshot();

        // Action
        ZBuffer glDepth = raytrace(snapshot, DepthBufferMode.OPENGL_DEPTH);
        ZBuffer distance = raytrace(snapshot, DepthBufferMode.RAY_DISTANCE);

        // Assert
        int hits = 0;
        int misses = 0;
        for ( int y = 0; y < HEIGHT; y++ ) {
            for ( int x = 0; x < WIDTH; x++ ) {
                float d = glDepth.getZ(x, y);
                float t = distance.getZ(x, y);
                if ( Float.isInfinite(t) ) {
                    misses++;
                    assertThat(d).isEqualTo(1.0f);
                    continue;
                }
                hits++;
                assertThat(d).isBetween(0.0f, 1.0f);
                Vector3Dd p = unproject(inverseProjection, x, y, d);
                // The point is on the sphere...
                assertThat(p.length()).isCloseTo(1.0, within(2e-3));
                // ... at the ray distance (from the eye, or from the image
                // plane in orthogonal projection)
                double expectedDistance;
                if ( projectionMode == Camera.PROJECTION_MODE_ORTHOGONAL ) {
                    expectedDistance = p.subtract(cameraSnapshot.getEyePosition())
                        .dotProduct(cameraSnapshot.getFront());
                }
                else {
                    expectedDistance = p.subtract(cameraSnapshot.getEyePosition()).length();
                }
                assertThat((double)t).isCloseTo(expectedDistance, within(2e-3));
            }
        }
        assertThat(hits).isGreaterThan(100);
        assertThat(misses).isGreaterThan(200);
    }

    @Test
    void given_perspectiveCamera_when_exportingOpenGlDepth_then_unprojectsToHitSurface()
    {
        checkOpenGlDepthMatchesProjection(Camera.PROJECTION_MODE_PERSPECTIVE);
    }

    @Test
    void given_orthogonalCamera_when_exportingOpenGlDepth_then_unprojectsToHitSurface()
    {
        checkOpenGlDepthMatchesProjection(Camera.PROJECTION_MODE_ORTHOGONAL);
    }

    @Test
    void given_depthModeNone_when_raytracing_then_depthBufferIsUntouched()
    {
        // Arrange
        SimpleSceneSnapshot snapshot = createSnapshot(Camera.PROJECTION_MODE_PERSPECTIVE);

        // Action
        ZBuffer depth = raytrace(snapshot, DepthBufferMode.NONE);

        // Assert
        for ( float value : depth.getZBuffer() ) {
            assertThat(value).isZero();
        }
    }

    @Test
    void given_depthMode_when_raytracingInParallel_then_depthEqualsSerial()
    {
        // Arrange
        SimpleSceneSnapshot snapshot = createSnapshot(Camera.PROJECTION_MODE_PERSPECTIVE);
        ParallelRaytracer parallel = new ParallelRaytracer(3);
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(WIDTH, HEIGHT);

        // Action / Assert
        assertThat(parallel.getDepthBufferMode()).isEqualTo(DepthBufferMode.NONE);
        parallel.execute(image, new RendererConfiguration(), snapshot, false);
        assertThat(parallel.getDepthBuffer()).isNull();

        for ( DepthBufferMode mode : new DepthBufferMode[] {
                  DepthBufferMode.RAY_DISTANCE, DepthBufferMode.OPENGL_DEPTH } ) {
            ZBuffer serial = raytrace(snapshot, mode);
            parallel.setDepthBufferMode(mode);
            parallel.execute(image, new RendererConfiguration(), snapshot, false);
            assertThat(parallel.getDepthBuffer().getXSize()).isEqualTo(WIDTH);
            assertThat(parallel.getDepthBuffer().getYSize()).isEqualTo(HEIGHT);
            assertThat(parallel.getDepthBuffer().getZBuffer())
                .containsExactly(serial.getZBuffer());
        }
        parallel.setDepthBufferMode(DepthBufferMode.NONE);
        assertThat(parallel.getDepthBuffer()).isNull();
        parallel.dispose();
    }

    @Test
    void given_customDepthRange_when_raytracingInParallel_then_depthIsRemapped()
    {
        // Arrange
        SimpleSceneSnapshot snapshot = createSnapshot(Camera.PROJECTION_MODE_PERSPECTIVE);
        ParallelRaytracer parallel = new ParallelRaytracer(2);
        RGBImageUncompressed image = new RGBImageUncompressed();
        ZBuffer standard = raytrace(snapshot, DepthBufferMode.OPENGL_DEPTH);
        image.init(WIDTH, HEIGHT);
        parallel.setDepthBufferMode(DepthBufferMode.OPENGL_DEPTH);
        parallel.setOpenGlDepthRange(0.25, 0.75);

        // Action
        parallel.execute(image, new RendererConfiguration(), snapshot, false);

        // Assert
        float[] remapped = parallel.getDepthBuffer().getZBuffer();
        float[] expected = standard.getZBuffer();
        for ( int i = 0; i < expected.length; i++ ) {
            assertThat((double)remapped[i])
                .isCloseTo(0.25 + 0.5 * expected[i], within(1e-6));
        }
        parallel.dispose();
    }

    @Test
    void given_encoder_when_convertingPlaneDistances_then_matchesOpenGlLimits()
    {
        // Arrange
        createSnapshot(Camera.PROJECTION_MODE_PERSPECTIVE);
        CameraSnapshot perspective = camera.exportToCameraSnapshot(WIDTH, HEIGHT);
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        CameraSnapshot orthogonal = camera.exportToCameraSnapshot(WIDTH, HEIGHT);

        for ( CameraSnapshot snapshot : new CameraSnapshot[] { perspective, orthogonal } ) {
            // Action
            DepthBufferEncoder encoder =
                new DepthBufferEncoder(DepthBufferMode.OPENGL_DEPTH, snapshot);
            DepthBufferEncoder identity =
                new DepthBufferEncoder(DepthBufferMode.RAY_DISTANCE, snapshot);

            // Assert
            assertThat(encoder.eyeDepthToWindowDepth(0.5)).isCloseTo(0.0, within(1e-12));
            assertThat(encoder.eyeDepthToWindowDepth(30)).isCloseTo(1.0, within(1e-12));
            assertThat(encoder.eyeDepthToWindowDepth(0.1)).isEqualTo(0.0);
            assertThat(encoder.eyeDepthToWindowDepth(100)).isEqualTo(1.0);
            assertThat(encoder.eyeDepthToWindowDepth(5))
                .isLessThan(encoder.eyeDepthToWindowDepth(6));
            assertThat(encoder.encode(snapshot.getEyePosition(), snapshot.getFront(),
                Double.POSITIVE_INFINITY)).isEqualTo(1.0f);
            assertThat(identity.encode(snapshot.getEyePosition(), snapshot.getFront(),
                7.25)).isEqualTo(7.25f);
        }
    }
}
