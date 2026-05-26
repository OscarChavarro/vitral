package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.gui.feedback.ProgressMonitor;

public class TriangleMeshVoxelization {
    /**
    Current method follows the voxelization algorithm strategy proposed
    in [DACH2000], but actual implementation only accounts for binary voxels.
    It is spected that with few changes, this algorithm manages the scalar
    (multivalued) voxel case for antialiased voxelization.
    */
    public static void doVoxelization(
        TriangleMesh mesh, VoxelVolume vv, Matrix4x4d M, ProgressMonitor reporter)
    {
        // The `*Geom` variables are in geometry space
        Vector3Dd p0Geom, p1Geom, p2Geom;
        // The `*Volume` variables are in voxel space
        Vector3Dd p0Volume, p1Volume, p2Volume, minpVolume, maxpVolume, pVolume;
        // Voxel volume control
        int minI, minJ, minK;
        int maxI, maxJ, maxK;
        int i, j, k;
        // Structural algorithm control variables
        int t;
        int status;
        Matrix4x4d Minv = M.inverse();
        double triangleMinmax[] = new double[6];
        double distanceTolerance;
        int[] triangleIndices = mesh.getTriangleIndexes();
        double[] vertexPositions = mesh.getVertexPositions();

        minpVolume = new Vector3Dd();
        maxpVolume = new Vector3Dd();

        p0Geom = new Vector3Dd();
        p1Geom = new Vector3Dd();
        p2Geom = new Vector3Dd();

        for ( t = 0; t < mesh.getNumTriangles(); t++ ) {
            // Process i-th triangle in mesh
            p0Geom = new Vector3Dd(vertexPositions[3*triangleIndices[3*t+0]+0], vertexPositions[3*triangleIndices[3*t+0]+1], vertexPositions[3*triangleIndices[3*t+0]+2]);
            p1Geom = new Vector3Dd(vertexPositions[3*triangleIndices[3*t+1]+0], vertexPositions[3*triangleIndices[3*t+1]+1], vertexPositions[3*triangleIndices[3*t+1]+2]);
            p2Geom = new Vector3Dd(vertexPositions[3*triangleIndices[3*t+2]+0], vertexPositions[3*triangleIndices[3*t+2]+1], vertexPositions[3*triangleIndices[3*t+2]+2]);
            // Obtain triangle in voxel coordinates
            p0Volume = Minv.multiply(p0Geom);
            p1Volume = Minv.multiply(p1Geom);
            p2Volume = Minv.multiply(p2Geom);
            // Obtain triangle minmax
            Triangle.minMax(p0Volume, p1Volume, p2Volume, triangleMinmax);
            minpVolume = new Vector3Dd(triangleMinmax[0], triangleMinmax[1], triangleMinmax[2]);
            maxpVolume = new Vector3Dd(triangleMinmax[3], triangleMinmax[4], triangleMinmax[5]);
            minI = vv.getNearestIFromX(minpVolume.x());
            minJ = vv.getNearestJFromY(minpVolume.y());
            minK = vv.getNearestKFromZ(minpVolume.z());
            maxI = vv.getNearestIFromX(maxpVolume.x());
            maxJ = vv.getNearestJFromY(maxpVolume.y());
            maxK = vv.getNearestKFromZ(maxpVolume.z());

            // Rasterize triangle in voxel space
            distanceTolerance = 2.0 / (double)vv.getXSize();
            for ( i = minI; i <= maxI; i++ ) {
                for ( j = minJ; j <= maxJ; j++ ) {
                    for ( k = minK; k <= maxK; k++ ) {
                        pVolume = vv.getVoxelPosition(i, j, k);
                        status = Triangle.containmentTest(
                            p0Volume, p1Volume, p2Volume,
                            pVolume, distanceTolerance);
                        if ( status != TriangleMesh.OUTSIDE ) {
                            vv.putVoxel(i, j, k, (byte)255);
                        }
                    }
                }
            }
        }
    }
}
