package vsdk.toolkit.io.geometry;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.csg.CsgBody;
import vsdk.toolkit.environment.scene.csg.CsgBodyEvaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGe;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectNe;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectThrow;
import static vsdk.toolkit.GTestAssertions.expectTrue;
import static vsdk.toolkit.GTestAssertions.removeFile;
import static vsdk.toolkit.GTestAssertions.tempDir;
import static vsdk.toolkit.GTestAssertions.vitralRepositoryRoot;
import static vsdk.toolkit.GTestAssertions.writeFile;

/**
Port of `cpp/base/src/test/reader_mit_triangles_tests.cpp`; each test method
is named `<cpp suite>_<cpp test>`.
*/
class ReaderMitTrianglesTest
{
    private static final String VITRAL_REPOSITORY_ROOT = vitralRepositoryRoot();

    @Test
    void ReaderMitTriangles_CowImportsFacesMaterialsAndHasRealHits() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/cow.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(6);
        int total = 0;
        for (int i = 0; i < bodies.size(); ++i) {
            assertThat(bodies.get(i).getGeometry()).isInstanceOf(TriangleMesh.class);
            TriangleMesh mesh = (TriangleMesh)bodies.get(i).getGeometry();
            assertThat(bodies.get(i).getMaterial()).isNotNull();
            expectGt(mesh.getNumTriangles(), 0);
            total += mesh.getNumTriangles();
        }
        expectEq(3640, total);
        assertThat(bodies.get(0).getGeometry()).isInstanceOf(TriangleMesh.class);
        TriangleMesh ground = (TriangleMesh)bodies.get(0).getGeometry();
        expectEq(2, ground.getNumTriangles());
        GeometryBounds bounds = new GeometryBounds();
        ground.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectDoubleEq(-10, bounds.getMinimum().x());
        expectDoubleEq(-4, bounds.getMinimum().y());
        expectDoubleEq(10, bounds.getMaximum().z());
        RayHit groundHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(ground.doIntersectionFirstHit(
            Ray.fromAffine(new Vector3Dd(0,0,0), new Vector3Dd(0,-1,0)), groundHit)).isTrue();
        expectDoubleEq(-4, groundHit.point.y());

