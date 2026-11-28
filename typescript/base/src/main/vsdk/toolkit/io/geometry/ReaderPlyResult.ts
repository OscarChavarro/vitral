import { ArrayList } from "../../../../java/util/ArrayList.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";

export class ReaderPlyResult {
    public triangleMesh: TriangleMesh | null = null;
    public pointCloud = new ArrayList<Vector3Dd>();
}
