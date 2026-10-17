package vsdk.toolkit.io.geometry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleScene;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderMitSceneTest
{
    @Test
    void givenSameReader_whenImportingMultipleScenes_thenScenesKeepIndependentCameras()
        throws Exception
    {
        // Arrange
        ReaderMitScene reader = new ReaderMitScene();

        SimpleScene firstScene = importScene(reader, 123, 45);
        Camera firstCamera = firstScene.getActiveCamera();

        SimpleScene secondScene = importScene(reader, 640, 480);
        Camera secondCamera = secondScene.getActiveCamera();

        // Action

        // Assert
        assertThat(firstCamera).isNotSameAs(secondCamera);
        assertThat(firstCamera.getViewportXSize()).isEqualTo(123.0);
        assertThat(firstCamera.getViewportYSize()).isEqualTo(45.0);
        assertThat(secondCamera.getViewportXSize()).isEqualTo(640.0);
        assertThat(secondCamera.getViewportYSize()).isEqualTo(480.0);
    }

    @Test
    void givenMitTriangleScene_whenImported_thenMeshesAreDoubleSidedForRasterRendering()
        throws Exception
    {
        String sceneDescription =
            "triangles\n" +
            "v 0 0 0\n" +
            "v 1 0 0\n" +
            "v 0 1 0\n" +
            "f 0 1 2\n" +
            "end\n";
        SimpleScene scene = new SimpleScene();

        try (InputStream is = new ByteArrayInputStream(
                 sceneDescription.getBytes(StandardCharsets.UTF_8))) {
            new ReaderMitScene().importEnvironment(is, scene);
        }

        TriangleMesh mesh = (TriangleMesh)scene.getSimpleBodies().get(0).getGeometry();
        assertThat(mesh.isDoubleSided()).isTrue();
    }

    private SimpleScene importScene(ReaderMitScene reader, int width, int height)
        throws Exception
    {
        SimpleScene scene = new SimpleScene();
        String sceneDescription =
            "viewport " + width + " " + height + "\n" +
            "eye 0 0 10\n" +
            "lookat 0 0 0\n" +
            "up 0 1 0\n" +
            "fov 30\n" +
            "background 0 0 0\n" +
            "light 1 1 1 ambient\n" +
            "sphere 0 0 0 1\n";

        try (InputStream is = new ByteArrayInputStream(
                 sceneDescription.getBytes(StandardCharsets.UTF_8))) {
            reader.importEnvironment(is, scene);
        }
        return scene;
    }
}
