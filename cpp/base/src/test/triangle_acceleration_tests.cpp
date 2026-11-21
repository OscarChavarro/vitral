#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahInstanceHierarchy.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahTriangleHierarchy.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/GridTriangleHierarchy.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleAccelerationStructure.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"

// Deterministic pseudo random numbers in [0, 1) (no platform rand()).
class TriangleAccelerationTestRandom {
  private:
    unsigned long state;
  public:
    explicit TriangleAccelerationTestRandom(unsigned long seed) : state(seed) {}
    double next()
    {
        state = (state * 1103515245UL + 12345UL) & 0x7fffffffUL;
        return (double)state / 2147483648.0;
    }
};

static void addVertex(java::ArrayList<double> &positions, double x, double y, double z)
{
    positions.add(x);
    positions.add(y);
    positions.add(z);
}

static void addTriangle(java::ArrayList<int> &indexes, int a, int b, int c)
{
    indexes.add(a);
    indexes.add(b);
    indexes.add(c);
}

// Triangle soup: overlapping triangles of mixed sizes in [-1, 1]^3.
static void buildSoup(int triangleCount, java::ArrayList<double> &positions,
    java::ArrayList<int> &indexes)
{
    TriangleAccelerationTestRandom random(7);
    for (int i = 0; i < triangleCount; i++) {
        const double cx = random.next() * 2 - 1;
        const double cy = random.next() * 2 - 1;
        const double cz = random.next() * 2 - 1;
        const double size = random.next() < 0.1 ? 0.8 : 0.08;
        for (int k = 0; k < 3; k++) {
            addVertex(positions, cx + (random.next() - 0.5) * size,
                cy + (random.next() - 0.5) * size, cz + (random.next() - 0.5) * size);
        }
        addTriangle(indexes, 3 * i, 3 * i + 1, 3 * i + 2);
    }
}

// Closed height field with shared vertices and edges, and an axis aligned
// flat part (zero thickness boxes) to exercise ties and degenerate slabs.
static void buildGrid(int n, java::ArrayList<double> &positions, java::ArrayList<int> &indexes)
{
    for (int j = 0; j <= n; j++) {
        for (int i = 0; i <= n; i++) {
            const double x = (double)i / n * 2 - 1;
            const double y = (double)j / n * 2 - 1;
            const double z = x < 0 ? 0.0 : 0.3 * x * y;
            addVertex(positions, x, y, z);
        }
    }
    for (int j = 0; j < n; j++) {
        for (int i = 0; i < n; i++) {
            const int a = j * (n + 1) + i;
            addTriangle(indexes, a, a + 1, a + n + 2);
            addTriangle(indexes, a, a + n + 2, a + n + 1);
        }
    }
}

static Ray randomRay(TriangleAccelerationTestRandom &random, int k)
{
    const Vector3Dd origin(random.next() * 6 - 3, random.next() * 6 - 3, random.next() * 6 - 3);
    Vector3Dd target(random.next() * 2 - 1, random.next() * 2 - 1, random.next() * 2 - 1);
    Vector3Dd direction = target.subtract(origin).normalized();
    // Some rays exactly parallel to the axes, through vertices and edges.
    if (k % 7 == 0) direction = Vector3Dd(0, 0, k % 2 == 0 ? -1 : 1);
    if (k % 11 == 0) return Ray(Vector3Dd(0, 0, 2), Vector3Dd(0, 0, -1));
    if (k % 13 == 0) return Ray(Vector3Dd(-0.5, 0.25, 2), Vector3Dd(0, 0, -1));
    return Ray(origin, direction);
}

