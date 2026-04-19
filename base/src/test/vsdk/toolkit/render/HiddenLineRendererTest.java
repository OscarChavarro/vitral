package vsdk.toolkit.render;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.processing.ComputationalGeometry;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;

import static org.assertj.core.api.Assertions.assertThat;

class HiddenLineRendererTest
{
    @Test
    void given_appe1967FeaturedSolid_when_executingAppelAlgorithm_then_allGeneratedEndpointsStayOnSolidEdges()
    {
        PolyhedralBoundedSolid solid =
            SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        double tolerance = PolyhedralBoundedSolidNumericPolicy.forSolid(solid)
            .bigEpsilon();
        ArrayList<Vector3D> contourLines = new ArrayList<Vector3D>();
        ArrayList<Vector3D> visibleLines = new ArrayList<Vector3D>();
        ArrayList<Vector3D> hiddenLines = new ArrayList<Vector3D>();

        HiddenLineRenderer.executeAppelAlgorithm(createSingleBodyScene(solid),
            createFeaturedCamera(), contourLines, visibleLines, hiddenLines);

        ArrayList<Vector3D> allPoints = new ArrayList<Vector3D>();
        allPoints.addAll(contourLines);
        allPoints.addAll(visibleLines);
        allPoints.addAll(hiddenLines);

        assertThat(allPoints).isNotEmpty();
        for ( int i = 0; i < allPoints.size(); i++ ) {
            assertThat(liesOnAnyEdge(solid, allPoints.get(i), tolerance))
                .as("endpoint %s should remain on an original edge", i)
                .isTrue();
        }
    }

    private static ArrayList<SimpleBody> createSingleBodyScene(
        PolyhedralBoundedSolid solid)
    {
        SimpleBody body = new SimpleBody();
        body.setGeometry(solid);
        body.setPosition(new Vector3D());
        body.setRotation(new Matrix4x4());
        body.setRotationInverse(new Matrix4x4());

        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>();
        bodies.add(body);
        return bodies;
    }

    private static Camera createFeaturedCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3D(2.0, -1.0, 2.0));
        Matrix4x4 rotation = new Matrix4x4();
        rotation = rotation.eulerAnglesRotation(Math.toRadians(135.0),
            Math.toRadians(-35.0), 0.0);
        camera.setRotation(rotation);
        camera.updateVectors();
        return camera;
    }

    private static boolean liesOnAnyEdge(PolyhedralBoundedSolid solid,
                                         Vector3D point,
                                         double tolerance)
    {
        for ( int i = 0; i < solid.edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(i);
            if ( edge.leftHalf == null || edge.rightHalf == null ) {
                continue;
            }
            Vector3D start = edge.leftHalf.startingVertex.position;
            Vector3D end = edge.rightHalf.startingVertex.position;
            if ( ComputationalGeometry.lineSegmentContainmentTest(start, end,
                     point, tolerance) == Geometry.LIMIT ) {
                return true;
            }
        }
        return false;
    }
}
