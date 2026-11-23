package vsdk.toolkit.common.dataStructures;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vsdk.toolkit.common.linealAlgebra.Vector3Df;
import static org.assertj.core.api.Assertions.assertThat;

class KDTreeTest {
    private static final class Point {
        final Vector3Df position;
        Point(float x, float y, float z) { position = new Vector3Df(x, y, z); }
    }
    private static final class Adapter implements KDTreeAdapter<Point> {
        public Vector3Df getKDPosition(Point point) { return point.position; }
        public Point copy(Point point) { return new Point(point.position.x(), point.position.y(), point.position.z()); }
    }

    @Test
    void findsNearestAndFiltersBeforeAndAfterBalancing() {
        KDTree<Point> tree = new KDTree<>(new Adapter(), true);
        tree.addPoint(new Point(0, 0, 0), (short)0x10);
        tree.addPoint(new Point(1, 0, 0), (short)0x20);
        tree.addPoint(new Point(3, 0, 0), (short)0);
        Point[] results = new Point[2];
        float[] distances = new float[2];
        for (int i = 0; i < 2; ++i) {
            assertThat(tree.query(new Vector3Df(0, 0, 0), 2, results, distances, 100, (short)0x10)).isEqualTo(2);
            assertThat(distances[0]).isEqualTo(9);
            assertThat(distances[1]).isEqualTo(1);
            tree.balance();
        }
    }

    @Test
    void mixedBalancedAndNewNodesMatchBruteForce() {
        KDTree<Point> tree = new KDTree<>(new Adapter(), true);
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 80; ++i) {
            Point point = new Point((i * 17) % 101, (i * 31) % 97, (i * 23) % 89);
            points.add(point);
            tree.addPoint(point, (short)(i % 3 == 0 ? 0x10 : 0));
            if (i == 49) tree.balance();
        }
        for (int i = 0; i < 50; ++i) {
            Vector3Df query = new Vector3Df(i, (i * 5) % 79, 7.5f);
            short excluded = (short)(i % 2 == 0 ? 0x10 : 0);
            List<Float> expected = new ArrayList<>();
            for (int j = 0; j < points.size(); ++j) {
                if (excluded != 0 && j % 3 == 0) continue;
                Vector3Df position = points.get(j).position;
                float dx = position.x() - query.x(), dy = position.y() - query.y(), dz = position.z() - query.z();
                float distance = dx * dx;
                distance += dy * dy;
                distance += dz * dz;
                expected.add(distance);
            }
            Collections.sort(expected);
            Point[] results = new Point[5];
            float[] distances = new float[5];
            assertThat(tree.query(query, 5, results, distances, 1e10f, excluded)).isEqualTo(5);
            java.util.Arrays.sort(distances);
            for (int j = 0; j < 5; ++j) assertThat(distances[j]).isEqualTo(expected.get(j));
        }
    }
}