static void expectSameAsBruteForce(TriangleAccelerationStructure &hierarchy,
    const java::ArrayList<double> &positions, const java::ArrayList<int> &indexes, int rayCount)
{
    hierarchy.build(positions, indexes);
    TriangleAccelerationTestRandom random(11);
    int hits = 0;
    for (int k = 0; k < rayCount; k++) {
        const Ray ray = randomRay(random, k);
        double t0 = 0, u0 = 0, v0 = 0, t1 = 0, u1 = 0, v1 = 0;
        int tri0 = -1, tri1 = -1;
        const bool hit0 = TriangleAccelerationStructure::firstHitBruteForce(
            ray, positions, indexes, t0, u0, v0, tri0);
        const bool hit1 = hierarchy.firstHit(ray, positions, indexes, t1, u1, v1, tri1);
        ASSERT_EQ(hit0, hit1) << "ray " << k;
        if (hit0) {
            hits++;
            EXPECT_EQ(tri0, tri1) << "ray " << k;
            EXPECT_EQ(t0, t1) << "ray " << k;
            EXPECT_EQ(u0, u1) << "ray " << k;
            EXPECT_EQ(v0, v1) << "ray " << k;
        }
    }
    EXPECT_GT(hits, rayCount / 10);
}

static void expectSameAsBruteForce(const java::ArrayList<double> &positions,
    const java::ArrayList<int> &indexes, int rayCount)
{
    BinnedSahTriangleHierarchy hierarchy;
    expectSameAsBruteForce(hierarchy, positions, indexes, rayCount);
}

TEST(BinnedSahTriangleHierarchy, SoupHitsMatchBruteForceExactly) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildSoup(3000, positions, indexes);
    expectSameAsBruteForce(positions, indexes, 4000);
}

TEST(BinnedSahTriangleHierarchy, SharedEdgeGridHitsMatchBruteForceExactly) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildGrid(40, positions, indexes);
    expectSameAsBruteForce(positions, indexes, 4000);
}

TEST(BinnedSahTriangleHierarchy, BuildIsDeterministicAndBounded) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildSoup(2000, positions, indexes);
    BinnedSahTriangleHierarchy a;
    BinnedSahTriangleHierarchy b;
    a.build(positions, indexes);
    b.build(positions, indexes);
    ASSERT_EQ(a.getNodeCount(), b.getNodeCount());
    const java::ArrayList<int> &orderA = a.getHierarchy().getPrimitiveOrder();
    const java::ArrayList<int> &orderB = b.getHierarchy().getPrimitiveOrder();
    for (long int i = 0; i < orderA.size(); i++) {
        EXPECT_EQ(orderA[i], orderB[i]);
    }
    EXPECT_LE(a.getDepth(), BinnedSahHierarchy::MAXIMUM_DEPTH);
    EXPECT_LE(a.getNodeCount(), 2L * 2000 - 1);
    EXPECT_GT(a.getDepth(), 5);
    // Every triangle appears exactly once in the leaves.
    java::ArrayList<int> seen;
    for (int i = 0; i < 2000; i++) seen.add(0);
    const java::ArrayList<BinnedSahHierarchyNode> &nodes = a.getHierarchy().getNodes();
    for (long int i = 0; i < nodes.size(); i++) {
        for (int k = 0; k < nodes[i].count; k++) {
            seen[orderA[nodes[i].leftOrFirst + k]]++;
        }
    }
    for (int i = 0; i < 2000; i++) EXPECT_EQ(1, seen[i]) << i;
}

TEST(BinnedSahTriangleHierarchy, CoincidentCentroidsStillSplit) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    for (int i = 0; i < 100; i++) {
        addVertex(positions, -1, -1, 0);
        addVertex(positions, 1, -1, 0);
        addVertex(positions, 0, 2, 0);
        addTriangle(indexes, 3 * i, 3 * i + 1, 3 * i + 2);
    }
    BinnedSahTriangleHierarchy hierarchy;
    hierarchy.build(positions, indexes);
    EXPECT_GT(hierarchy.getNodeCount(), 1);
    double t = 0, u = 0, v = 0;
    int triangle = -1;
    ASSERT_TRUE(hierarchy.firstHit(Ray(Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -1)),
        positions, indexes, t, u, v, triangle));
    EXPECT_EQ(0, triangle);  // ties go to the lowest index, as brute force
    EXPECT_DOUBLE_EQ(1.0, t);
}

