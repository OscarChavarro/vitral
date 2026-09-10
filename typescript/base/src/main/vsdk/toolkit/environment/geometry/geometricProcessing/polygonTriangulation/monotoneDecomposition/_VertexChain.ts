import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
export class _VertexChain {
    public point = new Vector2Dd();
    public adjacentVertexIndices = new Int32Array(4);
    public chainNodeIndicesByAdjacency = new Int32Array(4);
    public adjacencySlotCount = 0;
}
