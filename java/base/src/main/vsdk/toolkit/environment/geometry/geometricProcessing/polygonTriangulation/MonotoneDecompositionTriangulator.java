package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._Construct;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._Monotone;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._RandomSegmentOrder;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._SegmentTableBuilder;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

public class MonotoneDecompositionTriangulator {
    /**
    Resulting triangle of a polygon triangulation. The three fields are
    0-based indices into the input polygon vertices, taken in the order the
    contours are traversed (all loops concatenated, first loop first).
    */
    public static final class Triangle {
        public final int a;
        public final int b;
        public final int c;

        public Triangle(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private void stage1PrepareAndOrder(Polygon2D input, int[] numVertices) {
        if (input.loops.size() <= 0) {
            throw new IllegalArgumentException("Polygon input must contain at least one contour");
        }

        ArrayList<Integer> contourSizes = new ArrayList<>();
        ArrayList<Double> vertices = new ArrayList<>();
        for (_Polygon2DContour contour : input.loops) {
            int pointCount = contour.vertices.size();
            if (pointCount <= 0) continue;
            contourSizes.add(pointCount);
            for (Vertex2D vertex : contour.vertices) {
                vertices.add(vertex.x);
                vertices.add(vertex.y);
            }
        }

        if (contourSizes.isEmpty() || vertices.isEmpty()) {
            throw new IllegalArgumentException("Polygon input must contain at least one vertex");
        }

        int pointPairs = vertices.size() / 2;
        double[] vertexArray = new double[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) vertexArray[i] = vertices.get(i);

        int[] contourArray = new int[contourSizes.size()];
        for (int i = 0; i < contourSizes.size(); i++) contourArray[i] = contourSizes.get(i);

        numVertices[0] = _SegmentTableBuilder.prepareSegments(vertexArray, pointPairs, contourArray, contourArray.length);
    }

    private void stage2BootStrap(int numVertices) {
        for (int i = 1; i <= numVertices; ++i) {
            _Construct.setSegmentInserted(i, false);
        }
        _RandomSegmentOrder.generateRandomOrdering(numVertices);
    }

    private void stage3IncrementalBatchedInsertion(int numVertices) {
        _Construct.constructTrapezoids(numVertices);
    }

    private int stage4FinalizeAndExtractTriangles(int numVertices, List<Triangle> out) {
        int[][] op = new int[_Construct.SEGMENT_SIZE][3];
        int nmonpoly = _Monotone.monotonateTrapezoids(numVertices);
        int ntriangles = _Monotone.triangulateMonotonePolygons(numVertices, nmonpoly, op);

        out.clear();
        for (int i = 0; i < ntriangles; ++i) {
            // The Seidel decomposition numbers segments/vertices from 1 to
            // numVertices internally (see _SegmentTableBuilder.prepareSegments).
            // The public Triangle indices must reference the caller's 0-based
            // Polygon2D vertex ordering, so the internal 1-based vertex numbers
            // are normalized back to 0-based here.
            out.add(new Triangle(op[i][0] - 1, op[i][1] - 1, op[i][2] - 1));
        }

        return ntriangles;
    }

    public int triangulate(Polygon2D input, List<Triangle> triangles) {
        int[] numVertices = new int[] {0};
        stage1PrepareAndOrder(input, numVertices);
        stage2BootStrap(numVertices[0]);
        stage3IncrementalBatchedInsertion(numVertices[0]);
        return stage4FinalizeAndExtractTriangles(numVertices[0], triangles);
    }
}