TEST(BinnedSahTriangleHierarchy, EmptyMeshMisses) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    BinnedSahTriangleHierarchy hierarchy;
    hierarchy.build(positions, indexes);
    double t = 0, u = 0, v = 0;
    int triangle = -1;
    EXPECT_FALSE(hierarchy.firstHit(Ray(Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -1)),
        positions, indexes, t, u, v, triangle));
}

TEST(GridTriangleHierarchy, SoupHitsMatchBruteForceExactly) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildSoup(3000, positions, indexes);
    GridTriangleHierarchy grid;
    expectSameAsBruteForce(grid, positions, indexes, 4000);
    // floor(3000^(1/3) + 1) cells along each axis
    EXPECT_EQ(15, grid.getResolution());
}

TEST(GridTriangleHierarchy, SharedEdgeGridHitsMatchBruteForceExactly) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildGrid(40, positions, indexes);
    GridTriangleHierarchy grid;
    expectSameAsBruteForce(grid, positions, indexes, 4000);
}

TEST(GridTriangleHierarchy, CopyAnswersAsTheOriginal) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildSoup(500, positions, indexes);
    GridTriangleHierarchy grid;
    grid.build(positions, indexes);
    TriangleAccelerationStructure *copied = grid.copy();
    ASSERT_TRUE(copied != 0);
    EXPECT_TRUE(copied->getMethod() == TriangleMeshAccelerationMethod::VOXEL_GRID);
    EXPECT_TRUE(copied->matches((int)(positions.size() / 3), (int)(indexes.size() / 3)));
    TriangleAccelerationTestRandom random(5);
    for (int k = 0; k < 500; k++) {
        const Ray ray = randomRay(random, k);
        double t0 = 0, u0 = 0, v0 = 0, t1 = 0, u1 = 0, v1 = 0;
        int tri0 = -1, tri1 = -1;
        const bool hit0 = grid.firstHit(ray, positions, indexes, t0, u0, v0, tri0);
        const bool hit1 = copied->firstHit(ray, positions, indexes, t1, u1, v1, tri1);
        ASSERT_EQ(hit0, hit1) << "ray " << k;
        if (hit0) {
            EXPECT_EQ(tri0, tri1) << "ray " << k;
            EXPECT_EQ(t0, t1) << "ray " << k;
        }
    }
    delete copied;
}

TEST(GridTriangleHierarchy, EmptyMeshMisses) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    GridTriangleHierarchy grid;
    grid.build(positions, indexes);
    double t = 0, u = 0, v = 0;
    int triangle = -1;
    EXPECT_FALSE(grid.firstHit(Ray(Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -1)),
        positions, indexes, t, u, v, triangle));
    EXPECT_EQ(0, grid.getNodeCount());
}

TEST(TriangleMeshAccelerationMethod, NamesRoundTrip) {
    for (int code = 0; code < TriangleMeshAccelerationMethodUtil::COUNT; code++) {
        const TriangleMeshAccelerationMethod method =
            TriangleMeshAccelerationMethodUtil::fromCode(code);
        TriangleMeshAccelerationMethod parsed = TriangleMeshAccelerationMethod::BRUTE_FORCE;
        EXPECT_TRUE(TriangleMeshAccelerationMethodUtil::fromName(
            TriangleMeshAccelerationMethodUtil::name(method), parsed));
        EXPECT_TRUE(parsed == method);
    }
    TriangleMeshAccelerationMethod unused = TriangleMeshAccelerationMethod::BRUTE_FORCE;
    EXPECT_FALSE(TriangleMeshAccelerationMethodUtil::fromName("octree", unused));
    EXPECT_TRUE(TriangleAccelerationStructure::create(
        TriangleMeshAccelerationMethod::BRUTE_FORCE) == 0);
    TriangleMeshAccelerationMethod voxel = TriangleMeshAccelerationMethod::BRUTE_FORCE;
    EXPECT_TRUE(TriangleMeshAccelerationMethodUtil::fromName("voxel", voxel));
    TriangleAccelerationStructure *grid = TriangleAccelerationStructure::create(voxel);
    ASSERT_TRUE(grid != 0);
    EXPECT_TRUE(grid->getMethod() == TriangleMeshAccelerationMethod::VOXEL_GRID);
    delete grid;
}

