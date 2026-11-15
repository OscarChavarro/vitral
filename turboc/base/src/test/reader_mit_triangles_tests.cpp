#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"

TEST(ReaderMitTriangles, CowImportsFacesMaterialsAndHasRealHits) { long i;
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/cow.ray", &scene);
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    ASSERT_EQ(6, bodies.size());
    int total = 0;
    for (i = 0; i < bodies.size(); ++i) {
        TriangleMesh* mesh = vitralTestCast(bodies[i]->getGeometry(), TriangleMesh::CLASS, (TriangleMesh*)0);
        ASSERT_NE(NULL, mesh);
        ASSERT_NE(NULL, bodies[i]->getMaterial());
        EXPECT_GT(mesh->getNumTriangles(), 0);
        total += mesh->getNumTriangles();
    }
    EXPECT_EQ(3640, total);
    TriangleMesh* ground = vitralTestCast(bodies[0]->getGeometry(), TriangleMesh::CLASS, (TriangleMesh*)0);
    ASSERT_NE(NULL, ground);
    EXPECT_EQ(2, ground->getNumTriangles());
    GeometryBounds bounds;
    ground->getBounds(bounds);
    ASSERT_EQ(GeometryBounds::Kind::Finite, bounds.getKind());
    EXPECT_DOUBLE_EQ(-10, bounds.getMinimum().x());
    EXPECT_DOUBLE_EQ(-4, bounds.getMinimum().y());
    EXPECT_DOUBLE_EQ(10, bounds.getMaximum().z());
    RayHit groundHit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
    ASSERT_TRUE(ground->doIntersectionFirstHit(
        Ray::fromAffine(Vector3Dd(0,0,0), Vector3Dd(0,-1,0)), &groundHit));
    EXPECT_DOUBLE_EQ(-4, groundHit.point.y());

    TriangleMesh* cow = vitralTestCast(bodies[1]->getGeometry(), TriangleMesh::CLASS, (TriangleMesh*)0);
    ASSERT_NE(NULL, cow);
    ArrayList<int>& indices = cow->getTriangleIndexes();
    ArrayList<double>& positions = cow->getVertexPositions();
    BOOLEAN found = FALSE;
    for (i = 0; i < cow->getNumTriangles() && !found; ++i) {
        const int a = indices[i*3], b = indices[i*3+1], c = indices[i*3+2];
        const Vector3Dd p(positions[a*3], positions[a*3+1], positions[a*3+2]);
        const Vector3Dd q(positions[b*3], positions[b*3+1], positions[b*3+2]);
        const Vector3Dd r(positions[c*3], positions[c*3+1], positions[c*3+2]);
        const Vector3Dd cross = q.subtract(p).crossProduct(r.subtract(p));
        if (cross.dotProduct(cross) < 1e-8) continue;
        const Vector3Dd normal = cross.normalized();
        const Vector3Dd center = p.add(q).add(r).multiply(1.0/3.0);
        const Ray sample = Ray::fromAffine(center.add(normal), normal.multiply(-1));
        RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
        found = cow->doIntersectionFirstHit(sample, &hit);
        if (found) {
            RayHit typed(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
            RayBoundary boundary;
            EXPECT_EQ(RayFirstHitStatus::Hit,
                cow->queryFirstHit(sample, RayCrossingQuery(0,10), &typed, &boundary));
            EXPECT_EQ(cow, boundary.geometry);
            EXPECT_GE(boundary.surfaceIndex, 0);
        }
    }
    EXPECT_TRUE(found);
}

TEST(ReaderMitSolids, BoxAndHalfSpaceProduceActualSolidIntervals) {
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/box_offsets.ray", &scene);
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    ASSERT_EQ(3,bodies.size());
    ASSERT_NE(NULL,vitralTestCast(bodies[0]->getGeometry(), PlanarHalfSpace::CLASS, (PlanarHalfSpace*)0));
    ASSERT_NE(NULL,vitralTestCast(bodies[1]->getGeometry(), Box::CLASS, (Box*)0));
    RaySolidClassification result;
    const Ray boxRay=Ray::fromAffine(Vector3Dd(-2,0,0.5),Vector3Dd(1,0,0));
    ASSERT_EQ(RaySolidQueryStatus::Supported,bodies[1]->classifyRay(boxRay,RayCrossingQuery(0,10),result));
    ASSERT_EQ(2,result.boundaries.size());
    EXPECT_NEAR(0.8,result.boundaries[0].t,1e-12); EXPECT_NEAR(2.1,result.boundaries[1].t,1e-12);
    const Ray groundRay=Ray::fromAffine(Vector3Dd(0,0,2),Vector3Dd(0,0,-1));
    ASSERT_EQ(RaySolidQueryStatus::Supported,bodies[0]->classifyRay(groundRay,RayCrossingQuery(0,10),result));
    ASSERT_EQ(1,result.boundaries.size()); ASSERT_EQ(2,result.insideSegments.size());
    EXPECT_DOUBLE_EQ(2,result.boundaries[0].t); EXPECT_FALSE(result.insideSegments[0]); EXPECT_TRUE(result.insideSegments[1]);
}

#include <math.h>
#include <stdio.h>
#include "java/lang/Exception.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBody.h"
TEST(ReaderMitCsg, SculptureBlocksBuildCsgBodiesWithTheirEvaluator) {
    const char* files[2]={VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/csg_sculpture.ray",
        VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/csg_sculpture_pov.ray"};
    for (int i=0;i<2;++i) {
        SimpleScene scene;
        ReaderMitScene reader;
        reader.importEnvironment(files[i],&scene);
        ArrayList<SimpleBody*>& bodies=scene.getSimpleBodies();
        ASSERT_EQ(2,bodies.size());
        CsgBody* csg=vitralTestCast(bodies[1], CsgBody::CLASS, (CsgBody*)0);
        ASSERT_NE(NULL,csg);
        EXPECT_EQ(i==0 ? CsgBodyEvaluator::ROTH : CsgBodyEvaluator::CROSSING_MEMBERSHIP,csg->getEvaluator());
        EXPECT_EQ(3,csg->getNodeCount()); ASSERT_EQ(5,csg->getLeafCount());
        EXPECT_NE(csg->getLeaf(0)->getMaterial(),csg->getLeaf(2)->getMaterial());
        RayHit hit(RayHit::DETAIL_POINT|RayHit::DETAIL_NORMAL,FALSE);
        // Down the vertical hole: Roth sees no solid; the POV heuristic keeps the
        // nested union's internal surface of the x-axis cylinder.
        const BOOLEAN axisHit=csg->doIntersectionFirstHit(Ray::fromAffine(Vector3Dd(0,0,3),Vector3Dd(0,0,-1)),&hit);
        EXPECT_EQ(i==1,axisHit);
        if (axisHit) EXPECT_NEAR(3-(0.8+sqrt(0.24)),hit.getHitDistance(),1e-12);
        hit.reset(RayHit::DETAIL_POINT|RayHit::DETAIL_NORMAL);
        ASSERT_TRUE(csg->doIntersectionFirstHit(Ray::fromAffine(Vector3Dd(0.6,0,3),Vector3Dd(0,0,-1)),&hit));
        EXPECT_NEAR(1.45,hit.getHitDistance(),1e-12);
        EXPECT_NEAR(1,hit.normal.z(),1e-12);
        EXPECT_EQ(csg->getLeaf(0)->getMaterial(),hit.material);
    }
}

TEST(ReaderMitCsg, MalformedBlocksAreRejected) {
    const char* bodies[4]={"csg union\nsphere 0 0 0 1\n",
        "csg xor\nsphere 0 0 0 1\nend\n",
        "csg union roth\ncsg intersection pov\nend\nend\n",
        "csg union\ntriangles\nend\nend\n"};
    const String path=vitralTestTempDir()+"reader_mit_csg_malformed.ray";
    for (int i=0;i<4;++i) {
        FILE* file=fopen(path.c_str(),"w");
        ASSERT_NE(NULL,file);
        fputs(bodies[i],file); fclose(file);
        SimpleScene scene;
        ReaderMitScene reader;
        EXPECT_THROW(reader.importEnvironment(path.c_str(),&scene),Exception);
    }
    remove(path.c_str());
}

TEST(ReaderMitPrimitives, PolySceneBuildsPlacedPolynomialBodies) {
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/poly_surfaces.ray", &scene);
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    ASSERT_EQ(3, bodies.size());
    const PolynomialSurface* torus = vitralTestCast(bodies[1]->getGeometry(), PolynomialSurface::CLASS, (const PolynomialSurface*)0);
    ASSERT_NE(NULL, torus);
    EXPECT_EQ(4, torus->getOrder());
    EXPECT_TRUE(torus->usesSturmSequences());
    EXPECT_DOUBLE_EQ(-3.2, torus->getCoefficients()[PolynomialSurface::getTermIndex(4, 2, 0, 0)]);
    EXPECT_DOUBLE_EQ(-1.7, bodies[1]->getAffinePlacement().get(0, 3));
    // The tilted torus is hit on its outer equator from -x.
    RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL, FALSE);
    ASSERT_TRUE(bodies[1]->doIntersectionFirstHit(
        Ray::fromAffine(Vector3Dd(-6, 0, 0.5), Vector3Dd(1, 0, 0)), &hit));
    EXPECT_NEAR(6 - 1.7 - 1.6, hit.getHitDistance(), 1e-9);
    EXPECT_NEAR(-1, hit.normal.x(), 1e-9);
    CsgBody* saddle = vitralTestCast(bodies[2], CsgBody::CLASS, (CsgBody*)0);
    ASSERT_NE(NULL, saddle);
    ASSERT_EQ(2, saddle->getLeafCount());
    EXPECT_NE(NULL, vitralTestCast(saddle->getLeaf(0)->getGeometry(), PolynomialSurface::CLASS, (const PolynomialSurface*)0));
    // Below the saddle (z < 0 on its x = 0 line) the cut sphere is solid.
    hit.reset(RayHit::DETAIL_POINT);
    ASSERT_TRUE(saddle->doIntersectionFirstHit(
        Ray::fromAffine(Vector3Dd(1.7, -5, -0.3), Vector3Dd(0, 1, 0)), &hit));
    EXPECT_NEAR(5 - sqrt(1.21 - 0.25), hit.getHitDistance(), 1e-9);
}

TEST(ReaderMitPrimitives, MalformedPolyAndTransformLinesAreRejected) {
    const char* bodies[7] = {"poly 8\nend\n", "poly 3\nterm 1 2 2 0\nend\n",
        "poly 2\nterm 1 2 0 0\n", "poly 2 fast\nend\n", "poly 2\nsphere 0 0 0 1\nend\n",
        "transform 1 0 0 0 0 1 0 0 0 0 0 0\nsphere 0 0 0 1\n", "transform 1 0 0 0 0 1 0 0 0 0 1 0\n"};
    const String path = vitralTestTempDir() + "reader_mit_poly_malformed.ray";
    for (int i = 0; i < 7; ++i) {
        FILE* file = fopen(path.c_str(), "w");
        ASSERT_NE(NULL, file);
        fputs(bodies[i], file); fclose(file);
        SimpleScene scene;
        ReaderMitScene reader;
        EXPECT_THROW(reader.importEnvironment(path.c_str(), &scene), Exception);
    }
    remove(path.c_str());
}

VITRAL_TEST_MAIN()
