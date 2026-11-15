#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include <math.h>
#include <stdio.h>
#include "java/lang/Double.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "RayCrossingTestCollector.h"

// Control net over x = i, y = j, so the patch's x and y are 3u and 3v.
static void curvedNet(Vector3Dd net[4][4])
{
    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 4; ++j) net[i][j] = Vector3Dd(i, j, 0.6*sin(1.3*i + 0.8*j));
    }
}

TEST(SubdividedBezierPatchContract, EverySubdivisionApproachesTheBezierSurface) {
    Vector3Dd net[4][4];
    curvedNet(net);
    const BezierPatchSubdivision::Value kinds[5] = {BezierPatchSubdivision::UniformGrid,
        BezierPatchSubdivision::PrecomputedGrid, BezierPatchSubdivision::Adaptive,
        BezierPatchSubdivision::AdaptiveTree, BezierPatchSubdivision::SmoothGrid};
    const int steps[5] = {16, 16, 4, 4, 16};
    for (int k = 0; k < 5; ++k) {
        const SubdividedBezierPatch patch(kinds[k], steps[k], steps[k], 0.001, net);
        for (int s = 0; s < 25; ++s) {
            const double x = 0.3 + 0.1*s, y = 2.7 - 0.09*s;
            const Ray ray = Ray::fromAffine(Vector3Dd(x, y, 5), Vector3Dd(0, 0, -1));
            RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
            ASSERT_EQ(patch.queryFirstHit(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), &hit),
                RayFirstHitStatus::Hit);
            const double exact = patch.evaluate(x/3, y/3).z();
            EXPECT_NEAR(hit.point.z(), exact, 0.02);
            // Normals face the u x v side, here +z; the PovRay smooth
            // interpolation can tilt them far from the surface normal.
            EXPECT_GT(hit.normal.z(), kinds[k] == BezierPatchSubdivision::SmoothGrid ? 0.0 : 0.5);
            EXPECT_NEAR(hit.normal.length(), 1, 1e-9);
        }
    }
}

TEST(SubdividedBezierPatchContract, OpenSurfaceCrossingsBoundsAndBudget) {
    Vector3Dd net[4][4];
    curvedNet(net);
    const SubdividedBezierPatch patch(BezierPatchSubdivision::PrecomputedGrid, 8, 8, 0.1, net);
    const Ray ray = Ray::fromAffine(Vector3Dd(1.4, 1.1, 5), Vector3Dd(0, 0, -1));
    RayCrossingTestCollector crossings;
    ASSERT_EQ(patch.emitRayCrossings(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), crossings),
        RayCrossingSupport::Supported);
    ASSERT_EQ(crossings.roots.size(), 1);
    RaySolidClassification classification;
    EXPECT_EQ(patch.classifyRay(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), classification),
        RaySolidQueryStatus::Unsupported);
    EXPECT_EQ(patch.classifyPoint(Vector3Dd(1, 1, 0), 0), GeometryContainment::Unsupported);
    RayHit hit(RayHit::DETAIL_POINT, FALSE);
    EXPECT_EQ(patch.queryFirstHit(ray, RayCrossingQuery(-1, Double::POSITIVE_INFINITY), &hit),
        RayFirstHitStatus::Unsupported);
    EXPECT_EQ(patch.queryFirstHit(ray, RayCrossingQuery(0, 1), &hit), RayFirstHitStatus::Miss);

    GeometryBounds bounds;
    patch.getBounds(bounds);
    ASSERT_EQ(bounds.getKind(), GeometryBounds::Kind::Finite);
    EXPECT_DOUBLE_EQ(bounds.getMaximum().x(), 3);
    EXPECT_DOUBLE_EQ(bounds.getMinimum().y(), 0);

    // A spent budget stops the search.
    double depths[SubdividedBezierPatch::MAXIMUM_HITS];
    Vector3Dd points[SubdividedBezierPatch::MAXIMUM_HITS];
    Vector3Dd normals[SubdividedBezierPatch::MAXIMUM_HITS];
    EXPECT_EQ(patch.intersect(Vector3Dd(1.4, 1.1, 5), Vector3Dd(0, 0, -1), 0.001, 0, 32, depths, points, normals), 1);
    EXPECT_EQ(patch.intersect(Vector3Dd(1.4, 1.1, 5), Vector3Dd(0, 0, -1), 0.001, 32, 32, depths, points, normals), 0);

    Geometry* copy = patch.copyGeometry();
    RayHit copied(RayHit::DETAIL_POINT, FALSE);
    RayHit original(RayHit::DETAIL_POINT, FALSE);
    ASSERT_EQ(copy->queryFirstHit(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), &copied), RayFirstHitStatus::Hit);
    ASSERT_EQ(patch.queryFirstHit(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), &original), RayFirstHitStatus::Hit);
    EXPECT_EQ(copied.getHitDistance(), original.getHitDistance());
    delete copy;

    EXPECT_THROW(SubdividedBezierPatch(BezierPatchSubdivision::UniformGrid, 0, 4, 0.1, net), IllegalArgumentException);
    net[2][1] = Vector3Dd(Double::NaN, 0, 0);
    EXPECT_THROW(SubdividedBezierPatch(BezierPatchSubdivision::Adaptive, 2, 2, 0.1, net), IllegalArgumentException);
}