TEST(TriangleMesh, AccelerationStructureIsPreparedCopiedAndReleased) {
    TriangleMesh mesh;
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildGrid(10, positions, indexes);
    mesh.getVertexPositions() = positions;
    mesh.getTriangleIndexes() = indexes;
    EXPECT_TRUE(mesh.getAccelerationStructure() == 0);

    const Ray ray(Vector3Dd(0.31, -0.27, 2), Vector3Dd(0, 0, -1));
    RayHit bruteHit;
    ASSERT_TRUE(mesh.doIntersectionFirstHit(ray, &bruteHit));

    mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod::WALD2007_BINNED_SAH);
    const TriangleAccelerationStructure *built = mesh.getAccelerationStructure();
    ASSERT_TRUE(built != 0);
    mesh.prepareAccelerationStructure(TriangleMeshAccelerationMethod::WALD2007_BINNED_SAH);
    EXPECT_TRUE(mesh.getAccelerationStructure() == built);  // not rebuilt

    double prepared[6];
    ASSERT_TRUE(mesh.getPreparedMinMax(prepared));
    double* walked = mesh.getMinMax();
    for (int i = 0; i < 6; i++) EXPECT_EQ(walked[i], prepared[i]) << i;
    delete [] walked;

    RayHit acceleratedHit;
    ASSERT_TRUE(mesh.doIntersectionFirstHit(ray, &acceleratedHit));
    EXPECT_EQ(bruteHit.point, acceleratedHit.point);
    EXPECT_EQ(bruteHit.normal, acceleratedHit.normal);

    TriangleMesh copy(mesh);
    ASSERT_TRUE(copy.getAccelerationStructure() != 0);
    EXPECT_TRUE(copy.getAccelerationStructure() != built);  // deep copy

    mesh.initTriangleArrays(0);  // a mutator releases it
    EXPECT_TRUE(mesh.getAccelerationStructure() == 0);
    EXPECT_FALSE(mesh.getPreparedMinMax(prepared));
    copy.prepareAccelerationStructure(TriangleMeshAccelerationMethod::BRUTE_FORCE);
    EXPECT_TRUE(copy.getAccelerationStructure() == 0);
}

