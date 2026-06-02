package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _InsertionBatchSchedule {
    private _InsertionBatchSchedule() {}

    private static double log2Value(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    static int mathLogStarN(int n) {
        int i;
        double v;
        for (i = 0, v = (double)n; v >= 1; i++) v = log2Value(v);
        return (i - 1);
    }

    static int mathN(int n, int h) {
        int i;
        double v;
        for (i = 0, v = (double)n; i < h; i++) v = log2Value(v);
        return (int)Math.ceil((double)1.0 * n / v);
    }
}
