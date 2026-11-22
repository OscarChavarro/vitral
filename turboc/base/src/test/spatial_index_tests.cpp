#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"

#include "java/lang/Math.h"
#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/element/PlanarFace.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/BinnedSahSpatialIndex.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/GridSpatialIndex.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/LinearSpatialIndex.h"

// C++ port note: file scope classes and statics instead of an unnamed
// namespace; ArrayList instead of std::vector; the messages streamed into
// the failed expectations of the cpp port ("ray " << i) are not printed.

// Deterministic generator, so that every run builds the same scene and rays
// C++ port note: an unsigned long reduced to 32 bits, as the 32 bit unsigned
// int of the cpp port (16 bits on Turbo C++, 64 on LP64 hosts)
class Sequence {
  private:
    unsigned long state;

  public:
    explicit Sequence(const unsigned long seed) : state(seed & 0xFFFFFFFFUL) {}

    double next()
    {
        state = (state * 1664525UL + 1013904223UL) & 0xFFFFFFFFUL;
        return (double)(state >> 8) / (double)(1UL << 24);
    }
};

// A scene of single precision triangles with padded boxes, as an RPK scene
// gives its faces to the index
class Scene {
  private:
    ArrayList<PlanarFace> faces;
    ArrayList<double> boxes;

  public:
    // C++ port note: declared, so that `const Scene scene;` is valid C++98
    Scene() : faces(), boxes() {}

    void add(const Vector3Df &a, const Vector3Df &b, const Vector3Df &c)
    {
        // C++ port note: arrays filled one by one, without aggregate
        // initializers of non constant values (Turbo C++ 3.0)
        Vector3Df p[3];
        p[0] = a;
        p[1] = b;
        p[2] = c;
        faces.add(PlanarFace(3, p));
        const double padding = 1e-4;
        const Vector3Df *v[3];
        v[0] = &a;
        v[1] = &b;
        v[2] = &c;
        double lo[3] = {1e30, 1e30, 1e30};
        double hi[3] = {-1e30, -1e30, -1e30};
        int k;
        for ( int i = 0; i < 3; i++ ) {
            double q[3];
            q[0] = v[i]->x();
            q[1] = v[i]->y();
            q[2] = v[i]->z();
            for ( k = 0; k < 3; k++ ) {
                lo[k] = q[k] < lo[k] ? q[k] : lo[k];
                hi[k] = q[k] > hi[k] ? q[k] : hi[k];
            }
        }
        for ( k = 0; k < 3; k++ ) {
            boxes.add(lo[k] - padding);
        }
        for ( k = 0; k < 3; k++ ) {
            boxes.add(hi[k] + padding);
        }
    }

    const PlanarFace &face(const int i) const
    {
        return faces[i];
    }

    int size() const
    {
        return (int)faces.size();
    }

    const ArrayList<double> &getBoxes() const
    {
        return boxes;
    }
};

static Scene
randomTriangles(const int count, const unsigned long seed)
{
    Scene scene;
    Sequence s(seed);
    for ( int i = 0; i < count; i++ ) {
        // C++ port note: one draw per statement, from left to right (the
        // cpp port draws them in the arguments of one constructor, in an
        // order the language leaves unspecified)
        const float cx = (float)(10.0 * s.next());
        const float cy = (float)(10.0 * s.next());
        const float cz = (float)(10.0 * s.next());
        const Vector3Df a(cx, cy, cz);
        const float bx = cx + (float)(s.next());
        const float by = cy + (float)(0.2 * s.next());
        const float bz = cz + (float)(0.5 * s.next());
        const Vector3Df b(bx, by, bz);
        const float ccx = cx + (float)(0.3 * s.next());
        const float ccy = cy + (float)(s.next());
        const float ccz = cz + (float)(0.4 * s.next());
        const Vector3Df c(ccx, ccy, ccz);
        scene.add(a, b, c);
    }
    return scene;
}

// The exact test of the caller: PlanarFace::rayHit on the single precision
// ray, both sides accepted, with an optional excluded face and the lowest id
// winning ties, as brute force
class FaceRayTest : public PrimitiveRayTest {
  private:
    const Scene &scene;
    const Vector3Df origin;
    const Vector3Df direction;
    const int excluded;
    int acceptedPrimitive;

