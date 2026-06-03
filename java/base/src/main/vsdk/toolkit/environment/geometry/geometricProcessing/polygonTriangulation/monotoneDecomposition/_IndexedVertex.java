package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _IndexedVertex {
    final double x;
    final double y;
    final int originalIndex;

    _IndexedVertex(double x, double y, int originalIndex) {
        this.x = x;
        this.y = y;
        this.originalIndex = originalIndex;
    }
}
