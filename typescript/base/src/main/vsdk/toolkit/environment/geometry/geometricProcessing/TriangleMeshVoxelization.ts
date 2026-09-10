import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { ProgressMonitor } from "../../../gui/feedback/ProgressMonitor.js";
import { Triangle } from "../element/Triangle.js";
import { VoxelVolume } from "../volume/VoxelVolume.js";

export class TriangleMeshVoxelization {
    public static doVoxelization(
        mesh: TriangleMeshVoxelizable,
        volume: VoxelVolume,
        matrix: Matrix4x4d,
        _reporter: ProgressMonitor | null,
    ): void {
        const indices = mesh.getTriangleIndexes(),
            positions = mesh.getVertexPositions();
        if (indices === null || positions === null) return;
        const inverse = matrix.inverse(),
            bounds = new Float64Array(6);
        for (let triangle = 0; triangle < mesh.getNumTriangles(); triangle++) {
            const point = (corner: number) => {
                const index = indices[triangle * 3 + corner]! * 3;
                return inverse.multiply(
                    new Vector3Dd(positions[index]!, positions[index + 1]!, positions[index + 2]!),
                ) as Vector3Dd;
            };
            const p0 = point(0),
                p1 = point(1),
                p2 = point(2);
            Triangle.minMax(p0, p1, p2, bounds);
            const minI = volume.getNearestIFromX(bounds[0]!);
            const minJ = volume.getNearestJFromY(bounds[1]!);
            const minK = volume.getNearestKFromZ(bounds[2]!);
            const maxI = volume.getNearestIFromX(bounds[3]!);
            const maxJ = volume.getNearestJFromY(bounds[4]!);
            const maxK = volume.getNearestKFromZ(bounds[5]!);
            const tolerance = 2 / volume.getXSize();
            for (let i = minI; i <= maxI; i++)
                for (let j = minJ; j <= maxJ; j++)
                    for (let k = minK; k <= maxK; k++) {
                        if (Triangle.containmentTest(p0, p1, p2, volume.getVoxelPosition(i, j, k), tolerance) !== 0)
                            volume.putVoxel(i, j, k, 255);
                    }
        }
    }
}

export interface TriangleMeshVoxelizable {
    getNumTriangles(): number;
    getTriangleIndexes(): Int32Array | null;
    getVertexPositions(): Float64Array | null;
}