  public:
    FaceRayTest(const Scene &inScene, const Vector3Df &inOrigin, const Vector3Df &inDirection, const int inExcluded) :
        scene(inScene), origin(inOrigin), direction(inDirection), excluded(inExcluded), acceptedPrimitive(-1)
    {
    }

    virtual BOOLEAN test(const int primitive, const double minimumT, const double maximumT, double *hitT)
    {
        if ( primitive == excluded ) {
            return FALSE;
        }
        PlanarFace::RayHit hit;
        if ( !scene.face(primitive).rayHit(origin, direction, TRUE, TRUE,
                (float)minimumT, (float)maximumT, &hit) ) {
            return FALSE;
        }
        const double t = hit.distance;
        if ( t == maximumT && acceptedPrimitive >= 0 && primitive > acceptedPrimitive ) {
            return FALSE;
        }
        acceptedPrimitive = primitive;
        *hitT = t;
        return TRUE;
    }
};

static Ray
affineRay(const Vector3Df &origin, const Vector3Df &direction)
{
    return Ray::fromAffine(Vector3Dd(origin.x(), origin.y(), origin.z()),
        Vector3Dd(direction.x(), direction.y(), direction.z()));
}

struct Answer {
    BOOLEAN hit;
    int primitive;
    double t;
};

static Answer
ask(const SpatialIndex &index, const Scene &scene, const Vector3Df &origin, const Vector3Df &direction,
    const double minimumT, const double maximumT, const SpatialQueryMode::Value mode,
    SpatialIndexWorkspace &workspace, const int excluded = -1)
{
    FaceRayTest test(scene, origin, direction, excluded);
    const SpatialQueryResult result = index.query(affineRay(origin, direction), minimumT, maximumT, mode, test, workspace);
    Answer answer;
    answer.hit = result.hasHit();
    answer.primitive = result.getPrimitive();
    answer.t = result.hasHit() ? result.getParameter() : 0.0;
    return answer;
}

static void
randomRay(Sequence &s, Vector3Df *origin, Vector3Df *direction)
{
    // C++ port note: one draw per statement (see randomTriangles)
    const float ox = (float)(-2.0 + 14.0 * s.next());
    const float oy = (float)(-2.0 + 14.0 * s.next());
    const float oz = (float)(-2.0 + 14.0 * s.next());
    *origin = Vector3Df(ox, oy, oz);
    // Not normalized: the index works with the affine parameter of the test
    const float dx = (float)(-1.0 + 2.0 * s.next());
    const float dy = (float)(-1.0 + 2.0 * s.next());
    const float dz = (float)(-1.0 + 2.0 * s.next());
    *direction = Vector3Df(dx, dy, dz);
}

// Union of the boxes of primitives first .. last
static void
unionBox(const Scene &scene, const int first, const int last, double *box)
{
    int k;
    for ( k = 0; k < 3; k++ ) {
        box[k] = 1e30;
        box[3 + k] = -1e30;
    }
    for ( int i = first; i <= last; i++ ) {
        for ( k = 0; k < 3; k++ ) {
            const double lo = scene.getBoxes()[6L * i + k];
            const double hi = scene.getBoxes()[6L * i + 3 + k];
            box[k] = lo < box[k] ? lo : box[k];
            box[3 + k] = hi > box[3 + k] ? hi : box[3 + k];
        }
    }
}

// A scene of small clusters of triangles (as the objects of an RPK scene),
// the cluster sizes cycling through 1 .. 8 and 15, plus a few large
// triangles; and the group tree a grid is built from: the root holds one
// group per cluster, then the large triangles as primitives of their own
class ClusteredScene {
  private:
    Scene scene;
    SpatialGroupTree *tree;

    // C++ port note: not copyable (deleted in the cpp port): declared and
    // not defined
    ClusteredScene(const ClusteredScene &);
    ClusteredScene &operator=(const ClusteredScene &);

