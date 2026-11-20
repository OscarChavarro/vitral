#include <gtest/gtest.h>

#include <thread>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/element/PlanarFace.h"
#include "vsdk/toolkit/environment/geometry/spatialIndex/BinnedSahSpatialIndex.h"
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
    double t;
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
    answer.t = result.hasHit() ? result.getParameter() : 0.0;
    return answer;
}

void
randomRay(Sequence &s, Vector3Df *origin, Vector3Df *direction)
{
    *origin = Vector3Df(static_cast<float>(-2.0 + 14.0 * s.next()), static_cast<float>(-2.0 + 14.0 * s.next()), static_cast<float>(-2.0 + 14.0 * s.next()));
    // Not normalized: the index works with the affine parameter of the test
    *direction = Vector3Df(static_cast<float>(-1.0 + 2.0 * s.next()), static_cast<float>(-1.0 + 2.0 * s.next()), static_cast<float>(-1.0 + 2.0 * s.next()));
}

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
            EXPECT_EQ(a.t, b.t) << "ray " << i;
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
    EXPECT_DOUBLE_EQ(1.0, closest.t);
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
    EXPECT_EQ(a.t, b.t);
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
    EXPECT_DOUBLE_EQ(2.0, a.t);
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
        ASSERT_EQ(sequential[i].t, concurrent[i].t) << "ray " << i;
    }
}