TEST(ReaderMitPrimitives, PatchSceneBuildsDomeAndSheet) {
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/bezier_patches.ray", &scene);
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    ASSERT_EQ(6, bodies.size());
    const SubdividedBezierPatch* quadrant = vitralTestCast(bodies[1]->getGeometry(), SubdividedBezierPatch::CLASS, (const SubdividedBezierPatch*)0);
    ASSERT_NE(NULL, quadrant);
    EXPECT_EQ(BezierPatchSubdivision::PrecomputedGrid, quadrant->getSubdivision());
    EXPECT_EQ(10, quadrant->getUSteps());
    const SubdividedBezierPatch* sheet = vitralTestCast(bodies[5]->getGeometry(), SubdividedBezierPatch::CLASS, (const SubdividedBezierPatch*)0);
    ASSERT_NE(NULL, sheet);
    EXPECT_EQ(BezierPatchSubdivision::AdaptiveTree, sheet->getSubdivision());
    // Straight down onto the dome's pole at height 1.4.
    RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
    BOOLEAN found = FALSE;
    for (long i = 1; i <= 4 && !found; ++i) {
        found = bodies[i]->doIntersectionFirstHit(Ray::fromAffine(Vector3Dd(-1.52, 0.03, 5), Vector3Dd(0, 0, -1)), &hit);
    }
    ASSERT_TRUE(found);
    EXPECT_NEAR(5 - 1.4, hit.getHitDistance(), 0.02);
    EXPECT_GT(hit.normal.z(), 0.9);
}

TEST(ReaderMitPrimitives, MalformedPatchBlocksAreRejected) { int i;
    String controls;
    for (i = 0; i < 16; ++i) controls += "control " + String::valueOf(i % 4) + " " + String::valueOf(i / 4) + " 0\n";
    const String bodies[6] = {"patch curved 4 4\n" + controls + "end\n", "patch grid 0 4\n" + controls + "end\n",
        "patch grid 4 4\ncontrol 0 0 0\nend\n", "patch grid 4 4\n" + controls + "control 0 0 0\nend\n",
        "patch tree 3 3 flatness\n" + controls + "end\n", "csg union\npatch grid 2 2\n" + controls + "end\nend\n"};
    const String camera = "eye 0 5 5\nlookat 0 0 0\nup 0 1 0\n";
    const String path = vitralTestTempDir() + "reader_mit_patch_malformed.ray";
    for (i = 0; i < 6; ++i) {
        FILE* file = fopen(path.c_str(), "w");
        ASSERT_NE(NULL, file);
        fputs((camera + bodies[i]).c_str(), file); fclose(file);
        SimpleScene scene;
        ReaderMitScene reader;
        EXPECT_THROW(reader.importEnvironment(path.c_str(), &scene), Exception);
    }
    FILE* file = fopen(path.c_str(), "w");
    fputs((camera + "patch tree 3 3 flatness 0.05\n" + controls + "end\n").c_str(), file); fclose(file);
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(path.c_str(), &scene);
    ASSERT_EQ(1, scene.getSimpleBodies().size());
    remove(path.c_str());
}

VITRAL_TEST_MAIN()
