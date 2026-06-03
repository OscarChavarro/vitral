package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import java.util.ArrayList;

final class _ContourData {
    final ArrayList<_IndexedVertex> vertices = new ArrayList<>();
    final ArrayList<Integer> childContours = new ArrayList<>();
    double signedArea;
    int parentContour = -1;
    int depth = -1;
}
