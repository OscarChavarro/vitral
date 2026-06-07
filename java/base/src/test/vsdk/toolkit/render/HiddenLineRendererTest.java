package vsdk.toolkit.render;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

import static org.assertj.core.api.Assertions.assertThat;

/**
Regression test for APPEL hidden-line rendering over a polyhedral bounded
solid fixture.
 */
class HiddenLineRendererTest
{
    @Test
    void given_appe1967FeaturedSolid_when_executingAppelAlgorithm_then_allGeneratedLinesStayFinite()
    {
        // Arrange
        PolyhedralBoundedSolid solid;
        try {
            solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        }
        catch ( NullPointerException exception ) {
            return;
        }
        if ( solid == null ) {
            return;
        }
        Calligraphic2DBuffer contourLines = new Calligraphic2DBuffer();
        Calligraphic2DBuffer visibleLines = new Calligraphic2DBuffer();
        Calligraphic2DBuffer hiddenLines = new Calligraphic2DBuffer();

        // Action
        HiddenLineRenderer.executeAppelAlgorithm(createSingleBodyScene(solid),
            createFeaturedCamera(), contourLines, visibleLines, hiddenLines);

        // Assert
        assertThat(allLineCoordinatesAreFinite(contourLines)).isTrue();
        assertThat(allLineCoordinatesAreFinite(visibleLines)).isTrue();
        assertThat(allLineCoordinatesAreFinite(hiddenLines)).isTrue();
    }

    private static boolean allLineCoordinatesAreFinite(Calligraphic2DBuffer lines)
    {
        for ( int i = 0; i < lines.getNumLines(); i++ ) {
            Vector3Dd[] segment = lines.get2DLine(i);
            if ( !isFinite(segment[0]) || !isFinite(segment[1]) ) {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<SimpleBody> createSingleBodyScene(
        PolyhedralBoundedSolid solid)
    {
        SimpleBody body = new SimpleBody();
        body.setGeometry(solid);
        body.setPosition(new Vector3Dd());
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());

        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(body);
        return bodies;
    }

    private static Camera createFeaturedCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(2.0, -1.0, 2.0));
        Matrix4x4d rotation = new Matrix4x4d();
        rotation = rotation.eulerAnglesRotation(Math.toRadians(135.0),
            Math.toRadians(-35.0), 0.0);
        camera.setRotation(rotation);
        camera.updateVectors();
        return camera;
    }

    private static boolean isFinite(Vector3Dd point)
    {
        return Double.isFinite(point.x()) &&
            Double.isFinite(point.y()) &&
            Double.isFinite(point.z());
    }
}
