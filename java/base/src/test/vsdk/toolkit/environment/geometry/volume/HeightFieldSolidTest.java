package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDKFatalException;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.ReaderAltimetry;
import vsdk.toolkit.io.geometry.ReaderMitScene;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectThrow;
import static vsdk.toolkit.GTestAssertions.expectTrue;
import static vsdk.toolkit.GTestAssertions.removeFile;
import static vsdk.toolkit.GTestAssertions.tempDir;
import static vsdk.toolkit.GTestAssertions.vitralRepositoryRoot;
import static vsdk.toolkit.GTestAssertions.writeFile;

/**
Port of `cpp/base/src/test/heightfield_solid_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class HeightFieldSolidTest
{
    @Test
    void HeightFieldSolidContract_PlaneFieldHasExactCrossingsNormalAndContainment()
    {
        final HeightFieldSolid solid = HeightFieldTestFactory.createPlane();
        assertThat(solid.isValid()).isTrue();
        final Ray ray = Ray.fromAffine(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0, -1, 0));
        RaySolidClassification classification = new RaySolidClassification();
        assertThat(solid.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification))
            .isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(classification.boundaries).hasSize(2);
        expectNear(classification.boundaries.get(0).t, 10 - 2.3, 1e-12);
        assertThat(classification.boundaries.get(0).surfaceIndex).isEqualTo(0);
        expectNear(classification.boundaries.get(1).t, 10, 1e-12);
        assertThat(classification.boundaries.get(1).surfaceIndex).isEqualTo(3);
        assertThat(classification.insideSegments).hasSize(3);
        assertThat(classification.insideSegments.get(0)).isFalse();
        assertThat(classification.insideSegments.get(1)).isTrue();
        assertThat(classification.insideSegments.get(2)).isFalse();

        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(solid.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit))
            .isEqualTo(RayFirstHitStatus.Hit);
        final double length = Math.sqrt(0.25 + 1 + 0.0625);
        expectNear(hit.normal.x(), -0.5/length, 1e-12);
        expectNear(hit.normal.y(), 1/length, 1e-12);
        expectNear(hit.normal.z(), -0.25/length, 1e-12);

        // The PovRay traversal agrees on a slanted ray.
        final Ray slanted = Ray.fromAffine(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0.1, -1, -0.05));
        assertThat(solid.classifyRay(slanted, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification))
            .isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(classification.boundaries).isNotEmpty();
        double[] depth = {0};
        assertThat(solid.traceFirstHit(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0.1, -1, -0.05), depth))
            .isEqualTo(HeightFieldTraceResult.Hit);
        expectNear(depth[0], classification.boundaries.get(0).t, 1e-9);
        assertThat(solid.traceFirstHit(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0, 1, 0), depth))
            .isEqualTo(HeightFieldTraceResult.MissedBounds);

        assertThat(solid.classifyPoint(new Vector3Dd(1.3, 1, 2.6), 0)).isEqualTo(GeometryContainment.Inside);
        assertThat(solid.classifyPoint(new Vector3Dd(1.3, 3, 2.6), 0)).isEqualTo(GeometryContainment.Outside);
        assertThat(solid.classifyPointCertified(new Vector3Dd(1.3, 1, 2.6), 1e-6)).isEqualTo(GeometryContainment.Inside);
        assertThat(solid.classifyPointCertified(new Vector3Dd(1.3, 3.9, 2.6), 1e-6)).isEqualTo(GeometryContainment.Outside);
        assertThat(solid.classifyPointCertified(new Vector3Dd(1.3, 2.3, 2.6), 1e-6)).isEqualTo(GeometryContainment.Boundary);
        assertThat(solid.classifyPoint(new Vector3Dd(3.5, 1, 2), 0)).isEqualTo(GeometryContainment.Outside);

        GeometryBounds bounds = new GeometryBounds();
        solid.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectDoubleEq(bounds.getMaximum().y(), 1 + 1.5 + 0.75);
    }

    @Test
    void HeightFieldSolidContract_TraversalAndExactClassificationAgreeOnHills()
    {
        final HeightFieldSolid solid = HeightFieldTestFactory.createHills();
        long compared = 0;
        for ( int i = 0; i < 400; ++i ) {
            final Vector3Dd origin = new Vector3Dd(-3 + 0.055*i, 9, 1.013 + 0.037*i);
            final Vector3Dd direction = new Vector3Dd(Math.cos(0.3*i), -1.1 - 0.4*Math.sin(0.7*i), Math.sin(0.3*i));
            final Ray ray = Ray.fromAffine(origin, direction);
            RaySolidClassification classification = new RaySolidClassification();
            assertThat(solid.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification))
                .as("" + i).isEqualTo(RaySolidQueryStatus.Supported);
            // Membership between boundaries matches point containment.
            for ( int k = 0; k + 1 < classification.boundaries.size(); ++k ) {
                final double t = 0.5*(classification.boundaries.get(k).t + classification.boundaries.get(k + 1).t);
                final GeometryContainment c = solid.classifyPoint(origin.add(direction.multiply(t)), 0);
                assertThat(c == GeometryContainment.Inside).as("" + i)
                    .isEqualTo(classification.insideSegments.get(k + 1));
            }
            double[] depth = {0};
            if ( solid.traceFirstHit(origin, direction, depth) != HeightFieldTraceResult.Hit ) continue;
            // The PovRay traversal may report hits on cells just outside the box.
            final Vector3Dd p = origin.add(direction.multiply(depth[0]));
            if ( p.x() < 1 || p.x() > 15 || p.z() < 1 || p.z() > 15 ) continue;
            assertThat(classification.boundaries).as("" + i).isNotEmpty();
            final RayBoundary first = classification.boundaries.get(0);
            if ( first.surfaceIndex != 0 ) continue;
            expectNear(depth[0], first.t, 1e-6*(1 + first.t), "" + i);
            ++compared;
        }
        assertThat(compared).isGreaterThan(150);
    }

    @Test
    void HeightFieldSolidContract_CertifiedBoxesAndInvalidGrids()
    {
        final HeightFieldSolid solid = HeightFieldTestFactory.createPlane();
        assertThat(solid.classifyBoxCertified(new Vector3Dd(2, 0.5, 2), new Vector3Dd(0.4, 0.2, 0.4))).isEqualTo(GeometryContainment.Inside);
        assertThat(solid.classifyBoxCertified(new Vector3Dd(2, 4.6, 2), new Vector3Dd(0.4, 0.2, 0.4))).isEqualTo(GeometryContainment.Outside);
        assertThat(solid.classifyBoxCertified(new Vector3Dd(2, 2.5, 2), new Vector3Dd(0.4, 0.5, 0.4))).isEqualTo(GeometryContainment.Indeterminate);
        HeightFieldSolid outside = new HeightFieldSolid(3, 3, new Vector3Dd(0, 0, 0), new Vector3Dd(4, 1, 2));
        assertThat(outside.isValid()).isFalse();
        RaySolidClassification classification = new RaySolidClassification();
        assertThat(outside.classifyRay(Ray.fromAffine(new Vector3Dd(1, 5, 1), new Vector3Dd(0, -1, 0)),
            new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification)).isEqualTo(RaySolidQueryStatus.Indeterminate);
    }

    @Test
    void ReaderAltimetryFormat_ReadsGridsAndRejectsMalformedFiles()
    {
        final String path = tempDir() + "reader_altimetry.alt";
        writeFile(path, "# comment\naltimetry 3 2 # columns rows\n0 1 2\n3 4 5\n");
        HeightFieldSolid solid = ReaderAltimetry.importHeightField(path);
        assertThat(solid).isNotNull();
        expectEq(solid.getColumnCount(), 3);
        expectEq(solid.getRowCount(), 2);
        expectDoubleEq(solid.getHeight(2, 1), 5);
        expectDoubleEq(solid.getMinimum().y(), 0);
        expectDoubleEq(solid.getMaximum().y(), 5);
        expectDoubleEq(solid.getMaximum().x(), 2);
        expectTrue(solid.hasCurrentBlocks());
        final String[] malformed = {"altimetry 3 2\n0 1 2\n3 4\n", "altimetry 3 2\n0 1 2\n3 4 5 6\n",
            "altimetry 1 2\n0\n1\n", "heights 2 2\n0 0 0 0\n", "altimetry 2 2\n0 x 0 0\n"};
        for ( int i = 0; i < 5; ++i ) {
            writeFile(path, malformed[i]);
            expectThrow(VSDKFatalException.class, () -> ReaderAltimetry.importHeightField(path), "" + i);
        }
        removeFile(path);
    }

    @Test
    void ReaderMitPrimitives_HeightfieldSceneReadsAltimetryRelativeToTheScene() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(vitralRepositoryRoot() + "/etc/geometry/mitscenes/heightfield_terrain.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(2);
        assertThat(bodies.get(0).getGeometry()).isInstanceOf(HeightFieldSolid.class);
        final HeightFieldSolid terrain = (HeightFieldSolid)bodies.get(0).getGeometry();
        expectEq(65, terrain.getColumnCount());
        expectDoubleEq(0.6, terrain.getMinimum().y());
        // Straight down onto the grid centre (local 32, 32) lands on its height.
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(bodies.get(0).doIntersectionFirstHit(
            Ray.fromAffine(new Vector3Dd(0, 0, 10), new Vector3Dd(0, 0, -1)), hit)).isTrue();
        expectNear(10 - 0.15*terrain.getHeight(32, 32), hit.getHitDistance(), 1e-9);
        expectGt(hit.normal.z(), 0);
    }

    @Test
    void ReaderMitPrimitives_MalformedHeightfieldLinesAreRejected() throws Exception
    {
        final String altimetry = tempDir() + "reader_mit_hf.alt";
        writeFile(altimetry, "altimetry 2 2\n0 1\n1 2\n");
        final String camera = "eye 0 5 5\nlookat 0 0 0\nup 0 1 0\n";
        final String[] bodies = {"heightfield\n", "heightfield missing.alt\n",
            "heightfield reader_mit_hf.alt water 3\n", "heightfield reader_mit_hf.alt level 1\n"};
        final String path = tempDir() + "reader_mit_hf.ray";
        for ( int i = 0; i < 4; ++i ) {
            writeFile(path, camera + bodies[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class, () -> reader.importEnvironment(path, scene), "" + i);
        }
        writeFile(path, camera + "heightfield reader_mit_hf.alt water 0.5\n");
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(path, scene);
        assertThat(scene.getSimpleBodies().size()).isEqualTo(1);
        removeFile(path);
        removeFile(altimetry);
    }
}
