package vsdk.toolkit.environment.geometry.geometricProcessing;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.gui.feedback.ProgressMonitor;

public class Voxelization {
    /**
    This method implements a general voxelization strategy based on
    containment test. Note that in the case of multiple fragment geometries
    (i.e. meshes and polylines) this method is usually inefficient, and
    for that cases voxelization should be explicit, and overload this method.
    Current method is usually well behaved for basic solid models.
    Note that `reporter` can be null.
    @param geometry
    @param vv
    @param M
    @param reporter
    */
    public static void doVoxelization(
        Geometry geometry,
        VoxelVolume vv,
        Matrix4x4d M,
        ProgressMonitor reporter)
    {
        if ( geometry instanceof TriangleMesh ) {
            TriangleMeshVoxelization.doVoxelization(
                (TriangleMesh)geometry, vv, M, reporter);
            return;
        }
        if ( geometry instanceof TriangleMeshGroup ) {
            TriangleMeshGroupVoxelization.doVoxelization(
                (TriangleMeshGroup)geometry, vv, M, reporter);
            return;
        }
        if ( geometry instanceof FunctionalExplicitSurface ) {
            FunctionalExplicitSurfaceVoxelization.doVoxelization(
                (FunctionalExplicitSurface)geometry, vv, M, reporter);
            return;
        }

        int nx = vv.getXSize();
        int ny = vv.getYSize();
        int nz = vv.getZSize();
        int nmax;
        double minmax[] = geometry.getMinMax();
        double greaterScale, sx, sy, sz;

        sx = minmax[3]-minmax[0];
        sy = minmax[4]-minmax[1];
        sz = minmax[5]-minmax[2];
        greaterScale = sx;
        if ( sy > greaterScale ) {
            greaterScale = sy;
        }
        if ( sz > greaterScale ) {
            greaterScale = sz;
        }

        nmax = nx;
        if ( ny > nmax ) nmax = ny;
        if ( nz > nmax ) nmax = nz;
        int containmentStatus;
        int x, y, z;
        Vector3Dd p;
        Vector3Dd transformedP;

        if ( reporter != null ) {
            reporter.begin();
        }
        for ( x = 0; x < nx; x++ ) {
            for ( y = 0; y < ny; y++ ) {
                if ( reporter != null ) {
                    reporter.update(0, nx*ny, x*ny);
                }
                for ( z = 0; z < nz; z++ ) {
                    p = vv.getVoxelPosition(x, y, z);
                    transformedP = M.multiply(p);
                    containmentStatus = geometry.doContainmentTest(
                            transformedP, (1/((double)nmax) * greaterScale));
                    if ( containmentStatus == Geometry.INSIDE ||
                         containmentStatus == Geometry.LIMIT ) {
                        vv.putVoxel(x, y, z, (byte)-1);
                    }
                }
            }
        }
        if ( reporter != null ) {
            reporter.end();
        }
    }
}
