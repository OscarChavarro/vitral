package vsdk.toolkit.environment.geometry.surface;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.RayCrossingTestCollector;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

import java.util.ArrayList;

import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.ReaderMitScene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectThrow;
import static vsdk.toolkit.GTestAssertions.removeFile;
import static vsdk.toolkit.GTestAssertions.tempDir;
import static vsdk.toolkit.GTestAssertions.vitralRepositoryRoot;
import static vsdk.toolkit.GTestAssertions.writeFile;

/**
Port of `cpp/base/src/test/subdivided_bezier_patch_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class SubdividedBezierPatchTest
{
    // Control net over x = i, y = j, so the patch's x and y are 3u and 3v.
    private static void curvedNet(Vector3Dd[][] net)
    {
        for ( int i = 0; i < 4; ++i ) {
            for ( int j = 0; j < 4; ++j ) net[i][j] = new Vector3Dd(i, j, 0.6*Math.sin(1.3*i + 0.8*j));
        }
    }

    @Test
    void SubdividedBezierPatchContract_EverySubdivisionApproachesTheBezierSurface()
    {
        Vector3Dd[][] net = new Vector3Dd[4][4];
        curvedNet(net);
        final BezierPatchSubdivision[] kinds = {BezierPatchSubdivision.UniformGrid,
            BezierPatchSubdivision.PrecomputedGrid, BezierPatchSubdivision.Adaptive,
            BezierPatchSubdivision.AdaptiveTree, BezierPatchSubdivision.SmoothGrid};
        final int[] steps = {16, 16, 4, 4, 16};
        for ( int k = 0; k < 5; ++k ) {
            final SubdividedBezierPatch patch = new SubdividedBezierPatch(kinds[k], steps[k], steps[k], 0.001, net);
            for ( int s = 0; s < 25; ++s ) {
                final double x = 0.3 + 0.1*s, y = 2.7 - 0.09*s;
                final Ray ray = Ray.fromAffine(new Vector3Dd(x, y, 5), new Vector3Dd(0, 0, -1));
                RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
                assertThat(patch.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit))
                    .as(k + " " + s).isEqualTo(RayFirstHitStatus.Hit);
                final double exact = patch.evaluate(x/3, y/3).z();
                expectNear(hit.point.z(), exact, 0.02, k + " " + s);
                // Normals face the u x v side, here +z; the PovRay smooth
                // interpolation can tilt them far from the surface normal.
                assertThat(hit.normal.z()).as(k + " " + s)
                    .isGreaterThan(kinds[k] == BezierPatchSubdivision.SmoothGrid ? 0.0 : 0.5);
                expectNear(hit.normal.length(), 1, 1e-9);
            }
        }
    }

    @Test
    void SubdividedBezierPatchContract_OpenSurfaceCrossingsBoundsAndBudget()
    {
        Vector3Dd[][] net = new Vector3Dd[4][4];
        curvedNet(net);
        final SubdividedBezierPatch patch = new SubdividedBezierPatch(BezierPatchSubdivision.PrecomputedGrid, 8, 8, 0.1, net);
        final Ray ray = Ray.fromAffine(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1));
        RayCrossingTestCollector crossings = new RayCrossingTestCollector();
        assertThat(patch.emitRayCrossings(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), crossings))
            .isEqualTo(RayCrossingSupport.Supported);
        assertThat(crossings.roots).hasSize(1);
        RaySolidClassification classification = new RaySolidClassification();
        assertThat(patch.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification))
            .isEqualTo(RaySolidQueryStatus.Unsupported);
        assertThat(patch.classifyPoint(new Vector3Dd(1, 1, 0), 0)).isEqualTo(GeometryContainment.Unsupported);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT, false);
        assertThat(patch.queryFirstHit(ray, new RayCrossingQuery(-1, Double.POSITIVE_INFINITY), hit))
            .isEqualTo(RayFirstHitStatus.Unsupported);
        assertThat(patch.queryFirstHit(ray, new RayCrossingQuery(0, 1), hit)).isEqualTo(RayFirstHitStatus.Miss);

        GeometryBounds bounds = new GeometryBounds();
        patch.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectDoubleEq(bounds.getMaximum().x(), 3);
        expectDoubleEq(bounds.getMinimum().y(), 0);

        // A spent budget stops the search.
        double[] depths = new double[SubdividedBezierPatch.MAXIMUM_HITS];
        Vector3Dd[] points = new Vector3Dd[SubdividedBezierPatch.MAXIMUM_HITS];
        Vector3Dd[] normals = new Vector3Dd[SubdividedBezierPatch.MAXIMUM_HITS];
        for ( int i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++ ) {
            points[i] = new Vector3Dd();
            normals[i] = new Vector3Dd();
        }
        assertThat(patch.intersect(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1), 0.001, 0, 32, depths, points, normals)).isEqualTo(1);
        assertThat(patch.intersect(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1), 0.001, 32, 32, depths, points, normals)).isEqualTo(0);

        Geometry copy = patch.copyGeometry();
        RayHit copied = new RayHit(RayHit.DETAIL_POINT, false);
        RayHit original = new RayHit(RayHit.DETAIL_POINT, false);
        assertThat(copy.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), copied)).isEqualTo(RayFirstHitStatus.Hit);
        assertThat(patch.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), original)).isEqualTo(RayFirstHitStatus.Hit);
        assertThat(copied.getHitDistance()).isEqualTo(original.getHitDistance());

        assertThatThrownBy(() -> new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 0, 4, 0.1, net))
            .isInstanceOf(IllegalArgumentException.class);
        net[2][1] = new Vector3Dd(Double.NaN, 0, 0);
        assertThatThrownBy(() -> new SubdividedBezierPatch(BezierPatchSubdivision.Adaptive, 2, 2, 0.1, net))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ReaderMitPrimitives_PatchSceneBuildsDomeAndSheet() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(vitralRepositoryRoot() + "/etc/geometry/mitscenes/bezier_patches.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(6);
        assertThat(bodies.get(1).getGeometry()).isInstanceOf(SubdividedBezierPatch.class);
        final SubdividedBezierPatch quadrant = (SubdividedBezierPatch)bodies.get(1).getGeometry();
        expectEq(BezierPatchSubdivision.PrecomputedGrid, quadrant.getSubdivision());
        expectEq(10, quadrant.getUSteps());
        assertThat(bodies.get(5).getGeometry()).isInstanceOf(SubdividedBezierPatch.class);
        final SubdividedBezierPatch sheet = (SubdividedBezierPatch)bodies.get(5).getGeometry();
        expectEq(BezierPatchSubdivision.AdaptiveTree, sheet.getSubdivision());
        // Straight down onto the dome's pole at height 1.4.
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        boolean found = false;
        for ( int i = 1; i <= 4 && !found; ++i ) {
            found = bodies.get(i).doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-1.52, 0.03, 5), new Vector3Dd(0, 0, -1)), hit);
        }
        assertThat(found).isTrue();
        expectNear(5 - 1.4, hit.getHitDistance(), 0.02);
        expectGt(hit.normal.z(), 0.9);
    }

    @Test
    void ReaderMitPrimitives_MalformedPatchBlocksAreRejected() throws Exception
    {
        String controls = "";
        for ( int i = 0; i < 16; ++i ) controls += "control " + (i % 4) + " " + (i / 4) + " 0\n";
        final String[] bodies = {"patch curved 4 4\n" + controls + "end\n", "patch grid 0 4\n" + controls + "end\n",
            "patch grid 4 4\ncontrol 0 0 0\nend\n", "patch grid 4 4\n" + controls + "control 0 0 0\nend\n",
            "patch tree 3 3 flatness\n" + controls + "end\n", "csg union\npatch grid 2 2\n" + controls + "end\nend\n"};
        final String camera = "eye 0 5 5\nlookat 0 0 0\nup 0 1 0\n";
        final String path = tempDir() + "reader_mit_patch_malformed.ray";
        for ( int i = 0; i < 6; ++i ) {
            writeFile(path, camera + bodies[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class, () -> reader.importEnvironment(path, scene), "" + i);
        }
        writeFile(path, camera + "patch tree 3 3 flatness 0.05\n" + controls + "end\n");
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(path, scene);
        assertThat(scene.getSimpleBodies().size()).isEqualTo(1);
        removeFile(path);
    }
}
