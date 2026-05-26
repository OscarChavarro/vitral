package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.gui.feedback.ProgressMonitor;

public class TriangleMeshGroupVoxelization {
    public static void doVoxelization(
        TriangleMeshGroup meshGroup, VoxelVolume vv, Matrix4x4d M, ProgressMonitor reporter)
    {
        int i;

        // Chain of responsability behavior design pattern with TriangleMesh
        for ( i = 0; i < meshGroup.getMeshes().size(); i++ ) {
            TriangleMeshVoxelization.doVoxelization(meshGroup.getMeshes().get(i), vv, M, reporter);
        }
    }
}
