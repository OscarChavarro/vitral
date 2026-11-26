#include <gtest/gtest.h>
#include <algorithm>
#include <atomic>
#include <thread>
#include <vector>
#include "vsdk/toolkit/common/dataStructures/KDTree.h"

class KDTestPoint {
  private:
    Vector3Dd position;
  public:
    KDTestPoint(double x, double y, double z): position(x, y, z) {}
    const Vector3Dd &getKDPosition() const { return position; }
};

TEST(KDTreeContract, FindsNearestPointsBeforeAndAfterBalancing)
{
    KDTree<KDTestPoint> tree(true);
    KDTestPoint points[] = {KDTestPoint(0,0,0), KDTestPoint(1,0,0), KDTestPoint(3,0,0), KDTestPoint(0,2,0)};
    for ( int i = 0; i < 4; ++i ) tree.addPoint(&points[i], 0);
    points[1] = KDTestPoint(99,0,0);
    Vector3Dd position(0.9, 0, 0);
    KDTestPoint *results[2] = {nullptr, nullptr};
    double distances[2] = {};
    for ( int iteration = 0; iteration < 2; ++iteration ) {
        ASSERT_EQ(2, tree.query(position, 2, results, distances, 100));
        EXPECT_NEAR(0.81, distances[0], 0.00001);
        EXPECT_NEAR(0.01, distances[1], 0.00001);
        EXPECT_FLOAT_EQ(0, results[0]->getKDPosition().x());
        EXPECT_FLOAT_EQ(1, results[1]->getKDPosition().x());
        tree.balance();
    }
}

TEST(KDTreeContract, AllowsBorrowedDataOmittedDistancesAndEmptyQueries)
{
    KDTree<KDTestPoint> tree(false);
    KDTestPoint point(1,2,3);
    Vector3Dd position(1,2,3);
    KDTestPoint *results[1] = {nullptr};
    tree.addPoint(&point, 0);
    EXPECT_EQ(0, tree.query(position, 0, results));
    ASSERT_EQ(1, tree.query(position, 1, results));
    EXPECT_EQ(&point, results[0]);
    tree.balance();
    ASSERT_EQ(1, tree.query(position, 1, results));
    EXPECT_EQ(&point, results[0]);
}

TEST(KDTreeContract, ExcludesFlagsInBothTreeRepresentations)
{
    KDTree<KDTestPoint> tree;
    KDTestPoint excluded(0,0,0), included(1,0,0);
    tree.addPoint(&excluded, 0x10);
    tree.addPoint(&included, 0x20);
    KDTestPoint *results[2] = {};
    for (int i = 0; i < 2; ++i) {
        ASSERT_EQ(1, tree.query(Vector3Dd(0,0,0), 2, results, nullptr, 100, 0x10));
        EXPECT_FLOAT_EQ(1, results[0]->getKDPosition().x());
        tree.balance();
    }
}

class KDTrackedPoint {
  private:
    Vector3Dd position;
    int *liveCount;
  public:
    explicit KDTrackedPoint(int &count): position(0, 0, 0), liveCount(&count) { ++*liveCount; }
    KDTrackedPoint(const KDTrackedPoint &other): position(other.position), liveCount(other.liveCount) { ++*liveCount; }
    ~KDTrackedPoint() { --*liveCount; }
    const Vector3Dd &getKDPosition() const { return position; }
};

TEST(KDTreeContract, ConstructsAndDestroysOwnedValues)
{
    int liveCount = 0;
    {
        KDTrackedPoint point(liveCount);
        {
            KDTree<KDTrackedPoint> tree;
            tree.addPoint(&point, 0);
            EXPECT_EQ(2, liveCount);
            tree.balance();
            EXPECT_EQ(2, liveCount);
        }
        EXPECT_EQ(1, liveCount);
    }
    EXPECT_EQ(0, liveCount);
}

TEST(KDTreeContract, ConcurrentQueriesMatchBruteForceAcrossBalancedAndNewNodes)
{
    KDTree<KDTestPoint> tree;
    std::vector<Vector3Dd> positions;
    for (int i = 0; i < 80; ++i) {
        Vector3Dd position(double((i * 17) % 101), double((i * 31) % 97), double((i * 23) % 89));
        positions.push_back(position);
        KDTestPoint point(position.x(), position.y(), position.z());
        tree.addPoint(&point, i % 3 == 0 ? 0x10 : 0);
        if (i == 49) tree.balance();
    }
    std::atomic<int> failures(0);
    std::vector<std::thread> workers;
    for (int worker = 0; worker < 72; ++worker) {
        workers.emplace_back([&tree, &positions, &failures, worker]() {
            for (int iteration = 0; iteration < 30; ++iteration) {
                Vector3Dd query(double((worker * 7 + iteration) % 83), double((worker + iteration * 5) % 79), 7.5);
                const short excluded = iteration % 2 == 0 ? 0x10 : 0;
                std::vector<double> expected;
                for (int i = 0; i < static_cast<int>(positions.size()); ++i) {
                    if (excluded && i % 3 == 0) continue;
                    double dx = positions[i].x() - query.x();
                    double dy = positions[i].y() - query.y();
                    double dz = positions[i].z() - query.z();
                    double distance = dx * dx;
                    distance += dy * dy;
                    distance += dz * dz;
                    expected.push_back(distance);
                }
                std::sort(expected.begin(), expected.end());
                KDTestPoint *results[5];
                double distances[5];
                if (tree.query(query, 5, results, distances, 1e10, excluded) != 5) {
                    ++failures;
                    continue;
                }
                std::sort(distances, distances + 5);
                for (int i = 0; i < 5; ++i) {
                    if (distances[i] != expected[i]) ++failures;
                }
            }
        });
    }
    for (std::thread &worker : workers) worker.join();
    EXPECT_EQ(0, failures.load());
}

class KDTestVisitor {
  public:
    int count;
    KDTestVisitor(): count(0) {}
    void visit(KDTestPoint &) { ++count; }
};

TEST(KDTreeContract, VisitsTypedValuesAfterBalancing)
{
    KDTree<KDTestPoint> tree;
    KDTestPoint first(0, 0, 0), second(1, 0, 0);
    tree.addPoint(&first, 0);
    tree.addPoint(&second, 0);
    tree.balance();
    KDTestVisitor visitor;
    tree.iterateNodes(visitor);
    EXPECT_EQ(2, visitor.count);
}