  public:
    explicit ClusteredScene(const unsigned long seed) : scene(), tree(NULL)
    {
        Sequence s(seed);
        ArrayList<int> clusterFirst;
        int i;
        for ( int c = 0; c < 60; c++ ) {
            const int count = c % 9 == 8 ? 15 : 1 + c % 8;
            clusterFirst.add(scene.size());
            const double cx = 10.0 * s.next();
            const double cy = 10.0 * s.next();
            const double cz = 10.0 * s.next();
            for ( i = 0; i < count; i++ ) {
                const float x = (float)(cx + 0.4 * s.next());
                const float y = (float)(cy + 0.4 * s.next());
                const float z = (float)(cz + 0.4 * s.next());
                // C++ port note: one draw per statement (see randomTriangles)
                const float bx = x + (float)(0.2 * s.next());
                const float by = y + (float)(0.05 * s.next());
                const float bz = z + (float)(0.1 * s.next());
                const float ccx = x + (float)(0.05 * s.next());
                const float ccy = y + (float)(0.2 * s.next());
                const float ccz = z + (float)(0.1 * s.next());
                scene.add(Vector3Df(x, y, z), Vector3Df(bx, by, bz), Vector3Df(ccx, ccy, ccz));
            }
        }
        clusterFirst.add(scene.size());
        const int firstLarge = scene.size();
        for ( i = 0; i < 6; i++ ) {
            const float z = (float)(10.0 * s.next());
            scene.add(Vector3Df(-1.0F, -1.0F, z), Vector3Df(11.0F, -1.0F, z + 0.5F), Vector3Df(5.0F, 11.0F, z - 0.5F));
        }

        double box[6];
        unionBox(scene, 0, scene.size() - 1, box);
        tree = new SpatialGroupTree(scene.getBoxes(), box);
        for ( long int g = 0; g + 1 < clusterFirst.size(); g++ ) {
            unionBox(scene, clusterFirst[g], clusterFirst[g + 1] - 1, box);
            const int group = tree->addGroup(0, box);
            for ( i = clusterFirst[g]; i < clusterFirst[g + 1]; i++ ) {
                tree->addPrimitive(group, i);
            }
        }
        for ( i = firstLarge; i < scene.size(); i++ ) {
            tree->addPrimitive(0, i);
        }
    }

    ~ClusteredScene()
    {
        delete tree;
    }

    const Scene &getScene() const
    {
        return scene;
    }

    const SpatialGroupTree &getTree() const
    {
        return *tree;
    }
};

/**
C++ port note: the worker lambda of the concurrent cases of the cpp port.
One shared index, one workspace and one test per worker; with `alternate`
the even rays are closest hit queries to 1e30 and the odd ones any hit
queries to 1.
*/
class SpatialIndexTestWorker : public Runnable {
  public:
    const SpatialIndex *index;
    const Scene *scene;
    const ArrayList<Vector3Df> *origins;
    const ArrayList<Vector3Df> *directions;
    ArrayList<Answer> *answers;
    int worker;
    int numberOfWorkers;
    BOOLEAN alternate;

    SpatialIndexTestWorker() :
        index(NULL), scene(NULL), origins(NULL), directions(NULL), answers(NULL),
        worker(0), numberOfWorkers(1), alternate(FALSE)
    {
    }

    virtual void run()
    {
        SpatialIndexWorkspace workspace(index->getWorkspaceSize());
        for ( long int i = worker; i < origins->size(); i += numberOfWorkers ) {
            if ( alternate ) {
                const SpatialQueryMode::Value mode = i % 2 == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
                (*answers)[i] = ask(*index, *scene, (*origins)[i], (*directions)[i], 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, workspace);
            } else {
                (*answers)[i] = ask(*index, *scene, (*origins)[i], (*directions)[i], 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, workspace);
            }
        }
    }
};

/** Runs the workers (C++ port note: one after the other without pthreads) */
static void
runWorkers(SpatialIndexTestWorker *workers, const int numberOfWorkers)
{
    int w;
#ifndef VITRAL_WITHOUT_PTHREADS
    Thread *threads[4];
    for ( w = 0; w < numberOfWorkers; w++ ) {
        threads[w] = new Thread(&workers[w]);
        threads[w]->start();
    }
    for ( w = 0; w < numberOfWorkers; w++ ) {
        threads[w]->join();
        delete threads[w];
    }
#else
    // C++ port note: a single threaded build starts no thread (see
    // PosixThreads.h); the workers run one after the other
    for ( w = 0; w < numberOfWorkers; w++ ) {
        workers[w].run();
    }
#endif
}

