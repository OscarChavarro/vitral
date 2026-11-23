#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "vsdk/toolkit/common/dataStructures/KDTree.h"

class KDTestPoint {
  private:
    Vector3Df position;
  public:
    KDTestPoint(float x, float y, float z): position(x, y, z) {}
    const Vector3Df &getKDPosition() const { return position; }
};
class KDTestVisitor : public KDTreeVisitor<KDTestPoint> {
  public:
    int count;
    KDTestVisitor(): count(0) {}
    void visit(KDTestPoint &) { ++count; }
};

TEST(KDTreeContract, FindsNearestAndFiltersBeforeAndAfterBalancing) {
    KDTree<KDTestPoint> tree;
    KDTestPoint a(0, 0, 0), b(1, 0, 0), c(3, 0, 0);
    tree.addPoint(&a, 0x10);
    tree.addPoint(&b, 0x20);
    tree.addPoint(&c, 0);
    KDTestPoint *results[2] = {NULL, NULL};
    float distances[2] = {0, 0};
    for (int i = 0; i < 2; ++i) {
        EXPECT_EQ(2, tree.query(Vector3Df(0, 0, 0), 2, results, distances, 100, 0x10));
        EXPECT_FLOAT_EQ(9, distances[0]);
        EXPECT_FLOAT_EQ(1, distances[1]);
        tree.balance();
    }
    KDTestVisitor visitor;
    tree.iterateNodes(visitor);
    EXPECT_EQ(3, visitor.count);
}
VITRAL_TEST_MAIN()
