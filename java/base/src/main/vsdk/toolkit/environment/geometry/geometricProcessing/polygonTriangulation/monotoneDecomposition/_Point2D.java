package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _Point2D {
    double x;
    double y;

    _Point2D() { this(0.0, 0.0); }
    _Point2D(double x, double y) { this.x = x; this.y = y; }
    _Point2D(_Point2D other) { this.x = other.x; this.y = other.y; }

    void set(_Point2D other) { this.x = other.x; this.y = other.y; }
    void set(double x, double y) { this.x = x; this.y = y; }
}
