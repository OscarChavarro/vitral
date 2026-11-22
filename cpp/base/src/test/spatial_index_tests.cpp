#include <gtest/gtest.h>

#include <thread>
#include <vector>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/element/PlanarFace.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/BinnedSahSpatialIndex.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/GridSpatialIndex.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/LinearSpatialIndex.h"

namespace {

// Deterministic generator, so that every run builds the same scene and rays
class Sequence {
  private:
    unsigned int state;

  public:
    explicit Sequence(const unsigned int seed) : state(seed) {}

    double next()
    {
        state = state * 1664525U + 1013904223U;
        return static_cast<double>(state >> 8) / static_cast<double>(1U << 24);
    }
};

// A scene of single precision triangles with padded boxes, as an RPK scene
// gives its faces to the index
class Scene {
  private:
    std::vector<PlanarFace> faces;
    java::ArrayList<double> boxes;

  public:
    void add(const Vector3Df &a, const Vector3Df &b, const Vector3Df &c)
    {
        const Vector3Df p[3] = {a, b, c};
        faces.push_back(PlanarFace(3, p));
        const double padding = 1e-4;
        const Vector3Df *v[3] = {&a, &b, &c};
        double lo[3] = {1e30, 1e30, 1e30};
        double hi[3] = {-1e30, -1e30, -1e30};
        for ( int i = 0; i < 3; i++ ) {
            const double q[3] = {v[i]->x(), v[i]->y(), v[i]->z()};
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

    const PlanarFace &face(const int i) const
    {
        return faces[i];
    }

    int size() const
    {
        return static_cast<int>(faces.size());
    }

    const java::ArrayList<double> &getBoxes() const
    {
        return boxes;
    }
};

Scene
randomTriangles(const int count, const unsigned int seed)
{
    Scene scene;
    Sequence s(seed);
    for ( int i = 0; i < count; i++ ) {
        const float cx = static_cast<float>(10.0 * s.next());
        const float cy = static_cast<float>(10.0 * s.next());
        const float cz = static_cast<float>(10.0 * s.next());
        const Vector3Df a(cx, cy, cz);
        const Vector3Df b(cx + static_cast<float>(s.next()), cy + static_cast<float>(0.2 * s.next()), cz + static_cast<float>(0.5 * s.next()));
        const Vector3Df c(cx + static_cast<float>(0.3 * s.next()), cy + static_cast<float>(s.next()), cz + static_cast<float>(0.4 * s.next()));
        scene.add(a, b, c);
    }
    return scene;
}

// The exact test of the caller: PlanarFace::rayHit on the single precision
// ray, both sides accepted, with an optional excluded face and the lowest id
// winning ties, as brute force
class FaceRayTest final : public PrimitiveRayTest {
  private:
    const Scene &scene;
    const Vector3Df origin;
    const Vector3Df direction;
    const int excluded;
    int acceptedPrimitive;

  public:
    FaceRayTest(const Scene &inScene, const Vector3Df &inOrigin, const Vector3Df &inDirection, const int inExcluded = -1) :
        scene(inScene), origin(inOrigin), direction(inDirection), excluded(inExcluded), acceptedPrimitive(-1)
    {
    }

    bool test(const int primitive, const double minimumT, const double maximumT, double *hitT) final
    {
        if ( primitive == excluded ) {
            return false;
        }
        PlanarFace::RayHit hit;
        if ( !scene.face(primitive).rayHit(origin, direction, true, true,
                static_cast<float>(minimumT), static_cast<float>(maximumT), &hit) ) {
            return false;
        }
        const double t = hit.getDistance();
        if ( t == maximumT && acceptedPrimitive >= 0 && primitive > acceptedPrimitive ) {
            return false;
        }
        acceptedPrimitive = primitive;
        *hitT = t;
        return true;
    }
};

Ray
affineRay(const Vector3Df &origin, const Vector3Df &direction)
{
    return Ray::fromAffine(Vector3Dd(origin.x(), origin.y(), origin.z()),
        Vector3Dd(direction.x(), direction.y(), direction.z()));
}

struct Answer {
    bool hit;
    int primitive;
    double parameter;
};

Answer
ask(const SpatialIndex &index, const Scene &scene, const Vector3Df &origin, const Vector3Df &direction,
    const double minimumT, const double maximumT, const SpatialQueryMode mode,
    SpatialIndexWorkspace &workspace, const int excluded = -1)
{
    FaceRayTest test(scene, origin, direction, excluded);
    const SpatialQueryResult result = index.query(affineRay(origin, direction), minimumT, maximumT, mode, test, workspace);
    Answer answer;
    answer.hit = result.hasHit();
    answer.primitive = result.getPrimitive();
    answer.parameter = result.hasHit() ? result.getParameter() : 0.0;
    return answer;
}

void
randomRay(Sequence &s, Vector3Df *origin, Vector3Df *direction)
{
    *origin = Vector3Df(static_cast<float>(-2.0 + 14.0 * s.next()), static_cast<float>(-2.0 + 14.0 * s.next()), static_cast<float>(-2.0 + 14.0 * s.next()));
    // Not normalized: the index works with the affine parameter of the test
    *direction = Vector3Df(static_cast<float>(-1.0 + 2.0 * s.next()), static_cast<float>(-1.0 + 2.0 * s.next()), static_cast<float>(-1.0 + 2.0 * s.next()));
}

// Union of the boxes of primitives first .. last
void
unionBox(const Scene &scene, const int first, const int last, double *box)
{
    for ( int k = 0; k < 3; k++ ) {
        box[k] = 1e30;
        box[3 + k] = -1e30;
    }
    for ( int i = first; i <= last; i++ ) {
        for ( int k = 0; k < 3; k++ ) {
            const double lo = scene.getBoxes()[6 * i + k];
            const double hi = scene.getBoxes()[6 * i + 3 + k];
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

  public:
    explicit ClusteredScene(const unsigned int seed) : scene(), tree(nullptr)
    {
        Sequence s(seed);
        java::ArrayList<int> clusterFirst;
        for ( int c = 0; c < 60; c++ ) {
            const int count = c % 9 == 8 ? 15 : 1 + c % 8;
            clusterFirst.add(scene.size());
            const double cx = 10.0 * s.next();
            const double cy = 10.0 * s.next();
            const double cz = 10.0 * s.next();
            for ( int i = 0; i < count; i++ ) {
                const float x = static_cast<float>(cx + 0.4 * s.next());
                const float y = static_cast<float>(cy + 0.4 * s.next());
                const float z = static_cast<float>(cz + 0.4 * s.next());
                scene.add(Vector3Df(x, y, z),
                    Vector3Df(x + static_cast<float>(0.2 * s.next()), y + static_cast<float>(0.05 * s.next()), z + static_cast<float>(0.1 * s.next())),
                    Vector3Df(x + static_cast<float>(0.05 * s.next()), y + static_cast<float>(0.2 * s.next()), z + static_cast<float>(0.1 * s.next())));
            }
        }
        clusterFirst.add(scene.size());
        const int firstLarge = scene.size();
        for ( int i = 0; i < 6; i++ ) {
            const float z = static_cast<float>(10.0 * s.next());
            scene.add(Vector3Df(-1.0F, -1.0F, z), Vector3Df(11.0F, -1.0F, z + 0.5F), Vector3Df(5.0F, 11.0F, z - 0.5F));
        }

        double box[6];
        unionBox(scene, 0, scene.size() - 1, box);
        tree = new SpatialGroupTree(scene.getBoxes(), box);
        for ( long int c = 0; c + 1 < clusterFirst.size(); c++ ) {
            unionBox(scene, clusterFirst[c], clusterFirst[c + 1] - 1, box);
            const int group = tree->addGroup(0, box);
            for ( int i = clusterFirst[c]; i < clusterFirst[c + 1]; i++ ) {
                tree->addPrimitive(group, i);
            }
        }
        for ( int i = firstLarge; i < scene.size(); i++ ) {
            tree->addPrimitive(0, i);
        }
    }

    ~ClusteredScene()
    {
        delete tree;
    }

    ClusteredScene(const ClusteredScene &) = delete;
    ClusteredScene &operator=(const ClusteredScene &) = delete;

    const Scene &getScene() const
    {
        return scene;
    }

    const SpatialGroupTree &getTree() const
    {
        return *tree;
    }
};

}

TEST(SpatialIndex, ClosestHitAgreesWithTheLinearReference)
{
    const Scene scene = randomTriangles(400, 7U);
    const LinearSpatialIndex linear(scene.size());
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
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
        ASSERT_EQ(a.hit, b.hit) << "ray " << i;
        if ( a.hit ) {
            hits++;
            EXPECT_EQ(a.primitive, b.primitive) << "ray " << i;
            EXPECT_EQ(a.parameter, b.parameter) << "ray " << i;
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
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
    SpatialIndexWorkspace workspace;
    Sequence s(5U);
    for ( int i = 0; i < 2000; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        // A segment: visibility between two points, as a shadow ray
        const Answer a = ask(linear, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, workspace);
        const Answer b = ask(binned, scene, origin, direction, 1e-3, 1.0, SpatialQueryMode::ANY_HIT, workspace);
        ASSERT_EQ(a.hit, b.hit) << "ray " << i;
        if ( b.hit ) {
            // Any accepted blocker lies in the interval
            EXPECT_GE(b.parameter, 1e-3);
            EXPECT_LE(b.parameter, 1.0);
        }
    }
}

TEST(SpatialIndex, AnyHitStopsAtTheFirstAcceptedBlocker)
{
    // A stack of parallel squares (two triangles each) along the ray
    Scene scene;
    for ( int k = 0; k < 10; k++ ) {
        const float z = static_cast<float>(k + 1);
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
    EXPECT_DOUBLE_EQ(1.0, closest.parameter);
    EXPECT_LT(anyTests, workspace.getPrimitiveTests());
}

TEST(SpatialIndex, MinimumAndMaximumParametersAreInclusiveAndApplied)
{
    Scene scene;
    scene.add(Vector3Df(-1, -1, 1), Vector3Df(1, -1, 1), Vector3Df(0, 1, 1)); // t = 1
    scene.add(Vector3Df(-1, -1, 3), Vector3Df(1, -1, 3), Vector3Df(0, 1, 3)); // t = 3
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
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
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
    SpatialIndexWorkspace workspace;
    for ( int m = 0; m < 2; m++ ) {
        const SpatialQueryMode mode = m == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
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
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>(), 1);
    SpatialIndexWorkspace workspace;
    const Vector3Df origin(-0.5F, 0.0F, 0.0F);
    const Answer a = ask(linear, scene, origin, Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    const Answer b = ask(binned, scene, origin, Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    ASSERT_TRUE(b.hit);
    EXPECT_EQ(a.primitive, b.primitive);
    EXPECT_EQ(a.parameter, b.parameter);
}

TEST(SpatialIndex, UnboundedPrimitivesAreAlwaysTested)
{
    Scene scene;
    scene.add(Vector3Df(-1, -1, 5), Vector3Df(1, -1, 5), Vector3Df(0, 1, 5));
    scene.add(Vector3Df(-100, -100, 2), Vector3Df(100, -100, 2), Vector3Df(0, 100, 2));
    java::ArrayList<int> unboundedPrimitives;
    unboundedPrimitives.add(1);
    // Primitive 1 is unbounded: its box (which would contain it) is ignored
    const BinnedSahSpatialIndex binned(scene.getBoxes(), unboundedPrimitives);
    SpatialIndexWorkspace workspace;
    const Answer a = ask(binned, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace);
    ASSERT_TRUE(a.hit);
    EXPECT_EQ(1, a.primitive);
    EXPECT_DOUBLE_EQ(2.0, a.parameter);
}

TEST(SpatialIndex, EmptyIndex)
{
    const Scene scene;
    const LinearSpatialIndex linear(0);
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
    SpatialIndexWorkspace workspace;
    EXPECT_FALSE(ask(linear, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::CLOSEST_HIT, workspace).hit);
    EXPECT_FALSE(ask(binned, scene, Vector3Df(0, 0, 0), Vector3Df(0, 0, 1), 0.0, 10.0, SpatialQueryMode::ANY_HIT, workspace).hit);
    EXPECT_EQ(0, workspace.getPrimitiveTests());
}

TEST(SpatialIndex, QueriesWriteNoStaticStatistics)
{
    const Scene scene = randomTriangles(100, 13U);
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
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
    const BinnedSahSpatialIndex binned(scene.getBoxes(), java::ArrayList<int>());
    const int numberOfRays = 4000;
    std::vector<Vector3Df> origins;
    std::vector<Vector3Df> directions;
    Sequence s(29U);
    for ( int i = 0; i < numberOfRays; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        origins.push_back(origin);
        directions.push_back(direction);
    }

    std::vector<Answer> sequential(numberOfRays);
    SpatialIndexWorkspace sequentialWorkspace;
    for ( int i = 0; i < numberOfRays; i++ ) {
        sequential[i] = ask(binned, scene, origins[i], directions[i], 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, sequentialWorkspace);
    }

    // One shared index, one workspace and one test per worker
    const int numberOfWorkers = 4;
    std::vector<Answer> concurrent(numberOfRays);
    std::vector<std::thread> workers;
    for ( int w = 0; w < numberOfWorkers; w++ ) {
        workers.push_back(std::thread([&, w]() {
            SpatialIndexWorkspace workspace(binned.getWorkspaceSize());
            for ( int i = w; i < numberOfRays; i += numberOfWorkers ) {
                concurrent[i] = ask(binned, scene, origins[i], directions[i], 0.0, 1e30, SpatialQueryMode::CLOSEST_HIT, workspace);
            }
        }));
    }
    for ( int w = 0; w < numberOfWorkers; w++ ) {
        workers[w].join();
    }
    for ( int i = 0; i < numberOfRays; i++ ) {
        ASSERT_EQ(sequential[i].hit, concurrent[i].hit) << "ray " << i;
        ASSERT_EQ(sequential[i].primitive, concurrent[i].primitive) << "ray " << i;
        ASSERT_EQ(sequential[i].parameter, concurrent[i].parameter) << "ray " << i;
    }
}

TEST(GridSpatialIndex, BuildsTheGridsAsRenderPark)
{
    const ClusteredScene clustered(31U);
    const GridSpatialIndex grid(clustered.getTree());
    const int count = clustered.getScene().size();
    // floor(count^(1/3) + 1) cells along each axis at the top level
    EXPECT_EQ(static_cast<int>(java::Math::floor(java::Math::pow(static_cast<double>(count), 0.33333) + 1.0)), grid.getResolution(0));
    // The clusters of 15 triangles fit in a top level cell: sub grids
    EXPECT_GT(grid.getNumberOfGrids(), 1);
    for ( int g = 1; g < grid.getNumberOfGrids(); g++ ) {
        EXPECT_EQ(static_cast<int>(java::Math::floor(java::Math::pow(15.0, 0.33333) + 1.0)), grid.getResolution(g));
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
        ASSERT_EQ(a.hit, b.hit) << "ray " << i;
        if ( a.hit ) {
            hits++;
            EXPECT_EQ(a.primitive, b.primitive) << "ray " << i;
            EXPECT_EQ(a.parameter, b.parameter) << "ray " << i;
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
        ASSERT_EQ(a.hit, b.hit) << "ray " << i;
        if ( b.hit ) {
            blocked++;
            EXPECT_GE(b.parameter, 1e-3);
            EXPECT_LE(b.parameter, 1.0);
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
    std::vector<Vector3Df> origins;
    std::vector<Vector3Df> directions;
    Sequence s(59U);
    for ( int i = 0; i < numberOfRays; i++ ) {
        Vector3Df origin;
        Vector3Df direction;
        randomRay(s, &origin, &direction);
        origins.push_back(origin);
        directions.push_back(direction);
    }
    std::vector<Answer> sequential(numberOfRays);
    SpatialIndexWorkspace sequentialWorkspace(grid.getWorkspaceSize());
    for ( int i = 0; i < numberOfRays; i++ ) {
        const SpatialQueryMode mode = i % 2 == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
        sequential[i] = ask(grid, scene, origins[i], directions[i], 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, sequentialWorkspace);
    }
    const int numberOfWorkers = 4;
    std::vector<Answer> concurrent(numberOfRays);
    std::vector<std::thread> workers;
    for ( int w = 0; w < numberOfWorkers; w++ ) {
        workers.push_back(std::thread([&, w]() {
            SpatialIndexWorkspace workspace(grid.getWorkspaceSize());
            for ( int i = w; i < numberOfRays; i += numberOfWorkers ) {
                const SpatialQueryMode mode = i % 2 == 0 ? SpatialQueryMode::CLOSEST_HIT : SpatialQueryMode::ANY_HIT;
                concurrent[i] = ask(grid, scene, origins[i], directions[i], 0.0, i % 2 == 0 ? 1e30 : 1.0, mode, workspace);
            }
        }));
    }
    for ( int w = 0; w < numberOfWorkers; w++ ) {
        workers[w].join();
    }
    for ( int i = 0; i < numberOfRays; i++ ) {
        ASSERT_EQ(sequential[i].hit, concurrent[i].hit) << "ray " << i;
        ASSERT_EQ(sequential[i].primitive, concurrent[i].primitive) << "ray " << i;
        ASSERT_EQ(sequential[i].parameter, concurrent[i].parameter) << "ray " << i;
    }
}