TEST(SpatialIndex, ClosestHitAgreesWithTheLinearReference)
{
    const Scene scene = randomTriangles(400, 7U);
    const LinearSpatialIndex linear(scene.size());
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace linearWorkspace(linear.getWorkspaceSize());
    SpatialIndexWorkspace binnedWorkspace(binned.getWorkspaceSize());
    Sequence s(11U);
    int hits = 0;
    for ( int i = 0; i < 2000; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        const Answer a = ask(linear, scene, origin, direction, 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, linearWorkspace);
        const Answer b = ask(binned, scene, origin, direction, 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, binnedWorkspace);
        ASSERT_EQ(a.hit, b.hit);
        if ( a.hit ) {
            hits++;
            EXPECT_EQ(a.primitive, b.primitive);
            EXPECT_EQ(a.t, b.t);
        }
    }
    EXPECT_GT(hits, 100);
    // The hierarchy pruned: far fewer exact tests than brute force
    EXPECT_LT(binnedWorkspace.getPrimitiveTests(), linearWorkspace.getPrimitiveTests() / 4);
    EXPECT_GT(binnedWorkspace.getNodeVisits(), 0);
    EXPECT_EQ(0, linearWorkspace.getNodeVisits());
}

TEST(SpatialIndex, AnyHitAgreesOnVisibility)
{
    const Scene scene = randomTriangles(400, 3U);
    const LinearSpatialIndex linear(scene.size());
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace workspace;
    Sequence s(5U);
    for ( int i = 0; i < 2000; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        // A segment: visibility between two points, as a shadow ray
        const Answer a = ask(linear, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, workspace);
        const Answer b = ask(binned, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, workspace);
        ASSERT_EQ(a.hit, b.hit);
        if ( b.hit ) {
            // Any accepted blocker lies in the interval
            EXPECT_GE(b.t, 1e-3);
            EXPECT_LE(b.t, 1.0);
        }
    }
}