// Many small meshes, as an OBJ file split in groups: the top level hierarchy
// must give the hit of the linear loop over the meshes (mesh, triangle,
// point, normal), including meshes sharing an edge.
TEST(TriangleMeshGroup, MeshHierarchyHitsMatchLinearLoop) {
    TriangleMeshGroup linear;
    TriangleMeshGroup accelerated;
    for (int m = 0; m < 60; m++) {
        java::ArrayList<double> positions;
        java::ArrayList<int> indexes;
        if (m % 3 == 0) {
            buildGrid(4, positions, indexes);
            for (long int i = 0; i < positions.size(); i += 3) {
                positions[i] = positions[i] * 0.2 + (m % 6) * 0.3 - 0.9;
                positions[i + 1] = positions[i + 1] * 0.2 + (m / 6) * 0.2 - 0.9;
            }
        }
        else {
            buildSoup(40, positions, indexes);
        }
        TriangleMesh mesh;
        mesh.getVertexPositions() = positions;
        mesh.getTriangleIndexes() = indexes;
        linear.addMesh(mesh);
        accelerated.addMesh(mesh);
    }
    accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod::WALD2007_BINNED_SAH);
    ASSERT_TRUE(accelerated.getMeshHierarchy() != 0);
    ASSERT_TRUE(linear.getMeshHierarchy() == 0);
    double preparedBounds[6];
    ASSERT_TRUE(accelerated.getPreparedMinMax(preparedBounds));
    double* walked = accelerated.getMinMax();
    for (int i = 0; i < 6; i++) EXPECT_EQ(walked[i], preparedBounds[i]) << i;
    delete [] walked;

    TriangleAccelerationTestRandom random(5);
    int hits = 0;
    for (int k = 0; k < 3000; k++) {
        const Ray ray = randomRay(random, k);
        RayHit hit0;
        RayHit hit1;
        const bool found0 = linear.doIntersectionFirstHit(ray, &hit0);
        int* info0 = linear.doIntersectionInformation();
        const bool found1 = accelerated.doIntersectionFirstHit(ray, &hit1);
        int* info1 = accelerated.doIntersectionInformation();
        ASSERT_EQ(found0, found1) << "ray " << k;
        EXPECT_EQ(info0[0], info1[0]) << "ray " << k;
        EXPECT_EQ(info0[1], info1[1]) << "ray " << k;
        delete [] info0;
        delete [] info1;
        if (found0) {
            hits++;
            EXPECT_EQ(hit0.point, hit1.point) << "ray " << k;
            EXPECT_EQ(hit0.normal, hit1.normal) << "ray " << k;
        }
    }
    EXPECT_GT(hits, 300);

    accelerated.addMesh(TriangleMesh());  // drops the top level
    EXPECT_TRUE(accelerated.getMeshHierarchy() == 0);
    accelerated.prepareAccelerationStructures(TriangleMeshAccelerationMethod::BRUTE_FORCE);
    EXPECT_TRUE(accelerated.getMeshHierarchy() == 0);
}

// Leaf size parameter: leaves never exceed twice the requested size (they
// may keep up to that when a split is not cheaper), and size 1 gives one
// primitive per leaf on boxes that never share a centroid.
TEST(BinnedSahHierarchy, MaximumLeafSizeIsHonored) {
    java::ArrayList<double> boxes;
    TriangleAccelerationTestRandom random(5);
    for (int i = 0; i < 500; i++) {
        const double x = random.next() * 10, y = random.next() * 10, z = random.next() * 10;
        boxes.add(x); boxes.add(y); boxes.add(z);
        boxes.add(x + 0.1); boxes.add(y + 0.1); boxes.add(z + 0.1);
    }
    for (int leafSize = 1; leafSize <= 8; leafSize *= 2) {
        BinnedSahHierarchy hierarchy;
        hierarchy.build(boxes, leafSize);
        const java::ArrayList<BinnedSahHierarchyNode> &nodes = hierarchy.getNodes();
        int largest = 0;
        for (long int i = 0; i < nodes.size(); i++) {
            if (nodes[i].count > largest) largest = nodes[i].count;
        }
        EXPECT_LE(largest, 2 * leafSize) << leafSize;
        if (leafSize == 1) {
            EXPECT_EQ(1, largest);
            EXPECT_EQ(2L * 500 - 1, hierarchy.getNodeCount());
        }
    }
    BinnedSahHierarchy byDefault;
    BinnedSahHierarchy explicitDefault;
    byDefault.build(boxes);
    explicitDefault.build(boxes, BinnedSahHierarchy::DEFAULT_MAXIMUM_LEAF_SIZE);
    EXPECT_EQ(byDefault.getNodeCount(), explicitDefault.getNodeCount());
}

static Matrix4x4d instanceTransform(int k)
{
    const Matrix4x4d identity;
    const Matrix4x4d rotation = identity.axisRotation(0.3 + 0.7 * k, 1.0, 0.5 * k, 0.25);
    const Matrix4x4d scale = identity.scale(0.2 + 0.05 * (k % 3), 0.25, 0.15 + 0.05 * (k % 2));
    const Matrix4x4d translation = identity.translation(
        (k % 5) * 0.6 - 1.2, ((k / 5) % 5) * 0.6 - 1.2, (k % 3) * 0.4 - 0.4);
    return translation.multiply(rotation.multiply(scale));
}