        assertThat(bodies.get(1).getGeometry()).isInstanceOf(TriangleMesh.class);
        TriangleMesh cow = (TriangleMesh)bodies.get(1).getGeometry();
        int[] indices = cow.getTriangleIndexes();
        double[] positions = cow.getVertexPositions();
        boolean found = false;
        for (int i = 0; i < cow.getNumTriangles() && !found; ++i) {
            final int a = indices[i*3], b = indices[i*3+1], c = indices[i*3+2];
            final Vector3Dd p = new Vector3Dd(positions[a*3], positions[a*3+1], positions[a*3+2]);
            final Vector3Dd q = new Vector3Dd(positions[b*3], positions[b*3+1], positions[b*3+2]);
            final Vector3Dd r = new Vector3Dd(positions[c*3], positions[c*3+1], positions[c*3+2]);
            final Vector3Dd cross = q.subtract(p).crossProduct(r.subtract(p));
            if (cross.dotProduct(cross) < 1e-8) continue;
            final Vector3Dd normal = cross.normalized();
            final Vector3Dd center = p.add(q).add(r).multiply(1.0/3.0);
            final Ray sample = Ray.fromAffine(center.add(normal), normal.multiply(-1));
            RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
            found = cow.doIntersectionFirstHit(sample, hit);
            if (found) {
                RayHit typed = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
                RayBoundary boundary = new RayBoundary();
                expectEq(RayFirstHitStatus.Hit,
                    cow.queryFirstHit(sample, new RayCrossingQuery(0,10), typed, boundary));
                assertThat(boundary.geometry).isSameAs(cow);
                expectGe(boundary.surfaceIndex, 0);
            }
        }
        expectTrue(found);
    }

    @Test
    void ReaderMitSolids_BoxAndHalfSpaceProduceActualSolidIntervals() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/box_offsets.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(3);
        assertThat(bodies.get(0).getGeometry()).isInstanceOf(PlanarHalfSpace.class);
        assertThat(bodies.get(1).getGeometry()).isInstanceOf(Box.class);
        RaySolidClassification result = new RaySolidClassification();
        final Ray boxRay=Ray.fromAffine(new Vector3Dd(-2,0,0.5),new Vector3Dd(1,0,0));
        assertThat(bodies.get(1).classifyRay(boxRay,new RayCrossingQuery(0,10),result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(2);
        expectNear(0.8,result.boundaries.get(0).t,1e-12); expectNear(2.1,result.boundaries.get(1).t,1e-12);
        final Ray groundRay=Ray.fromAffine(new Vector3Dd(0,0,2),new Vector3Dd(0,0,-1));
        assertThat(bodies.get(0).classifyRay(groundRay,new RayCrossingQuery(0,10),result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(1); assertThat(result.insideSegments.size()).isEqualTo(2);
        expectDoubleEq(2,result.boundaries.get(0).t); expectFalse(result.insideSegments.get(0).booleanValue()); expectTrue(result.insideSegments.get(1).booleanValue());
    }

    @Test
    void ReaderMitCsg_SculptureBlocksBuildCsgBodiesWithTheirEvaluator() throws Exception
    {
        final String[] files={VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/csg_sculpture.ray",
            VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/csg_sculpture_pov.ray"};
        for (int i=0;i<2;++i) {
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            reader.importEnvironment(files[i],scene);
            ArrayList<SimpleBody> bodies=scene.getSimpleBodies();
            assertThat(bodies.size()).isEqualTo(2);
            assertThat(bodies.get(1)).isInstanceOf(CsgBody.class);
            CsgBody csg=(CsgBody)bodies.get(1);
            expectEq(i==0 ? CsgBodyEvaluator.ROTH : CsgBodyEvaluator.CROSSING_MEMBERSHIP,csg.getEvaluator());
            expectEq(3,csg.getNodeCount()); assertThat(csg.getLeafCount()).isEqualTo(5);
            assertThat(csg.getLeaf(2).getMaterial()).isNotSameAs(csg.getLeaf(0).getMaterial());
            RayHit hit = new RayHit(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL,false);
            // Down the vertical hole: Roth sees no solid; the POV heuristic keeps the
            // nested union's internal surface of the x-axis cylinder.
            final boolean axisHit=csg.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(0,0,3),new Vector3Dd(0,0,-1)),hit);
            expectEq(i==1,axisHit);
            if (axisHit) expectNear(3-(0.8+Math.sqrt(0.24)),hit.getHitDistance(),1e-12);
            hit.reset(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL);
            assertThat(csg.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(0.6,0,3),new Vector3Dd(0,0,-1)),hit)).isTrue();
            expectNear(1.45,hit.getHitDistance(),1e-12);
            expectNear(1,hit.normal.z(),1e-12);
            assertThat(hit.material).isSameAs(csg.getLeaf(0).getMaterial());
        }
    }

    @Test
    void ReaderMitCsg_MalformedBlocksAreRejected()
    {
        final String[] bodies={"csg union\nsphere 0 0 0 1\n",
            "csg xor\nsphere 0 0 0 1\nend\n",
            "csg union roth\ncsg intersection pov\nend\nend\n",
            "csg union\ntriangles\nend\nend\n"};
        final String path=tempDir()+"reader_mit_csg_malformed.ray";
        for (int i=0;i<4;++i) {
            writeFile(path,bodies[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class,() -> reader.importEnvironment(path,scene),"" + i);
        }
        removeFile(path);
    }

    @Test
    void ReaderMitPrimitives_PolySceneBuildsPlacedPolynomialBodies() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/poly_surfaces.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(3);
        assertThat(bodies.get(1).getGeometry()).isInstanceOf(PolynomialSurface.class);
        final PolynomialSurface torus = (PolynomialSurface)bodies.get(1).getGeometry();
        expectEq(4, torus.getOrder());
        expectTrue(torus.usesSturmSequences());
        expectDoubleEq(-3.2, torus.getCoefficients()[PolynomialSurface.getTermIndex(4, 2, 0, 0)]);
        expectDoubleEq(-1.7, bodies.get(1).getAffinePlacement().get(0, 3));
        // The tilted torus is hit on its outer equator from -x.
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(bodies.get(1).doIntersectionFirstHit(
            Ray.fromAffine(new Vector3Dd(-6, 0, 0.5), new Vector3Dd(1, 0, 0)), hit)).isTrue();
        expectNear(6 - 1.7 - 1.6, hit.getHitDistance(), 1e-9);
        expectNear(-1, hit.normal.x(), 1e-9);
        assertThat(bodies.get(2)).isInstanceOf(CsgBody.class);
        CsgBody saddle = (CsgBody)bodies.get(2);
        assertThat(saddle.getLeafCount()).isEqualTo(2);
        expectNe(null, saddle.getLeaf(0).getGeometry() instanceof PolynomialSurface ? saddle.getLeaf(0).getGeometry() : null);
        // Below the saddle (z < 0 on its x = 0 line) the cut sphere is solid.
        hit.reset(RayHit.DETAIL_POINT);
        assertThat(saddle.doIntersectionFirstHit(
            Ray.fromAffine(new Vector3Dd(1.7, -5, -0.3), new Vector3Dd(0, 1, 0)), hit)).isTrue();
        expectNear(5 - Math.sqrt(1.21 - 0.25), hit.getHitDistance(), 1e-9);
    }

    @Test
    void ReaderMitPrimitives_MalformedPolyAndTransformLinesAreRejected()
    {
        final String[] bodies = {"poly 8\nend\n", "poly 3\nterm 1 2 2 0\nend\n",
            "poly 2\nterm 1 2 0 0\n", "poly 2 fast\nend\n", "poly 2\nsphere 0 0 0 1\nend\n",
            "transform 1 0 0 0 0 1 0 0 0 0 0 0\nsphere 0 0 0 1\n", "transform 1 0 0 0 0 1 0 0 0 0 1 0\n"};
        final String path = tempDir() + "reader_mit_poly_malformed.ray";
        for (int i = 0; i < 7; ++i) {
            writeFile(path, bodies[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class, () -> reader.importEnvironment(path, scene), "" + i);
        }
        removeFile(path);
    }
}
