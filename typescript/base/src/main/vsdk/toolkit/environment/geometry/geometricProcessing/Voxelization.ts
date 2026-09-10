import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { ProgressMonitor } from "../../../gui/feedback/ProgressMonitor.js";
import { Geometry } from "../Geometry.js";
import { FunctionalExplicitSurface } from "../surface/FunctionalExplicitSurface.js";
import { TriangleMesh } from "../surface/TriangleMesh.js";
import { TriangleMeshGroup } from "../surface/TriangleMeshGroup.js";
import { VoxelVolume } from "../volume/VoxelVolume.js";
import { FunctionalExplicitSurfaceVoxelization } from "./FunctionalExplicitSurfaceVoxelization.js";
import { TriangleMeshGroupVoxelization } from "./TriangleMeshGroupVoxelization.js";
import { TriangleMeshVoxelization } from "./TriangleMeshVoxelization.js";

/** Dispatches voxelization to a surface rasterizer or the generic containment path. */
export class Voxelization {
    public static doVoxelization(
        geometry: Geometry,
        volume: VoxelVolume,
        matrix: Matrix4x4d,
        reporter: ProgressMonitor | null,
    ): void {
        if (geometry instanceof TriangleMesh) {
            TriangleMeshVoxelization.doVoxelization(geometry, volume, matrix, reporter);
            return;
        }
        if (geometry instanceof TriangleMeshGroup) {
            TriangleMeshGroupVoxelization.doVoxelization(geometry, volume, matrix, reporter);
            return;
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            FunctionalExplicitSurfaceVoxelization.doVoxelization(geometry, volume, matrix, reporter);
            return;
        }
        const nx = volume.getXSize(),
            ny = volume.getYSize(),
            nz = volume.getZSize();
        const minMax = geometry.getMinMax();
        const greaterScale = Math.max(minMax[3]! - minMax[0]!, minMax[4]! - minMax[1]!, minMax[5]! - minMax[2]!);
        const nmax = Math.max(nx, ny, nz);
        reporter?.begin();
        for (let x = 0; x < nx; x++)
            for (let y = 0; y < ny; y++) {
                reporter?.update(0, nx * ny, x * ny);
                for (let z = 0; z < nz; z++) {
                    const status = geometry.doContainmentTest(
                        matrix.multiply(volume.getVoxelPosition(x, y, z)) as Vector3Dd,
                        greaterScale / nmax,
                    );
                    // Java's signed byte -1 is the same stored sample as 255.
                    if (status === Geometry.INSIDE || status === Geometry.LIMIT) volume.putVoxel(x, y, z, 255);
                }
            }
        reporter?.end();
    }
}