// Closest hit over transformed copies of meshes: the visitor of the
// instance hierarchy tests one mesh hierarchy in object space.
class _InstanceTestClosestHit : public BinnedSahInstanceVisitor {
  public:
    const java::ArrayList<BinnedSahTriangleHierarchy *> &meshes;
    const java::ArrayList<int> &meshOfInstance;
    const java::ArrayList<java::ArrayList<double> > &positions;
    const java::ArrayList<java::ArrayList<int> > &indexes;
    int bestInstance;
    int bestTriangle;
    _InstanceTestClosestHit(const java::ArrayList<BinnedSahTriangleHierarchy *> &inMeshes,
        const java::ArrayList<int> &inMeshOfInstance,
        const java::ArrayList<java::ArrayList<double> > &inPositions,
        const java::ArrayList<java::ArrayList<int> > &inIndexes)
        : meshes(inMeshes), meshOfInstance(inMeshOfInstance), positions(inPositions),
          indexes(inIndexes), bestInstance(-1), bestTriangle(-1) {}
    void visit(int instance, const Ray &objectRay, double &inOutBestT)
    {
        const int m = meshOfInstance[instance];
        double t, u, v;
        int triangle;
        if (!meshes[m]->firstHit(objectRay, positions[m], indexes[m], t, u, v, triangle)) return;
        if (t < inOutBestT || (t == inOutBestT && instance < bestInstance)) {
            inOutBestT = t;
            bestInstance = instance;
            bestTriangle = triangle;
        }
    }
};

TEST(BinnedSahInstanceHierarchy, TransformedInstancesMatchLinearLoop) {
    java::ArrayList<java::ArrayList<double> > positions;
    java::ArrayList<java::ArrayList<int> > indexes;
    java::ArrayList<BinnedSahTriangleHierarchy *> meshes;
    for (int m = 0; m < 3; m++) {
        java::ArrayList<double> p;
        java::ArrayList<int> i;
        if (m == 1) buildGrid(6, p, i); else buildSoup(60 + 40 * m, p, i);
        positions.add(p);
        indexes.add(i);
    }
    // Built after the arrays stop moving (the hierarchies keep no pointers,
    // the arrays are passed again on each query).
    for (int m = 0; m < 3; m++) {
        BinnedSahTriangleHierarchy *h = new BinnedSahTriangleHierarchy();
        h->build(positions[m], indexes[m]);
        meshes.add(h);
    }
    java::ArrayList<double> objectBoxes;
    java::ArrayList<Matrix4x4d> transforms;
    java::ArrayList<int> meshOfInstance;
    for (int k = 0; k < 40; k++) {
        const int m = k % 3;
        double bounds[6];
        meshes[m]->getVertexBounds(bounds);
        for (int c = 0; c < 6; c++) objectBoxes.add(bounds[c]);
        transforms.add(instanceTransform(k));
        meshOfInstance.add(m);
    }
    // One empty instance: never reached.
    for (int c = 0; c < 3; c++) objectBoxes.add(1.0);
    for (int c = 0; c < 3; c++) objectBoxes.add(-1.0);
    transforms.add(Matrix4x4d());
    meshOfInstance.add(0);

    BinnedSahInstanceHierarchy instances;
    instances.build(objectBoxes, transforms);
    ASSERT_EQ(41, instances.getInstanceCount());
    EXPECT_LE(instances.getHierarchy().getDepth(), BinnedSahHierarchy::MAXIMUM_DEPTH);

    TriangleAccelerationTestRandom random(23);
    int hits = 0;
    for (int k = 0; k < 2000; k++) {
        const Ray ray = randomRay(random, k);
        _InstanceTestClosestHit linear(meshes, meshOfInstance, positions, indexes);
        double linearT = 1e308;
        for (int i = 0; i < 40; i++) {
            linear.visit(i, instances.toObjectSpace(i, ray), linearT);
        }
        _InstanceTestClosestHit accelerated(meshes, meshOfInstance, positions, indexes);
        double acceleratedT = 1e308;
        instances.traverseClosest(ray, accelerated, acceleratedT);
        ASSERT_EQ(linear.bestInstance, accelerated.bestInstance) << "ray " << k;
        EXPECT_EQ(linear.bestTriangle, accelerated.bestTriangle) << "ray " << k;
        EXPECT_EQ(linearT, acceleratedT) << "ray " << k;
        if (linear.bestInstance >= 0) hits++;
    }
    EXPECT_GT(hits, 200);
    for (long int m = 0; m < meshes.size(); m++) delete meshes[m];
}

