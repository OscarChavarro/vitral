//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 22 2007 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.IndexedColorImage;

/**
VoxelVolume represents a voxelized paralelogram volume in memory.  Note that
this class is intended for simpler applications in which data volume fix into
main memory. Current class doesn't support any caching or data storage
optimization. When the x/y/z sizes of the voxel volume are equal,
the corresponding space covered by the volume is the cube from the point
<-1, -1, -1> to the point <1, 1, 1>. The voxels are always assume to be
square, and when dimensions are different, the largest dimension fits in to
the <-1, 1> interval (other dimensions are proportional to maintain
voxel sizes).

As current voxel volume is based in specific data samples of 8 bits per voxel,
it is not general and not well biased to processing applications. This is
just a placeholder for regions of interest in memory, as an aid to
other algorithms.

For general representation of N-dimensional images of arbitrary data sample
format, use another toolkit, like ITK or VTK.
*/
public class VoxelVolume extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20070222L;

    private ArrayList<IndexedColorImage> data;

    public VoxelVolume()
    {
        data = null;
    }

    public boolean init(int xSize, int ySize, int zSize)
    {
        int z;
        ArrayList<IndexedColorImage> localData;
        IndexedColorImage slice;

        localData = new ArrayList<IndexedColorImage>();

    for ( z = 0; z < zSize; z++ ) {
            slice = new IndexedColorImage();
        if ( !slice.init(xSize, ySize) ) {
                return false;
        }
        localData.add(slice);
    }

        data = localData;
    return true;
    }

    public void putVoxel(int x, int y, int z, byte val)
    {
        try {
            IndexedColorImage slice = data.get(z);
            slice.putPixel(x, y, val);
    }
    catch ( Exception e ) {
        //
    }
    }

    public int getVoxel(int x, int y, int z)
    {
        try {
            IndexedColorImage slice = data.get(z);
            return slice.getPixel(x, y);
    }
    catch ( Exception e ) {
        //
    }
    return 0;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    */
    public double[] getMinMax() {
        double minMax[];

        minMax = new double[6];
        minMax[0] = -1.0;
        minMax[1] = -1.0;
        minMax[2] = -1.0;
        minMax[3] = 1.0;
        minMax[4] = 1.0;
        minMax[5] = 1.0;
        return minMax;
    }

    /**
     Check the general interface contract in superclass method
     Geometry.doIntersection.

       Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
       intersecta con alguna de las mallas de triangulos. Si el rayo no intersecta
       al objeto se retorna 0, y de lo contrario se retorna la distancia desde
       el origen del rayo hasta el punto de interseccion mas cercano de todas las mallas.
     */
    public boolean doIntersection(Ray inOut_Ray) {
    return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    */
    public void
    doExtraInformation(Ray inRay, double inT,
                                   GeometryIntersectionInformation outData) {
    ;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
