package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _VertexChain {
    _Point2D point = new _Point2D();
    int[] adjacentVertexIndices = new int[4];
    int[] chainNodeIndicesByAdjacency = new int[4];
    int adjacencySlotCount;
}