TEST(SpatialIndex, AnyHitStopsAtTheFirstAcceptedBlocker)
{
    // A stack of parallel squares (two triangles each) along the ray
    Scene scene;
    for ( int k = 0; k < 10; k++ ) {
        const float z = (float)(k + 1);
        scene.add(Vector3Df(-1, -1, z), Vector3Df(1, -1, z), Vector3Df(1, 1, z));
        scene.add(Vector3Df(-1, -1, z), Vector3Df(1, 1, z), Vector3Df(-1, 1, z));
    }
    const LinearSpatialIndex linear(scene.size());
    SpatialIndexWorkspace workspace;
    const Answer any = ask(linear, scene, Vector3Df(0.1F, 0.2F, 0), Vector3Df(0, 0, 1), 0.0, 100.0, SpatialQueryMode::ANY_HIT, workspace);
    ASSERT_TRUE(any.hit);
    const long int anyTests = workspace.getPrimitiveTests();
    workspace.clearCounters();
    const Answer closest = ask(linear, scene, Vector3Df(0.1F, 0.2F, 0), Vector3Df(0, 0, 1), 0.0, 100.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(closest.hit);
    EXPECT_DOUBLE_EQ(1.0, closest.t);
    EXPECT_LT(anyTests, workspace.getPrimitiveTests());
}

TEST(SpatialIndex, MinimumAndMaximumParametersAreInclusiveAndApplied)
{
    Scene scene;
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(1, -1, 1), Vector3Df(0, 1, 1)); // t = 1
    scene.add(Vector3Df(-1, -1, 3), Vector3Df(1, -1, 3), Vector3Df(0, 1, 3)); // t = 3
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace workspace;
    const Vector3Df origin(0, 0, 0);
    const Vector3Df up(0, 0, 1);
    // A blocker closer than the minimum is not accepted; the next one is
    Answer a = ask(binned, scene, origin, up, 1.5, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    EXPECT_EQ(1, a.primitive);
    // Inclusive ends
    a = ask(binned, scene, origin, up, 1.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    EXPECT_EQ(0, a.primitive);
    a = ask(binned, scene, origin, up, 1.5, 3.0, SpatialQueryMode::ANY_HIT, workspace);
    EXPECT_EQ(1, a.primitive);
    // A blocker just beyond the end
    a = ask(binned, scene, origin, up, 1.5, 2.999, SpatialQueryMode::ANY_HIT, workspace);
    EXPECT_FALSE(a.hit);
}

TEST(SpatialIndex, ExcludedPrimitiveDoesNotHideTheOneBehind)
{
    Scene scene;
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(1, -1, 1), Vector3Df(0, 1, 1));
    scene.add(Vector3Df(-1, -1, 2), Vector3Df(1, -1, 2), Vector3Df(0, 1, 2));
    const LinearSpatialIndex linear(scene.size());
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace workspace;
    for ( int m = 0; m < 2; m++ ) {
        const SpatialQueryMode::Value mode = m == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
        const Answer a = ask(linear, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
        const Answer b = ask(binned, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, mode, workspace, 0);
        EXPECT_EQ(1, a.primitive);
        EXPECT_EQ(1, b.primitive);
    }
}

TEST(SpatialIndex, EqualParameterTiesFollowTheTestRule)
{
    // Two triangles sharing an edge: a ray through the edge hits both at t = 1
    Scene scene;
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(1, -1, 1), Vector3Df(0, 1, 1));
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(0, 1, 1), Vector3Df(-2, 1, 1));
    const LinearSpatialIndex linear(scene.size());
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>(), 1);
    SpatialIndexWorkspace workspace;
    const Vector3Df origin(-0.5F, 0.0F, 0.0F);
    const Answer a = ask(linear, scene, origin, Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    const Answer b = ask(binned, scene, origin, Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    ASSERT_TRUE(b.hit);
    EXPECT_EQ(a.primitive, b.primitive);
    EXPECT_EQ(a.t, b.t);
}

TEST(SpatialIndex, UnboundedPrimitivesAreAlwaysTested)
{
    Scene scene;
    scene.add(Vector3Df(-1, -1, 5), Vector3Df(1, -1, 5), Vector3Df(0, 1, 5));
    scene.add(Vector3Df(-100, -100, 2), Vector3Df(100, -100, 2), Vector3Df(0, 100, 2));
    ArrayList<int> unboundedPrimitives;
    unboundedPrimitives.add(1);
    // Primitive 1 is unbounded: its box (which would contain it) is ignored
    const BinnedSahSpatialIndex binned(scene.getBoxes(), unboundedPrimitives);
    SpatialIndexWorkspace workspace;
    const Answer a = ask(binned, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    EXPECT_EQ(1, a.primitive);
    EXPECT_DOUBLE_EQ(2.0, a.t);
}

TEST(SpatialIndex, EmptyIndex)
{
    const Scene scene;
    const LinearSpatialIndex linear(0);
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace workspace;
    EXPECT_FALSE(ask(linear, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace).hit);
    EXPECT_FALSE(ask(binned, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::ANY_HIT, workspace).hit);
    EXPECT_EQ(0, workspace.getPrimitiveTests());
}

TEST(SpatialIndex, QueriesWriteNoStaticStatistics)
{
    const Scene scene = randomTriangles(100, 13U);
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    SpatialIndexWorkspace workspace;
    const long long before = RaytraceStatistics::getInstanceLevelTraversals();
    Sequence s(17U);
    for ( int i = 0; i < 100; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        ask(binned, scene, origin, direction, 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, workspace);
    }
    EXPECT_EQ(before, RaytraceStatistics::getInstanceLevelTraversals());
    EXPECT_GT(workspace.getNodeVisits(), 0);
}

TEST(SpatialIndex, WorkspaceMailboxReportsEachPrimitiveOncePerQuery)
{
    SpatialIndexWorkspace workspace(3);
    workspace.beginMailboxQuery();
    EXPECT_TRUE(workspace.firstVisit(2));
    EXPECT_FALSE(workspace.firstVisit(2));
    EXPECT_TRUE(workspace.firstVisit(0));
    workspace.beginMailboxQuery();
    EXPECT_TRUE(workspace.firstVisit(2));
}

TEST(SpatialIndex, ConcurrentQueriesEqualSequentialOnes)
{
    const Scene scene = randomTriangles(300, 23U);
    const BinnedSahSpatialIndex binned(scene.getBoxes(), ArrayList<int>());
    const int numberOfRays = 4000;
    ArrayList<Vector3Df> origins;
    ArrayList<Vector3Df> directions;
    Sequence s(29U);
    int i;
    for ( i = 0; i < numberOfRays; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        origins.add(origin);
        directions.add(direction);
    }

    ArrayList<Answer> sequential;
    SpatialIndexWorkspace sequentialWorkspace;
    for ( i = 0; i < numberOfRays; i++ ) {
        sequential.add(ask(binned, scene, origins[i], directions[i], 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, sequentialWorkspace));
    }

    // One shared index, one workspace and one test per worker
    enum { NUMBER_OF_WORKERS = 4 };
    ArrayList<Answer> concurrent;
    for ( i = 0; i < numberOfRays; i++ ) {
        concurrent.add(Answer());
    }
    SpatialIndexTestWorker workers[NUMBER_OF_WORKERS];
    for ( int w = 0; w < NUMBER_OF_WORKERS; w++ ) {
        workers[w].index = &binned;
        workers[w].scene = &scene;
        workers[w].origins = &origins;
        workers[w].directions = &directions;
        workers[w].answers = &concurrent;
        workers[w].worker = w;
        workers[w].numberOfWorkers = NUMBER_OF_WORKERS;
        workers[w].alternate = FALSE;
    }
    runWorkers(workers, NUMBER_OF_WORKERS);
    for ( i = 0; i < numberOfRays; i++ ) {
        ASSERT_EQ(sequential[i].hit, concurrent[i].hit);
        ASSERT_EQ(sequential[i].primitive, concurrent[i].primitive);
        ASSERT_EQ(sequential[i].t, concurrent[i].t);
    }
}

TEST(GridSpatialIndex, BuildsTheGridsAsRenderPark)
{
    const ClusteredScene clustered(31U);
    const GridSpatialIndex grid(clustered.getTree());
    const int count = clustered.getScene().size();
    // floor(count^(1/3) + 1) cells along each axis at the top level
    EXPECT_EQ((int)(Math::floor(Math::pow((double)count, 0.33333) + 1.0)), grid.getResolution(0));
    // The clusters of 15 triangles fit in a top level cell: sub grids
    EXPECT_GT(grid.getNumberOfGrids(), 1);
    for ( int g = 1; g < grid.getNumberOfGrids(); g++ ) {
        EXPECT_EQ((int)(Math::floor(Math::pow(15.0, 0.33333) + 1.0)), grid.getResolution(g));
    }
    // One mailbox entry per item, fewer items than primitives (group items)
    EXPECT_EQ(grid.getNumberOfItems(), grid.getWorkspaceSize());
    EXPECT_EQ(count, grid.getNumberOfPrimitives());
}

TEST(GridSpatialIndex, ClosestHitAgreesWithTheLinearReference)
{
    const ClusteredScene clustered(37U);
    const Scene &scene = clustered.getScene();
    const LinearSpatialIndex linear(scene.size());
    const GridSpatialIndex grid(clustered.getTree());
    SpatialIndexWorkspace linearWorkspace(linear.getWorkspaceSize());
    SpatialIndexWorkspace gridWorkspace(grid.getWorkspaceSize());
    Sequence s(41U);
    int hits = 0;
    for ( int i = 0; i < 3000; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        const Answer a = ask(linear, scene, origin, direction, 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, linearWorkspace);
        const Answer b = ask(grid, scene, origin, direction, 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, gridWorkspace);
        ASSERT_EQ(a.hit, b.hit);
        if ( a.hit ) {
            hits++;
            EXPECT_EQ(a.primitive, b.primitive);
            EXPECT_EQ(a.t, b.t);
        }
    }
    EXPECT_GT(hits, 300);
    EXPECT_LT(gridWorkspace.getPrimitiveTests(), linearWorkspace.getPrimitiveTests() / 4);
    EXPECT_GT(gridWorkspace.getNodeVisits(), 0);
}

TEST(GridSpatialIndex, AnyHitAgreesOnVisibility)
{
    const ClusteredScene clustered(43U);
    const Scene &scene = clustered.getScene();
    const LinearSpatialIndex linear(scene.size());
    const GridSpatialIndex grid(clustered.getTree());
    SpatialIndexWorkspace linearWorkspace;
    SpatialIndexWorkspace gridWorkspace(grid.getWorkspaceSize());
    Sequence s(47U);
    int blocked = 0;
    for ( int i = 0; i < 3000; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        const Answer a = ask(linear, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, linearWorkspace);
        const Answer b = ask(grid, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, gridWorkspace);
        ASSERT_EQ(a.hit, b.hit);
        if ( b.hit ) {
            blocked++;
            EXPECT_GE(b.t, 1e-3);
            EXPECT_LE(b.t, 1.0);
        }
    }
    EXPECT_GT(blocked, 100);
}

TEST(GridSpatialIndex, IntervalEndsAndExclusionsAreTheTestsOnes)
{
    // Two parallel triangles crossing the whole scene, in one group that does
    // not fit in a cell, so that they are items of their own
    Scene scene;
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(1, -1, 1), Vector3Df(0, 1, 1)); // t = 1
    scene.add(Vector3Df(-1, -1, 3), Vector3Df(1, -1, 3), Vector3Df(0, 1, 3)); // t = 3
    double box[6];
    unionBox(scene, 0, 1, box);
    SpatialGroupTree tree(scene.getBoxes(), box);
    tree.addPrimitive(0, 0);
    tree.addPrimitive(0, 1);
    const GridSpatialIndex grid(tree);
    SpatialIndexWorkspace workspace(grid.getWorkspaceSize());
    const Vector3Df origin(0, 0, 0);
    const Vector3Df up(0, 0, 1);
    Answer a = ask(grid, scene, origin, up, 1.5, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    EXPECT_EQ(1, a.primitive);
    a = ask(grid, scene, origin, up, 1.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    EXPECT_EQ(0, a.primitive);
    a = ask(grid, scene, origin, up, 1.5, 3.0, SpatialQueryMode::ANY_HIT, workspace);
    EXPECT_EQ(1, a.primitive);
    a = ask(grid, scene, origin, up, 1.5, 2.999, SpatialQueryMode::ANY_HIT, workspace);
    EXPECT_FALSE(a.hit);
    // An excluded face does not hide the one behind
    a = ask(grid, scene, origin, up, 0.0, 10.0, SpatialQueryMode::ANY_HIT, workspace, 0);
    EXPECT_EQ(1, a.primitive);
    // A ray starting inside the scene box, along an axis
    a = ask(grid, scene, Vector3Df(0.1F, 0.0F, 2.0F), Vector3Df(0, 0, -1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    EXPECT_EQ(0, a.primitive);
}

TEST(GridSpatialIndex, EmptyTree)
{
    const Scene scene;
    const double box[6] = {0, 0, 0, 1, 1, 1};
    const SpatialGroupTree tree(scene.getBoxes(), box);
    const GridSpatialIndex grid(tree);
    SpatialIndexWorkspace workspace(grid.getWorkspaceSize());
    EXPECT_FALSE(ask(grid, scene, Vector3Df(0, 0, -1), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace).hit);
    EXPECT_EQ(0, workspace.getPrimitiveTests());
}

TEST(GridSpatialIndex, ConcurrentQueriesEqualSequentialOnes)
{
    const ClusteredScene clustered(53U);
    const Scene &scene = clustered.getScene();
    const GridSpatialIndex grid(clustered.getTree());
    const int numberOfRays = 4000;
    ArrayList<Vector3Df> origins;
    ArrayList<Vector3Df> directions;
    Sequence s(59U);
    int i;
    for ( i = 0; i < numberOfRays; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        origins.add(origin);
        directions.add(direction);
    }
    ArrayList<Answer> sequential;
    SpatialIndexWorkspace sequentialWorkspace(grid.getWorkspaceSize());
    for ( i = 0; i < numberOfRays; i++ ) {
        const SpatialQueryMode::Value mode = i % 2 == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
        sequential.add(ask(grid, scene, origins[i], directions[i], 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, sequentialWorkspace));
    }
    enum { NUMBER_OF_WORKERS = 4 };
    ArrayList<Answer> concurrent;
    for ( i = 0; i < numberOfRays; i++ ) {
        concurrent.add(Answer());
    }
    SpatialIndexTestWorker workers[NUMBER_OF_WORKERS];
    for ( int w = 0; w < NUMBER_OF_WORKERS; w++ ) {
        workers[w].index = &grid;
        workers[w].scene = &scene;
        workers[w].origins = &origins;
        workers[w].directions = &directions;
        workers[w].answers = &concurrent;
        workers[w].worker = w;
        workers[w].numberOfWorkers = NUMBER_OF_WORKERS;
        workers[w].alternate = TRUE;
    }
    runWorkers(workers, NUMBER_OF_WORKERS);
    for ( i = 0; i < numberOfRays; i++ ) {
        ASSERT_EQ(sequential[i].hit, concurrent[i].hit);
        ASSERT_EQ(sequential[i].primitive, concurrent[i].primitive);
        ASSERT_EQ(sequential[i].t, concurrent[i].t);
    }
}

VITRAL_TEST_MAIN()