TEST(BinnedSahInstanceHierarchy, ObjectRayKeepsWorldParameter) {
    java::ArrayList<double> objectBoxes;
    for (int c = 0; c < 3; c++) objectBoxes.add(-1.0);
    for (int c = 0; c < 3; c++) objectBoxes.add(1.0);
    java::ArrayList<Matrix4x4d> transforms;
    transforms.add(instanceTransform(7));
    BinnedSahInstanceHierarchy instances;
    instances.build(objectBoxes, transforms);
    const Ray world(Vector3Dd(0.3, -2, 0.7), Vector3Dd(0.1, 1, -0.2));
    const Ray object = instances.toObjectSpace(0, world);
    // The point at t in object space, taken back, is the world point at t.
    const double t = 1.7;
    const Vector3Dd objectPoint = object.getOrigin().add(object.getDirection().multiply(t));
    const Vector3Dd back = BinnedSahInstanceHierarchy::transformPoint(
        instances.getObjectToWorld(0), objectPoint);
    const Vector3Dd expected = world.getOrigin().add(world.getDirection().multiply(t));
    EXPECT_NEAR(expected.x(), back.x(), 1e-12);
    EXPECT_NEAR(expected.y(), back.y(), 1e-12);
    EXPECT_NEAR(expected.z(), back.z(), 1e-12);
}

TEST(RaytraceStatistics, CountsHierarchyWork) {
    java::ArrayList<double> positions;
    java::ArrayList<int> indexes;
    buildGrid(20, positions, indexes);
    BinnedSahTriangleHierarchy hierarchy;
    hierarchy.build(positions, indexes);
    const Ray ray(Vector3Dd(0.31, -0.27, 2), Vector3Dd(0, 0, -1));
    double t, u, v;
    int triangle;

    RaytraceStatistics::setEnabled(false);
    RaytraceStatistics::reset();
    ASSERT_TRUE(hierarchy.firstHit(ray, positions, indexes, t, u, v, triangle));
    EXPECT_EQ(0, RaytraceStatistics::getPrimitiveLevelTraversals());

    RaytraceStatistics::setEnabled(true);
    ASSERT_TRUE(hierarchy.firstHit(ray, positions, indexes, t, u, v, triangle));
    EXPECT_EQ(1, RaytraceStatistics::getPrimitiveLevelTraversals());
    const long long visits = RaytraceStatistics::getPrimitiveLevelNodeVisits();
    const long long tests = RaytraceStatistics::getPrimitiveLevelPrimitiveTests();
    EXPECT_GT(visits, 0);
    EXPECT_GT(tests, 0);
    EXPECT_LT(tests, (long long)(indexes.size() / 3));
    EXPECT_LE(visits, hierarchy.getNodeCount());

    TriangleAccelerationStructure::firstHitBruteForce(ray, positions, indexes, t, u, v, triangle);
    EXPECT_EQ(2, RaytraceStatistics::getPrimitiveLevelTraversals());
    EXPECT_EQ(tests + (long long)(indexes.size() / 3),
        RaytraceStatistics::getPrimitiveLevelPrimitiveTests());
    EXPECT_EQ(visits, RaytraceStatistics::getPrimitiveLevelNodeVisits());
    RaytraceStatistics::setEnabled(false);
    RaytraceStatistics::reset();
}
