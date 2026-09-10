import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { ProgressMonitor } from "../../../gui/feedback/ProgressMonitor.js";
import { TriangleMeshGroup } from "../surface/TriangleMeshGroup.js";
import { VoxelVolume } from "../volume/VoxelVolume.js";
import { TriangleMeshVoxelization, type TriangleMeshVoxelizable } from "./TriangleMeshVoxelization.js";
export class TriangleMeshGroupVoxelization {
    public static doVoxelization(
        group: TriangleMeshGroup,
        volume: VoxelVolume,
        matrix: Matrix4x4d,
        reporter: ProgressMonitor | null,
    ): void {
        for (const mesh of group.getMeshes()) {
            if ("getNumTriangles" in mesh && "getTriangleIndexes" in mesh && "getVertexPositions" in mesh)
                TriangleMeshVoxelization.doVoxelization(mesh as TriangleMeshVoxelizable, volume, matrix, reporter);
        }
    }
}
