import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { ProgressMonitor } from "../../../gui/feedback/ProgressMonitor.js";
import { FunctionalExplicitSurface } from "../surface/FunctionalExplicitSurface.js";
import { VoxelVolume } from "../volume/VoxelVolume.js";
import { TriangleMeshVoxelization } from "./TriangleMeshVoxelization.js";
export class FunctionalExplicitSurfaceVoxelization {
    public static doVoxelization(
        surface: FunctionalExplicitSurface,
        volume: VoxelVolume,
        matrix: Matrix4x4d,
        reporter: ProgressMonitor | null,
    ): void {
        TriangleMeshVoxelization.doVoxelization(surface.getInternalTriangleMesh(), volume, matrix, reporter);
    }
}
