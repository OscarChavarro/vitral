package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import vsdk.toolkit.common.linealAlgebra.Vector2Dd;

final class _VertexChain {
    Vector2Dd point = new Vector2Dd();
    int[] adjacentVertexIndices = new int[4];
    int[] chainNodeIndicesByAdjacency = new int[4];
    int adjacencySlotCount;
}
