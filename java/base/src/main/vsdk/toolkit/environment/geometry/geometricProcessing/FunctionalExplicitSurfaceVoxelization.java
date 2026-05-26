package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.gui.feedback.ProgressMonitor;

public class FunctionalExplicitSurfaceVoxelization {
    public static void doVoxelization(
        FunctionalExplicitSurface surface, VoxelVolume vv, Matrix4x4d M, ProgressMonitor reporter)
    {
        TriangleMeshVoxelization.doVoxelization(
            surface.getInternalTriangleMesh(), vv, M, reporter);
    }
}
