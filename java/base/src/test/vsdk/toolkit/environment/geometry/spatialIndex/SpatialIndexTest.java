package vsdk.toolkit.environment.geometry.spatialIndex;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.statistics.RaytraceStatistics;
import vsdk.toolkit.environment.geometry.element.PlanarFace;
import vsdk.toolkit.environment.geometry.element.Ray;

import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGe;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLe;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/spatial_index_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class SpatialIndexTest
{
    // Deterministic generator, so that every run builds the same scene and rays
    // (cpp unsigned int arithmetic: the int wraps the same way)
    private static final class Sequence {
        private int state;

        Sequence(int seed)
        {
            state = seed;
        }

        double next()
        {
            state = state * 1664525 + 1013904223;
            return (double)(state >>> 8) / (double)(1 << 24);
        }
    }

    // A scene of single precision triangles with padded boxes, as an RPK scene
    // gives its faces to the index
    private static final class Scene {
        private final ArrayList<PlanarFace> faces = new ArrayList<>();
        private final ArrayList<Double> boxes = new ArrayList<>();

        void add(Vector3Dd a, Vector3Dd b, Vector3Dd c)
        {
            final Vector3Dd[] p = {a, b, c};
            faces.add(new PlanarFace(3, p));
            final double padding = 1e-4;
            final Vector3Dd[] v = {a, b, c};
            double[] lo = {1e30, 1e30, 1e30};
            double[] hi = {-1e30, -1e30, -1e30};
            for ( int i = 0; i < 3; i++ ) {
                final double[] q = {v[i].x(), v[i].y(), v[i].z()};
                for ( int k = 0; k < 3; k++ ) {
                    lo[k] = q[k] < lo[k] ? q[k] : lo[k];
                    hi[k] = q[k] > hi[k] ? q[k] : hi[k];
                }
            }
            for ( int k = 0; k < 3; k++ ) {
                boxes.add(lo[k] - padding);
            }
            for ( int k = 0; k < 3; k++ ) {
                boxes.add(hi[k] + padding);
            }
        }

        PlanarFace face(int i)
        {
            return faces.get(i);
        }

        int size()
        {
            return faces.size();
        }

        ArrayList<Double> getBoxes()
        {
            return boxes;
        }
    }

    private static Scene randomTriangles(int count, int seed)
    {
        Scene scene = new Scene();
        Sequence s = new Sequence(seed);
        for ( int i = 0; i < count; i++ ) {
            final double cx = (double)(10.0 * s.next());
            final double cy = (double)(10.0 * s.next());
            final double cz = (double)(10.0 * s.next());
            final Vector3Dd a = new Vector3Dd(cx, cy, cz);
            final Vector3Dd b = new Vector3Dd(cx + (double)(s.next()), cy + (double)(0.2 * s.next()), cz + (double)(0.5 * s.next()));
            final Vector3Dd c = new Vector3Dd(cx + (double)(0.3 * s.next()), cy + (double)(s.next()), cz + (double)(0.4 * s.next()));
            scene.add(a, b, c);
        }
        return scene;
    }

    // The exact test of the caller: PlanarFace.rayHit on the single precision
    // ray, both sides accepted, with an optional excluded face and the lowest id
    // winning ties, as brute force
    private static final class FaceRayTest implements PrimitiveRayTest {
        private final Scene scene;
        private final Vector3Dd origin;
        private final Vector3Dd direction;
        private final int excluded;
        private int acceptedPrimitive;

        FaceRayTest(Scene inScene, Vector3Dd inOrigin, Vector3Dd inDirection, int inExcluded)
        {
            scene = inScene;
            origin = inOrigin;
            direction = inDirection;
            excluded = inExcluded;
            acceptedPrimitive = -1;
        }

        @Override
        public boolean test(int primitive, double minimumT, double maximumT, double[] hitT)
        {
            if ( primitive == excluded ) {
                return false;
            }
            PlanarFace.RayHit hit = new PlanarFace.RayHit();
            if ( !scene.face(primitive).rayHit(origin, direction, true, true,
                    (double)minimumT, (double)maximumT, hit) ) {
                return false;
            }
            final double t = hit.distance;
            if ( t == maximumT && acceptedPrimitive >= 0 && primitive > acceptedPrimitive ) {
                return false;
            }
            acceptedPrimitive = primitive;
            hitT[0] = t;
            return true;
        }
    }

    private static Ray affineRay(Vector3Dd origin, Vector3Dd direction)
    {
        return Ray.fromAffine(new Vector3Dd(origin.x(), origin.y(), origin.z()),
            new Vector3Dd(direction.x(), direction.y(), direction.z()));
    }

    private static final class Answer {
        boolean hit;
        int primitive;
        double parameter;
    }

    private static Answer ask(SpatialIndex index, Scene scene, Vector3Dd origin, Vector3Dd direction,
        double minimumT, double maximumT, SpatialQueryMode mode,
        SpatialIndexWorkspace workspace)
    {
        return ask(index, scene, origin, direction, minimumT, maximumT, mode, workspace, -1);
    }

    private static Answer ask(SpatialIndex index, Scene scene, Vector3Dd origin, Vector3Dd direction,
        double minimumT, double maximumT, SpatialQueryMode mode,
        SpatialIndexWorkspace workspace, int excluded)
    {
        FaceRayTest test = new FaceRayTest(scene, origin, direction, excluded);
        final SpatialQueryResult result = index.query(affineRay(origin, direction), minimumT, maximumT, mode, test, workspace);
        Answer answer = new Answer();
        answer.hit = result.hasHit();
        answer.primitive = result.getPrimitive();
        answer.parameter = result.hasHit() ? result.getParameter() : 0.0;
        return answer;
    }

    /** `randomRay(s, &origin, &direction)`: returns {origin, direction} */
    private static Vector3Dd[] randomRay(Sequence s)
    {
        final Vector3Dd origin = new Vector3Dd((double)(-2.0 + 14.0 * s.next()), (double)(-2.0 + 14.0 * s.next()), (double)(-2.0 + 14.0 * s.next()));
        // Not normalized: the index works with the affine parameter of the test
        final Vector3Dd direction = new Vector3Dd((double)(-1.0 + 2.0 * s.next()), (double)(-1.0 + 2.0 * s.next()), (double)(-1.0 + 2.0 * s.next()));
        return new Vector3Dd[] {origin, direction};
    }

    // Union of the boxes of primitives first .. last
    private static void unionBox(Scene scene, int first, int last, double[] box)
    {
        for ( int k = 0; k < 3; k++ ) {
            box[k] = 1e30;
            box[3 + k] = -1e30;
        }
        for ( int i = first; i <= last; i++ ) {
            for ( int k = 0; k < 3; k++ ) {
                final double lo = scene.getBoxes().get(6 * i + k);
                final double hi = scene.getBoxes().get(6 * i + 3 + k);
                box[k] = lo < box[k] ? lo : box[k];
                box[3 + k] = hi > box[3 + k] ? hi : box[3 + k];
            }
        }
    }

    // A scene of small clusters of triangles (as the objects of an RPK scene),
    // the cluster sizes cycling through 1 .. 8 and 15, plus a few large
    // triangles; and the group tree a grid is built from: the root holds one
    // group per cluster, then the large triangles as primitives of their own
    private static final class ClusteredScene {
        private final Scene scene;
        private final SpatialGroupTree tree;

        ClusteredScene(int seed)
        {
            scene = new Scene();
            Sequence s = new Sequence(seed);
            ArrayList<Integer> clusterFirst = new ArrayList<>();
            for ( int c = 0; c < 60; c++ ) {
                final int count = c % 9 == 8 ? 15 : 1 + c % 8;
                clusterFirst.add(scene.size());
                final double cx = 10.0 * s.next();
                final double cy = 10.0 * s.next();
                final double cz = 10.0 * s.next();
                for ( int i = 0; i < count; i++ ) {
                    final double x = (double)(cx + 0.4 * s.next());
                    final double y = (double)(cy + 0.4 * s.next());
                    final double z = (double)(cz + 0.4 * s.next());
                    scene.add(new Vector3Dd(x, y, z),
                        new Vector3Dd(x + (double)(0.2 * s.next()), y + (double)(0.05 * s.next()), z + (double)(0.1 * s.next())),
                        new Vector3Dd(x + (double)(0.05 * s.next()), y + (double)(0.2 * s.next()), z + (double)(0.1 * s.next())));
                }
            }
            clusterFirst.add(scene.size());
            final int firstLarge = scene.size();
            for ( int i = 0; i < 6; i++ ) {
                final double z = (double)(10.0 * s.next());
                scene.add(new Vector3Dd(-1.0, -1.0, z), new Vector3Dd(11.0, -1.0, z + 0.5), new Vector3Dd(5.0, 11.0, z - 0.5));
            }

            double[] box = new double[6];
            unionBox(scene, 0, scene.size() - 1, box);
            tree = new SpatialGroupTree(scene.getBoxes(), box);
            for ( int c = 0; c + 1 < clusterFirst.size(); c++ ) {
                unionBox(scene, clusterFirst.get(c), clusterFirst.get(c + 1) - 1, box);
                final int group = tree.addGroup(0, box);
                for ( int i = clusterFirst.get(c); i < clusterFirst.get(c + 1); i++ ) {
                    tree.addPrimitive(group, i);
                }
            }
            for ( int i = firstLarge; i < scene.size(); i++ ) {
                tree.addPrimitive(0, i);
            }
        }

        Scene getScene()
        {
            return scene;
        }

        SpatialGroupTree getTree()
        {
            return tree;
        }
    }

    @Test
    void SpatialIndex_ClosestHitAgreesWithTheLinearReference()
    {
        final Scene scene = randomTriangles(400, 7);
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace linearWorkspace = new SpatialIndexWorkspace(linear.getWorkspaceSize());
        SpatialIndexWorkspace binnedWorkspace = new SpatialIndexWorkspace(binned.getWorkspaceSize());
        Sequence s = new Sequence(11);
        int hits = 0;
        for ( int i = 0; i < 2000; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            final Answer a = ask(linear, scene, ray[0], ray[1], 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, linearWorkspace);
            final Answer b = ask(binned, scene, ray[0], ray[1], 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, binnedWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if ( a.hit ) {
                hits++;
                expectEq(a.primitive, b.primitive, "ray " + i);
                expectEq(a.parameter, b.parameter, "ray " + i);
            }
        }
        expectGt(hits, 100);
        // The hierarchy pruned: far fewer exact tests than brute force
        expectLt(binnedWorkspace.getPrimitiveTests(), linearWorkspace.getPrimitiveTests() / 4);
        expectGt(binnedWorkspace.getNodeVisits(), 0);
        expectEq(0, linearWorkspace.getNodeVisits());
    }

    @Test
    void SpatialIndex_AnyHitAgreesOnVisibility()
    {
        final Scene scene = randomTriangles(400, 3);
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        Sequence s = new Sequence(5);
        for ( int i = 0; i < 2000; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            // A segment: visibility between two points, as a shadow ray
            final Answer a = ask(linear, scene, ray[0], ray[1], 1e-3, 1.0, SpatialQueryMode.ANY_HIT, workspace);
            final Answer b = ask(binned, scene, ray[0], ray[1], 1e-3, 1.0, SpatialQueryMode.ANY_HIT, workspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if ( b.hit ) {
                // Any accepted blocker lies in the interval
                expectGe(b.parameter, 1e-3);
                expectLe(b.parameter, 1.0);
            }
        }
    }

    @Test
    void SpatialIndex_AnyHitStopsAtTheFirstAcceptedBlocker()
    {
        // A stack of parallel squares (two triangles each) along the ray
        Scene scene = new Scene();
        for ( int k = 0; k < 10; k++ ) {
            final double z = (double)(k + 1);
            scene.add(new Vector3Dd(-1, -1, z), new Vector3Dd(1, -1, z), new Vector3Dd(1, 1, z));
            scene.add(new Vector3Dd(-1, -1, z), new Vector3Dd(1, 1, z), new Vector3Dd(-1, 1, z));
        }
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        final Answer any = ask(linear, scene, new Vector3Dd(0.1, 0.2, 0), new Vector3Dd(0, 0, 1), 0.0, 100.0, SpatialQueryMode.ANY_HIT, workspace);
        expectTrue(any.hit);
        final long anyTests = workspace.getPrimitiveTests();
        workspace.clearCounters();
        final Answer closest = ask(linear, scene, new Vector3Dd(0.1, 0.2, 0), new Vector3Dd(0, 0, 1), 0.0, 100.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(closest.hit);
        expectDoubleEq(1.0, closest.parameter);
        expectLt(anyTests, workspace.getPrimitiveTests());
    }

    @Test
    void SpatialIndex_MinimumAndMaximumParametersAreInclusiveAndApplied()
    {
        Scene scene = new Scene();
        scene.add(new Vector3Dd(-1, -1, 1), new Vector3Dd(1, -1, 1), new Vector3Dd(0, 1, 1)); // t = 1
        scene.add(new Vector3Dd(-1, -1, 3), new Vector3Dd(1, -1, 3), new Vector3Dd(0, 1, 3)); // t = 3
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        final Vector3Dd origin = new Vector3Dd(0, 0, 0);
        final Vector3Dd up = new Vector3Dd(0, 0, 1);
        // A blocker closer than the minimum is not accepted; the next one is
        Answer a = ask(binned, scene, origin, up, 1.5, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        // Inclusive ends
        a = ask(binned, scene, origin, up, 1.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectEq(0, a.primitive);
        a = ask(binned, scene, origin, up, 1.5, 3.0, SpatialQueryMode.ANY_HIT, workspace);
        expectEq(1, a.primitive);
        // A blocker just beyond the end
        a = ask(binned, scene, origin, up, 1.5, 2.999, SpatialQueryMode.ANY_HIT, workspace);
        expectFalse(a.hit);
    }

    @Test
    void SpatialIndex_ExcludedPrimitiveDoesNotHideTheOneBehind()
    {
        Scene scene = new Scene();
        scene.add(new Vector3Dd(-1, -1, 1), new Vector3Dd(1, -1, 1), new Vector3Dd(0, 1, 1));
        scene.add(new Vector3Dd(-1, -1, 2), new Vector3Dd(1, -1, 2), new Vector3Dd(0, 1, 2));
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        for ( int m = 0; m < 2; m++ ) {
            final SpatialQueryMode mode = m == 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
            final Answer a = ask(linear, scene, new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
            final Answer b = ask(binned, scene, new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
            expectEq(1, a.primitive);
            expectEq(1, b.primitive);
        }
    }

    @Test
    void SpatialIndex_EqualParameterTiesFollowTheTestRule()
    {
        // Two triangles sharing an edge: a ray through the edge hits both at t = 1
        Scene scene = new Scene();
        scene.add(new Vector3Dd(-1, -1, 1), new Vector3Dd(1, -1, 1), new Vector3Dd(0, 1, 1));
        scene.add(new Vector3Dd(-1, -1, 1), new Vector3Dd(0, 1, 1), new Vector3Dd(-2, 1, 1));
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>(), 1);
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        final Vector3Dd origin = new Vector3Dd(-0.5, 0.0, 0.0);
        final Answer a = ask(linear, scene, origin, new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        final Answer b = ask(binned, scene, origin, new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectTrue(b.hit);
        expectEq(a.primitive, b.primitive);
        expectEq(a.parameter, b.parameter);
    }

    @Test
    void SpatialIndex_UnboundedPrimitivesAreAlwaysTested()
    {
        Scene scene = new Scene();
        scene.add(new Vector3Dd(-1, -1, 5), new Vector3Dd(1, -1, 5), new Vector3Dd(0, 1, 5));
        scene.add(new Vector3Dd(-100, -100, 2), new Vector3Dd(100, -100, 2), new Vector3Dd(0, 100, 2));
        ArrayList<Integer> unboundedPrimitives = new ArrayList<>();
        unboundedPrimitives.add(1);
        // Primitive 1 is unbounded: its box (which would contain it) is ignored
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), unboundedPrimitives);
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        final Answer a = ask(binned, scene, new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        expectDoubleEq(2.0, a.parameter);
    }

    @Test
    void SpatialIndex_EmptyIndex()
    {
        final Scene scene = new Scene();
        final LinearSpatialIndex linear = new LinearSpatialIndex(0);
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        expectFalse(ask(linear, scene, new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace).hit);
        expectFalse(ask(binned, scene, new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.ANY_HIT, workspace).hit);
        expectEq(0, workspace.getPrimitiveTests());
    }

    @Test
    void SpatialIndex_QueriesWriteNoStaticStatistics()
    {
        final Scene scene = randomTriangles(100, 13);
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace();
        final long before = RaytraceStatistics.getInstanceLevelTraversals();
        Sequence s = new Sequence(17);
        for ( int i = 0; i < 100; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            ask(binned, scene, ray[0], ray[1], 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, workspace);
        }
        expectEq(before, RaytraceStatistics.getInstanceLevelTraversals());
        expectGt(workspace.getNodeVisits(), 0);
    }

    @Test
    void SpatialIndex_WorkspaceMailboxReportsEachPrimitiveOncePerQuery()
    {
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace(3);
        workspace.beginMailboxQuery();
        expectTrue(workspace.firstVisit(2));
        expectFalse(workspace.firstVisit(2));
        expectTrue(workspace.firstVisit(0));
        workspace.beginMailboxQuery();
        expectTrue(workspace.firstVisit(2));
    }

    @Test
    void SpatialIndex_ConcurrentQueriesEqualSequentialOnes() throws InterruptedException
    {
        final Scene scene = randomTriangles(300, 23);
        final BinnedSahSpatialIndex binned = new BinnedSahSpatialIndex(scene.getBoxes(), new ArrayList<>());
        final int numberOfRays = 4000;
        final ArrayList<Vector3Dd> origins = new ArrayList<>();
        final ArrayList<Vector3Dd> directions = new ArrayList<>();
        Sequence s = new Sequence(29);
        for ( int i = 0; i < numberOfRays; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            origins.add(ray[0]);
            directions.add(ray[1]);
        }

        final Answer[] sequential = new Answer[numberOfRays];
        SpatialIndexWorkspace sequentialWorkspace = new SpatialIndexWorkspace();
        for ( int i = 0; i < numberOfRays; i++ ) {
            sequential[i] = ask(binned, scene, origins.get(i), directions.get(i), 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, sequentialWorkspace);
        }

        // One shared index, one workspace and one test per worker
        final int numberOfWorkers = 4;
        final Answer[] concurrent = new Answer[numberOfRays];
        final ArrayList<Thread> workers = new ArrayList<>();
        for ( int w = 0; w < numberOfWorkers; w++ ) {
            final int worker = w;
            workers.add(new Thread(() -> {
                SpatialIndexWorkspace workspace = new SpatialIndexWorkspace(binned.getWorkspaceSize());
                for ( int i = worker; i < numberOfRays; i += numberOfWorkers ) {
                    concurrent[i] = ask(binned, scene, origins.get(i), directions.get(i), 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, workspace);
                }
            }));
            workers.get(w).start();
        }
        for ( int w = 0; w < numberOfWorkers; w++ ) {
            workers.get(w).join();
        }
        for ( int i = 0; i < numberOfRays; i++ ) {
            expectEq(sequential[i].hit, concurrent[i].hit, "ray " + i);
            expectEq(sequential[i].primitive, concurrent[i].primitive, "ray " + i);
            expectEq(sequential[i].parameter, concurrent[i].parameter, "ray " + i);
        }
    }

    @Test
    void GridSpatialIndex_BuildsTheGridsAsRenderPark()
    {
        final ClusteredScene clustered = new ClusteredScene(31);
        final GridSpatialIndex grid = new GridSpatialIndex(clustered.getTree());
        final int count = clustered.getScene().size();
        // floor(count^(1/3) + 1) cells along each axis at the top level
        expectEq((int)(Math.floor(Math.pow((double)count, 0.33333) + 1.0)), grid.getResolution(0));
        // The clusters of 15 triangles fit in a top level cell: sub grids
        expectGt(grid.getNumberOfGrids(), 1);
        for ( int g = 1; g < grid.getNumberOfGrids(); g++ ) {
            expectEq((int)(Math.floor(Math.pow(15.0, 0.33333) + 1.0)), grid.getResolution(g));
        }
        // One mailbox entry per item, fewer items than primitives (group items)
        expectEq(grid.getNumberOfItems(), grid.getWorkspaceSize());
        expectEq(count, grid.getNumberOfPrimitives());
    }

    @Test
    void GridSpatialIndex_ClosestHitAgreesWithTheLinearReference()
    {
        final ClusteredScene clustered = new ClusteredScene(37);
        final Scene scene = clustered.getScene();
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final GridSpatialIndex grid = new GridSpatialIndex(clustered.getTree());
        SpatialIndexWorkspace linearWorkspace = new SpatialIndexWorkspace(linear.getWorkspaceSize());
        SpatialIndexWorkspace gridWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        Sequence s = new Sequence(41);
        int hits = 0;
        for ( int i = 0; i < 3000; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            final Answer a = ask(linear, scene, ray[0], ray[1], 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, linearWorkspace);
            final Answer b = ask(grid, scene, ray[0], ray[1], 0.0, 1e30, SpatialQueryMode.CLOSEST_HIT, gridWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if ( a.hit ) {
                hits++;
                expectEq(a.primitive, b.primitive, "ray " + i);
                expectEq(a.parameter, b.parameter, "ray " + i);
            }
        }
        expectGt(hits, 300);
        expectLt(gridWorkspace.getPrimitiveTests(), linearWorkspace.getPrimitiveTests() / 4);
        expectGt(gridWorkspace.getNodeVisits(), 0);
    }

    @Test
    void GridSpatialIndex_AnyHitAgreesOnVisibility()
    {
        final ClusteredScene clustered = new ClusteredScene(43);
        final Scene scene = clustered.getScene();
        final LinearSpatialIndex linear = new LinearSpatialIndex(scene.size());
        final GridSpatialIndex grid = new GridSpatialIndex(clustered.getTree());
        SpatialIndexWorkspace linearWorkspace = new SpatialIndexWorkspace();
        SpatialIndexWorkspace gridWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        Sequence s = new Sequence(47);
        int blocked = 0;
        for ( int i = 0; i < 3000; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            final Answer a = ask(linear, scene, ray[0], ray[1], 1e-3, 1.0, SpatialQueryMode.ANY_HIT, linearWorkspace);
            final Answer b = ask(grid, scene, ray[0], ray[1], 1e-3, 1.0, SpatialQueryMode.ANY_HIT, gridWorkspace);
            expectEq(a.hit, b.hit, "ray " + i);
            if ( b.hit ) {
                blocked++;
                expectGe(b.parameter, 1e-3);
                expectLe(b.parameter, 1.0);
            }
        }
        expectGt(blocked, 100);
    }

    @Test
    void GridSpatialIndex_IntervalEndsAndExclusionsAreTheTestsOnes()
    {
        // Two parallel triangles crossing the whole scene, in one group that does
        // not fit in a cell, so that they are items of their own
        Scene scene = new Scene();
        scene.add(new Vector3Dd(-1, -1, 1), new Vector3Dd(1, -1, 1), new Vector3Dd(0, 1, 1)); // t = 1
        scene.add(new Vector3Dd(-1, -1, 3), new Vector3Dd(1, -1, 3), new Vector3Dd(0, 1, 3)); // t = 3
        double[] box = new double[6];
        unionBox(scene, 0, 1, box);
        SpatialGroupTree tree = new SpatialGroupTree(scene.getBoxes(), box);
        tree.addPrimitive(0, 0);
        tree.addPrimitive(0, 1);
        final GridSpatialIndex grid = new GridSpatialIndex(tree);
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        final Vector3Dd origin = new Vector3Dd(0, 0, 0);
        final Vector3Dd up = new Vector3Dd(0, 0, 1);
        Answer a = ask(grid, scene, origin, up, 1.5, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(1, a.primitive);
        a = ask(grid, scene, origin, up, 1.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectEq(0, a.primitive);
        a = ask(grid, scene, origin, up, 1.5, 3.0, SpatialQueryMode.ANY_HIT, workspace);
        expectEq(1, a.primitive);
        a = ask(grid, scene, origin, up, 1.5, 2.999, SpatialQueryMode.ANY_HIT, workspace);
        expectFalse(a.hit);
        // An excluded face does not hide the one behind
        a = ask(grid, scene, origin, up, 0.0, 10.0, SpatialQueryMode.ANY_HIT, workspace, 0);
        expectEq(1, a.primitive);
        // A ray starting inside the scene box, along an axis
        a = ask(grid, scene, new Vector3Dd(0.1, 0.0, 2.0), new Vector3Dd(0, 0, -1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace);
        expectTrue(a.hit);
        expectEq(0, a.primitive);
    }

    @Test
    void GridSpatialIndex_EmptyTree()
    {
        final Scene scene = new Scene();
        final double[] box = {0, 0, 0, 1, 1, 1};
        final SpatialGroupTree tree = new SpatialGroupTree(scene.getBoxes(), box);
        final GridSpatialIndex grid = new GridSpatialIndex(tree);
        SpatialIndexWorkspace workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        expectFalse(ask(grid, scene, new Vector3Dd(0, 0, -1), new Vector3Dd(0, 0, 1), 0.0, 10.0, SpatialQueryMode.CLOSEST_HIT, workspace).hit);
        expectEq(0, workspace.getPrimitiveTests());
    }

    @Test
    void GridSpatialIndex_ConcurrentQueriesEqualSequentialOnes() throws InterruptedException
    {
        final ClusteredScene clustered = new ClusteredScene(53);
        final Scene scene = clustered.getScene();
        final GridSpatialIndex grid = new GridSpatialIndex(clustered.getTree());
        final int numberOfRays = 4000;
        final ArrayList<Vector3Dd> origins = new ArrayList<>();
        final ArrayList<Vector3Dd> directions = new ArrayList<>();
        Sequence s = new Sequence(59);
        for ( int i = 0; i < numberOfRays; i++ ) {
            final Vector3Dd[] ray = randomRay(s);
            origins.add(ray[0]);
            directions.add(ray[1]);
        }
        final Answer[] sequential = new Answer[numberOfRays];
        SpatialIndexWorkspace sequentialWorkspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
        for ( int i = 0; i < numberOfRays; i++ ) {
            final SpatialQueryMode mode = i % 2 == 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
            sequential[i] = ask(grid, scene, origins.get(i), directions.get(i), 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, sequentialWorkspace);
        }
        final int numberOfWorkers = 4;
        final Answer[] concurrent = new Answer[numberOfRays];
        final ArrayList<Thread> workers = new ArrayList<>();
        for ( int w = 0; w < numberOfWorkers; w++ ) {
            final int worker = w;
            workers.add(new Thread(() -> {
                SpatialIndexWorkspace workspace = new SpatialIndexWorkspace(grid.getWorkspaceSize());
                for ( int i = worker; i < numberOfRays; i += numberOfWorkers ) {
                    final SpatialQueryMode mode = i % 2 == 0 ? SpatialQueryMode.CLOSEST_HIT : SpatialQueryMode.ANY_HIT;
                    concurrent[i] = ask(grid, scene, origins.get(i), directions.get(i), 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, workspace);
                }
            }));
            workers.get(w).start();
        }
        for ( int w = 0; w < numberOfWorkers; w++ ) {
            workers.get(w).join();
        }
        for ( int i = 0; i < numberOfRays; i++ ) {
            expectEq(sequential[i].hit, concurrent[i].hit, "ray " + i);
            expectEq(sequential[i].primitive, concurrent[i].primitive, "ray " + i);
            expectEq(sequential[i].parameter, concurrent[i].parameter, "ray " + i);
        }
    }
}
