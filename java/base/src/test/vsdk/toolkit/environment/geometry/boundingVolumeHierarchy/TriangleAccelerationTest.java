package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLe;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/triangle_acceleration_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class TriangleAccelerationTest
{
    // Deterministic pseudo random numbers in [0, 1) (no platform rand()).
    private static final class TriangleAccelerationTestRandom {
        private long state;

        TriangleAccelerationTestRandom(long seed)
        {
            state = seed;
        }

        double next()
        {
            state = (state * 1103515245L + 12345L) & 0x7fffffffL;
            return (double)state / 2147483648.0;
        }
    }

    private static void addVertex(ArrayList<Double> positions, double x, double y, double z)
    {
        positions.add(x);
        positions.add(y);
        positions.add(z);
    }

    private static void addTriangle(ArrayList<Integer> indexes, int a, int b, int c)
    {
        indexes.add(a);
        indexes.add(b);
        indexes.add(c);
    }

    // The Java mesh arrays are `double[]` / `int[]` where cpp uses
    // `java::ArrayList`: the lists built by the cpp helpers are converted.
    private static double[] toDoubleArray(ArrayList<Double> values)
    {
        double[] array = new double[values.size()];
        for ( int i = 0; i < array.length; i++ ) array[i] = values.get(i);
        return array;
    }

    private static int[] toIntArray(ArrayList<Integer> values)
    {
        int[] array = new int[values.size()];
        for ( int i = 0; i < array.length; i++ ) array[i] = values.get(i);
        return array;
    }

    /** `mesh.getVertexPositions() = positions; mesh.getTriangleIndexes() = indexes;` */
    private static void assignMeshArrays(TriangleMesh mesh, ArrayList<Double> positions,
        ArrayList<Integer> indexes)
    {
        mesh.initVertexPositionsArray(positions.size() / 3);
        System.arraycopy(toDoubleArray(positions), 0, mesh.getVertexPositions(), 0, positions.size());
        mesh.initTriangleArrays(indexes.size() / 3);
        System.arraycopy(toIntArray(indexes), 0, mesh.getTriangleIndexes(), 0, indexes.size());
    }

    // Triangle soup: overlapping triangles of mixed sizes in [-1, 1]^3.
    private static void buildSoup(int triangleCount, ArrayList<Double> positions,
        ArrayList<Integer> indexes)
    {
        TriangleAccelerationTestRandom random = new TriangleAccelerationTestRandom(7);
        for ( int i = 0; i < triangleCount; i++ ) {
            final double cx = random.next() * 2 - 1;
            final double cy = random.next() * 2 - 1;
            final double cz = random.next() * 2 - 1;
            final double size = random.next() < 0.1 ? 0.8 : 0.08;
            for ( int k = 0; k < 3; k++ ) {
                addVertex(positions, cx + (random.next() - 0.5) * size,
                    cy + (random.next() - 0.5) * size, cz + (random.next() - 0.5) * size);
            }
            addTriangle(indexes, 3 * i, 3 * i + 1, 3 * i + 2);
        }
    }

    // Closed height field with shared vertices and edges, and an axis aligned
    // flat part (zero thickness boxes) to exercise ties and degenerate slabs.
    private static void buildGrid(int n, ArrayList<Double> positions, ArrayList<Integer> indexes)
    {
        for ( int j = 0; j <= n; j++ ) {
            for ( int i = 0; i <= n; i++ ) {
                final double x = (double)i / n * 2 - 1;
                final double y = (double)j / n * 2 - 1;
                final double z = x < 0 ? 0.0 : 0.3 * x * y;
                addVertex(positions, x, y, z);
            }
        }
        for ( int j = 0; j < n; j++ ) {
            for ( int i = 0; i < n; i++ ) {
                final int a = j * (n + 1) + i;
                addTriangle(indexes, a, a + 1, a + n + 2);
                addTriangle(indexes, a, a + n + 2, a + n + 1);
            }
        }
    }

    private static Ray randomRay(TriangleAccelerationTestRandom random, int k)
    {
        final Vector3Dd origin = new Vector3Dd(random.next() * 6 - 3, random.next() * 6 - 3, random.next() * 6 - 3);
        Vector3Dd target = new Vector3Dd(random.next() * 2 - 1, random.next() * 2 - 1, random.next() * 2 - 1);
        Vector3Dd direction = target.subtract(origin).normalized();
        // Some rays exactly parallel to the axes, through vertices and edges.
        if ( k % 7 == 0 ) direction = new Vector3Dd(0, 0, k % 2 == 0 ? -1 : 1);
        if ( k % 11 == 0 ) return new Ray(new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, -1));
        if ( k % 13 == 0 ) return new Ray(new Vector3Dd(-0.5, 0.25, 2), new Vector3Dd(0, 0, -1));
        return new Ray(origin, direction);
    }

    private static void expectSameAsBruteForce(double[] positions, int[] indexes, int rayCount)
    {
        BinnedSahTriangleHierarchy hierarchy = new BinnedSahTriangleHierarchy();
        hierarchy.build(positions, indexes);
        TriangleAccelerationTestRandom random = new TriangleAccelerationTestRandom(11);
        int hits = 0;
        for ( int k = 0; k < rayCount; k++ ) {
            final Ray ray = randomRay(random, k);
            double[] t0 = {0}, u0 = {0}, v0 = {0}, t1 = {0}, u1 = {0}, v1 = {0};
            int[] tri0 = {-1}, tri1 = {-1};
            final boolean hit0 = TriangleAccelerationStructure.firstHitBruteForce(
                ray, positions, indexes, t0, u0, v0, tri0);
            final boolean hit1 = hierarchy.firstHit(ray, positions, indexes, t1, u1, v1, tri1);
            assertThat(hit1).as("ray " + k).isEqualTo(hit0);
            if ( hit0 ) {
                hits++;
                expectEq(tri0[0], tri1[0], "ray " + k);
                expectEq(t0[0], t1[0], "ray " + k);
                expectEq(u0[0], u1[0], "ray " + k);
                expectEq(v0[0], v1[0], "ray " + k);
            }
        }
        expectGt(hits, rayCount / 10);
    }

    @Test
    void BinnedSahTriangleHierarchy_SoupHitsMatchBruteForceExactly()
    {
        ArrayList<Double> positions = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        buildSoup(3000, positions, indexes);
        expectSameAsBruteForce(toDoubleArray(positions), toIntArray(indexes), 4000);
    }

    @Test
    void BinnedSahTriangleHierarchy_SharedEdgeGridHitsMatchBruteForceExactly()
    {
        ArrayList<Double> positions = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        buildGrid(40, positions, indexes);
        expectSameAsBruteForce(toDoubleArray(positions), toIntArray(indexes), 4000);
    }

    @Test
    void BinnedSahTriangleHierarchy_BuildIsDeterministicAndBounded()
    {
        ArrayList<Double> positionList = new ArrayList<>();
        ArrayList<Integer> indexList = new ArrayList<>();
        buildSoup(2000, positionList, indexList);
        final double[] positions = toDoubleArray(positionList);
        final int[] indexes = toIntArray(indexList);
        BinnedSahTriangleHierarchy a = new BinnedSahTriangleHierarchy();
        BinnedSahTriangleHierarchy b = new BinnedSahTriangleHierarchy();
        a.build(positions, indexes);
        b.build(positions, indexes);
        assertThat(b.getNodeCount()).isEqualTo(a.getNodeCount());
        final ArrayList<Integer> orderA = a.getHierarchy().getPrimitiveOrder();
        final ArrayList<Integer> orderB = b.getHierarchy().getPrimitiveOrder();
        for ( int i = 0; i < orderA.size(); i++ ) {
            expectEq(orderA.get(i), orderB.get(i));
        }
        expectLe(a.getDepth(), BinnedSahHierarchy.MAXIMUM_DEPTH);
        expectLe(a.getNodeCount(), 2L * 2000 - 1);
        expectGt(a.getDepth(), 5);
        // Every triangle appears exactly once in the leaves.
        ArrayList<Integer> seen = new ArrayList<>();
        for ( int i = 0; i < 2000; i++ ) seen.add(0);
        final ArrayList<BinnedSahHierarchyNode> nodes = a.getHierarchy().getNodes();
        for ( int i = 0; i < nodes.size(); i++ ) {
            for ( int k = 0; k < nodes.get(i).count; k++ ) {
                final int p = orderA.get(nodes.get(i).leftOrFirst + k);
                seen.set(p, seen.get(p) + 1);
            }
        }
        for ( int i = 0; i < 2000; i++ ) expectEq(1, seen.get(i), "" + i);
    }

    @Test
    void BinnedSahTriangleHierarchy_CoincidentCentroidsStillSplit()
    {
        ArrayList<Double> positionList = new ArrayList<>();
        ArrayList<Integer> indexList = new ArrayList<>();
        for ( int i = 0; i < 100; i++ ) {
            addVertex(positionList, -1, -1, 0);
            addVertex(positionList, 1, -1, 0);
            addVertex(positionList, 0, 2, 0);
            addTriangle(indexList, 3 * i, 3 * i + 1, 3 * i + 2);
        }
        final double[] positions = toDoubleArray(positionList);
        final int[] indexes = toIntArray(indexList);
        BinnedSahTriangleHierarchy hierarchy = new BinnedSahTriangleHierarchy();
        hierarchy.build(positions, indexes);
        expectGt(hierarchy.getNodeCount(), 1);
        double[] t = {0}, u = {0}, v = {0};
        int[] triangle = {-1};
        assertThat(hierarchy.firstHit(new Ray(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1)),
            positions, indexes, t, u, v, triangle)).isTrue();
        expectEq(0, triangle[0]);  // ties go to the lowest index, as brute force
        expectDoubleEq(1.0, t[0]);
    }

    @Test
    void BinnedSahTriangleHierarchy_EmptyMeshMisses()
    {
        final double[] positions = new double[0];
        final int[] indexes = new int[0];
        BinnedSahTriangleHierarchy hierarchy = new BinnedSahTriangleHierarchy();
        hierarchy.build(positions, indexes);
        double[] t = {0}, u = {0}, v = {0};
        int[] triangle = {-1};
        expectFalse(hierarchy.firstHit(new Ray(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1)),
            positions, indexes, t, u, v, triangle));
    }

    @Test
    void TriangleMeshAccelerationMethod_NamesRoundTrip()
    {
        for ( int code = 0; code < TriangleMeshAccelerationMethodUtil.COUNT; code++ ) {
            final TriangleMeshAccelerationMethod method =
                TriangleMeshAccelerationMethodUtil.fromCode(code);
            TriangleMeshAccelerationMethod[] parsed = {TriangleMeshAccelerationMethod.BRUTE_FORCE};
            expectTrue(TriangleMeshAccelerationMethodUtil.fromName(
                TriangleMeshAccelerationMethodUtil.name(method), parsed));
            expectTrue(parsed[0] == method);
        }
        TriangleMeshAccelerationMethod[] unused = {TriangleMeshAccelerationMethod.BRUTE_FORCE};
        expectFalse(TriangleMeshAccelerationMethodUtil.fromName("octree", unused));
        expectTrue(TriangleAccelerationStructure.create(
            TriangleMeshAccelerationMethod.BRUTE_FORCE) == null);
    }

    @Test
    void TriangleMesh_AccelerationStructureIsPreparedCopiedAndReleased()
        throws CloneNotSupportedException
    {
        TriangleMesh mesh = new TriangleMesh();
        ArrayList<Double> positions = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        buildGrid(10, positions, indexes);
        assignMeshArrays(mesh, positions, indexes);
        expectTrue(mesh.getAccelerationStructure() == null);

        final Ray ray = new Ray(new Vector3Dd(0.31, -0.27, 2), new Vector3Dd(0, 0, -1));
        RayHit bruteHit = new RayHit();
        assertThat(mesh.doIntersectionFirstHit(ray, bruteHit)).isTrue();

        mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        final TriangleAccelerationStructure built = mesh.getAccelerationStructure();
        assertThat(built).isNotNull();
        mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        expectTrue(mesh.getAccelerationStructure() == built);  // not rebuilt

        double[] prepared = new double[6];
        assertThat(mesh.getPreparedMinMax(prepared)).isTrue();
        double[] walked = mesh.getMinMax();
        for ( int i = 0; i < 6; i++ ) expectEq(walked[i], prepared[i], "" + i);

        RayHit acceleratedHit = new RayHit();
        assertThat(mesh.doIntersectionFirstHit(ray, acceleratedHit)).isTrue();
        expectEq(bruteHit.point, acceleratedHit.point);
        expectEq(bruteHit.normal, acceleratedHit.normal);

        TriangleMesh copy = mesh.clone();
        assertThat(copy.getAccelerationStructure()).isNotNull();
        expectTrue(copy.getAccelerationStructure() != built);  // deep copy

        mesh.initTriangleArrays(0);  // a mutator releases it
        expectTrue(mesh.getAccelerationStructure() == null);
        expectFalse(mesh.getPreparedMinMax(prepared));
        copy.prepareAccelerationStructure(TriangleMeshAccelerationMethod.BRUTE_FORCE);
        expectTrue(copy.getAccelerationStructure() == null);
    }

    // Many small meshes, as an OBJ file split in groups: the top level hierarchy
    // must give the hit of the linear loop over the meshes (mesh, triangle,
    // point, normal), including meshes sharing an edge.
    @Test
    void TriangleMeshGroup_MeshHierarchyHitsMatchLinearLoop()
        throws CloneNotSupportedException
    {
        TriangleMeshGroup linear = new TriangleMeshGroup();
        TriangleMeshGroup accelerated = new TriangleMeshGroup();
        for ( int m = 0; m < 60; m++ ) {
            ArrayList<Double> positions = new ArrayList<>();
            ArrayList<Integer> indexes = new ArrayList<>();
            if ( m % 3 == 0 ) {
                buildGrid(4, positions, indexes);
                for ( int i = 0; i < positions.size(); i += 3 ) {
                    positions.set(i, positions.get(i) * 0.2 + (m % 6) * 0.3 - 0.9);
                    positions.set(i + 1, positions.get(i + 1) * 0.2 + (m / 6) * 0.2 - 0.9);
                }
            }
            else {
                buildSoup(40, positions, indexes);
            }
            TriangleMesh mesh = new TriangleMesh();
            assignMeshArrays(mesh, positions, indexes);
            // cpp `addMesh` stores a copy; Java stores the reference, so each
            // group gets its own mesh object.
            linear.addMesh(mesh);
            accelerated.addMesh(mesh.clone());
        }
        accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH);
        assertThat(accelerated.getMeshHierarchy()).isNotNull();
        assertThat(linear.getMeshHierarchy()).isNull();
        double[] preparedBounds = new double[6];
        assertThat(accelerated.getPreparedMinMax(preparedBounds)).isTrue();
        double[] walked = accelerated.getMinMax();
        for ( int i = 0; i < 6; i++ ) expectEq(walked[i], preparedBounds[i], "" + i);

        TriangleAccelerationTestRandom random = new TriangleAccelerationTestRandom(5);
        int hits = 0;
        for ( int k = 0; k < 3000; k++ ) {
            final Ray ray = randomRay(random, k);
            RayHit hit0 = new RayHit();
            RayHit hit1 = new RayHit();
            final boolean found0 = linear.doIntersectionFirstHit(ray, hit0);
            int[] info0 = linear.doIntersectionInformation();
            final boolean found1 = accelerated.doIntersectionFirstHit(ray, hit1);
            int[] info1 = accelerated.doIntersectionInformation();
            assertThat(found1).as("ray " + k).isEqualTo(found0);
            expectEq(info0[0], info1[0], "ray " + k);
            expectEq(info0[1], info1[1], "ray " + k);
            if ( found0 ) {
                hits++;
                expectEq(hit0.point, hit1.point, "ray " + k);
                expectEq(hit0.normal, hit1.normal, "ray " + k);
            }
        }
        expectGt(hits, 300);

        accelerated.addMesh(new TriangleMesh());  // drops the top level
        assertThat(accelerated.getMeshHierarchy()).isNull();
        accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod.BRUTE_FORCE);
        assertThat(accelerated.getMeshHierarchy()).isNull();
    }
}
