//= References:                                                             =
//= [KAUF1987] Kaufman, Arie. "Efficient Algorithms for 3D Scan-Conversion  =
//=     of Parametric Curves, Surfaces, and Volumes", ACM SIGGRAPH Computer =
//=     Graphics, volume 21, number 4, July 1987.                           =
//= [AMAN1987] Amanatides, John. Woo, Andrew. "A Fast Voxel Traversal      =
//=     Algorithm for Ray Tracing", Eurographics '87, 1987.                 =
//= [SNYD1987] Snyder, John. Barr, Alan. "Ray Tracing Complex Models       =
//=     Containing Surface Tessellations", SIGGRAPH '87, p. 119-128, 1987.  =

package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.media.IndexedColorImageUncompressed;

/**
VoxelVolume represents a voxelized paralelogram volume in memory, as the
"cubic frame buffer" proposed in [KAUF1987]. Note that this class is intended
for simpler applications in which data volume fix into main memory. Current
class doesn't support any caching or data storage optimization. When the
x/y/z sizes of the voxel volume are equal, the corresponding space covered
by the volume is the cube from the point <-1, -1, -1> to the point <1, 1, 1>.
The voxels are always assume to be square, and when dimensions are different,
the largest dimension fits in to the <-1, 1> interval (other dimensions are
proportional to maintain voxel sizes).

As current voxel volume is based in specific data samples of 8 bits per voxel,
it is not general and not well biased to processing applications. This is
just a placeholder for regions of interest in memory, as an aid to
other algorithms.

For general representation of N-dimensional images of arbitrary data sample
format, use another toolkit, like ITK or VTK.
*/
public class VoxelVolume extends Solid {
    @Serial private static final long serialVersionUID = 20070222L;

    /** Default lowest value of a voxel considered part of the solid */
    public static final int DEFAULT_THRESHOLD = 127;

    /** Smallest ray direction component not considered parallel to an axis */
    private static final double PARALLEL_EPSILON = 1.0e-12;

    private ArrayList<IndexedColorImageUncompressed> data;
    private int threshold;

    public VoxelVolume()
    {
        data = null;
        threshold = DEFAULT_THRESHOLD;
    }

    public int getXSize()
    {
        if ( data == null || data.isEmpty() ) return 0;
        return data.get(0).getXSize();
    }

    public int getYSize()
    {
        if ( data == null || data.isEmpty() ) return 0;
        return data.get(0).getYSize();
    }

    public int getZSize()
    {
        if ( data == null || data.isEmpty() ) return 0;
        return data.size();
    }

    /**
    @return lowest value of a voxel considered part of the solid (by the ray
    intersection and by the renderers)
    */
    public int getThreshold()
    {
        return threshold;
    }

    /**
    @param threshold lowest value of a voxel considered part of the solid
    */
    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }

    /**
    @param x voxel index along X
    @param y voxel index along Y
    @param z voxel index along Z
    @return true if the voxel is inside the volume and its value reaches the
    threshold
    */
    public boolean isFilled(int x, int y, int z)
    {
        if ( x < 0 || y < 0 || z < 0 || x >= getXSize() ||
             y >= getYSize() || z >= getZSize() ) {
            return false;
        }
        return getVoxel(x, y, z) >= threshold;
    }

    public boolean init(int xSize, int ySize, int zSize)
    {
        int z;
        ArrayList<IndexedColorImageUncompressed> localData;
        IndexedColorImageUncompressed slice;

        localData = new ArrayList<IndexedColorImageUncompressed>();

        for ( z = 0; z < zSize; z++ ) {
            slice = new IndexedColorImageUncompressed();
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
            if ( (x < 0 || x >= getXSize()) ||
                 (y < 0 || y >= getYSize()) ||
                 (z < 0 || z >= getZSize()) ) {
                return;
            }
            IndexedColorImageUncompressed slice = data.get(z);
            slice.putPixel(x, y, val);
        }
        catch ( Exception e ) {
            //
        }
    }

    /**
    Given current voxel set geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxel set size, and cell position to a voxel; this
    methods gives the position of the voxel center in world coordinates.
    @param x
    @param y
    @param z
    @return a new Vector3Dd with point position corresponding to given indexes
    inside the matrix of voxels
    */
    public Vector3Dd getVoxelPosition(int x, int y, int z)
    {
        Vector3Dd p = new Vector3Dd();
        p = p.withX(((double)x+0.5) / ((double)getXSize())*2 - 1);
        p = p.withY(((double)y+0.5) / ((double)getYSize())*2 - 1);
        p = p.withZ(((double)z+0.5) / ((double)getZSize())*2 - 1);
        return p;
    }

    /**
    Partial coordinate convertion (X axis) for `x` voxel coordinate to
    corresponding voxel index.
    @param x
    @return a matrix index for the given coordinate
    */
    public int getNearestIFromX(double x)
    {
        return (int)(((x + 1)/2) * ((double)getXSize()) - 0.5);
    }

    /**
    Partial coordinate convertion (Y axis) for `y` voxel coordinate to
    corresponding voxel index.
    @param y
    @return a matrix index for the given coordinate
    */
    public int getNearestJFromY(double y)
    {
        return (int)(((y + 1)/2) * ((double)getYSize()) - 0.5);
    }

    /**
    Partial coordinate convertion (Z axis) for `z` voxel coordinate to
    corresponding voxel index.
    @param z
    @return a matrix index for the given coordinate
    */
    public int getNearestKFromZ(double z)
    {
        return (int)(((z + 1)/2) * ((double)getZSize()) - 0.5);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods gives the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).
    @param x
    @param y
    @param z
    @return value of voxel at specified indexed position
    */
    public int getVoxelAtPosition(double x, double y, double z)
    {
        if ( x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1 ) return 0;
        int i, j, k;

        i = (int)(((x + 1)/2) * ((double)getXSize()) - 0.5);
        j = (int)(((y + 1)/2) * ((double)getYSize()) - 0.5);
        k = (int)(((z + 1)/2) * ((double)getZSize()) - 0.5);

        return getVoxel(i, j, k);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods gives the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).
    @param p
    @return voxel value for given point in space
    */
    public int getVoxelAtPosition(Vector3Dd p)
    {
        if ( p.x() < -1 || p.x() > 1 || p.y() < -1 || p.y() > 1 || p.z() < -1 || p.z() > 1 ) return 0;
        int i, j, k;

        i = (int)(((p.x() + 1)/2) * ((double)getXSize()) - 0.5);
        j = (int)(((p.y() + 1)/2) * ((double)getYSize()) - 0.5);
        k = (int)(((p.z() + 1)/2) * ((double)getZSize()) - 0.5);

        return getVoxel(i, j, k);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods puts the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).
    @param x
    @param y
    @param z
    @param val
    */
    public void putVoxelAtPosition(double x, double y, double z, byte val)
    {
        if ( x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1 ) return;
        int i, j, k;

        i = (int)(((x + 1)/2) * ((double)getXSize()) - 0.5);
        j = (int)(((y + 1)/2) * ((double)getYSize()) - 0.5);
        k = (int)(((z + 1)/2) * ((double)getZSize()) - 0.5);

        putVoxel(i, j, k, val);
    }

    /**
    Given current voxelset geometric space (cube from <-1, -1, -1> to
    <1, 1, 1>), current voxelset size, and cell position to a voxel; this
    methods puts the voxel value with a position corresponding to coordinate
    <x, y, z> (inside voxel space cube).
    @param p
    @param val
    */
    public void putVoxelAtPosition(Vector3Dd p, byte val)
    {
        if ( p.x() < -1 || p.x() > 1 || p.y() < -1 || p.y() > 1 || p.z() < -1 || p.z() > 1 ) return;
        int i, j, k;

        i = (int)(((p.x() + 1)/2) * ((double)getXSize()) - 0.5);
        j = (int)(((p.y() + 1)/2) * ((double)getYSize()) - 0.5);
        k = (int)(((p.z() + 1)/2) * ((double)getZSize()) - 0.5);

        putVoxel(i, j, k, val);
    }

    public int getVoxel(int x, int y, int z)
    {
        try {
            IndexedColorImageUncompressed slice = data.get(z);
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
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
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
    Geometry.doIntersectionFirstHit.
    @param inOut_Ray
    @return the ray with the distance to the first filled voxel hit, or null
    if no filled voxel is hit
    */
    public Ray doIntersectionFirstHit(Ray inOut_Ray) {
        RayHit hit = new RayHit();
        if ( doIntersectionFirstHit(inOut_Ray, hit) ) {
            return hit.getRay();
        }
        return null;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit. The volume is seen as the union of the
    cubes of its filled voxels (see `isFilled`), and the ray hits the first
    face through which it enters one of them. A ray starting inside a filled
    voxel does not hit the voxels it leaves: as with the other solids, only
    the surface it enters counts.

    The voxels are visited in the order the ray crosses them, as the uniform
    grid traversal of [SNYD1987] (with the incremental formulation of
    [AMAN1987]), so the cost grows with the number of voxels crossed, not
    with the number of voxels of the volume.
    @param inRay ray in the space of the volume
    @param outHit receives the hit, or null if only the test is needed
    @return true if the ray hits a filled voxel
    */
    @Override
    public boolean doIntersectionFirstHit(Ray inRay, RayHit outHit)
    {
        VoxelFaceHit hit = traceFirstFilledVoxel(inRay);

        if ( hit == null ) {
            return false;
        }
        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() || outHit.needsAnySurfaceData() ) {
                Ray hitRay = inRay.withT(hit.t());
                outHit.setRay(hitRay);
                if ( outHit.needsAnySurfaceData() ) {
                    fillSurfaceData(hitRay, hit, outHit);
                    outHit.setRay(hitRay);
                }
            }
            else {
                outHit.setHitDistance(hit.t());
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay ray (in the space of the volume) whose first hit is described
    @param inT distance to the hit
    @param outData receives the point, normal, texture coordinates and
    tangent of the hit
    */
    public void
    doExtraInformation(Ray inRay, 
        double inT,
        RayHit outData) {
        VoxelFaceHit hit = traceFirstFilledVoxel(inRay);

        if ( hit != null ) {
            fillSurfaceData(inRay.withT(hit.t()), hit, outData);
        }
    }

    /**
    Face of a voxel entered by a ray.
    @param t distance along the ray to the face
    @param axis axis perpendicular to the face (0: x, 1: y, 2: z)
    @param side sign of the outer normal of the face along its axis
    */
    private record VoxelFaceHit(double t, int axis, int side)
    {
    }

    /**
    Finds the face of the first filled voxel entered by the ray: clips the
    ray against the cube <-1, -1, -1>-<1, 1, 1> and visits the voxels it
    crosses, as `VoxelGrid.gridIntersect` of [SNYD1987]. For each axis,
    `tNext` is the distance to the next voxel boundary along that axis and
    `tDelta` the distance between two boundaries; each step moves to the
    voxel whose boundary is nearest.
    @param ray ray in the space of the volume
    @return the face entered, or null if the ray enters no filled voxel
    */
    private VoxelFaceHit traceFirstFilledVoxel(Ray ray)
    {
        int[] size = { getXSize(), getYSize(), getZSize() };

        if ( size[0] <= 0 || size[1] <= 0 || size[2] <= 0 || ray == null ) {
            return null;
        }

        Vector3Dd o = ray.getOrigin();
        Vector3Dd d = ray.getDirection();
        double[] origin = { o.x(), o.y(), o.z() };
        double[] direction = { d.x(), d.y(), d.z() };
        double[] cellSize = new double[3];
        double tEnter = Double.NEGATIVE_INFINITY;
        double tExit = Double.POSITIVE_INFINITY;
        int enterAxis = 0;
        int i;

        //- Clip the ray against the volume cube (slabs method) ------------
        for ( i = 0; i < 3; i++ ) {
            cellSize[i] = 2.0 / size[i];
            if ( Math.abs(direction[i]) < PARALLEL_EPSILON ) {
                if ( origin[i] < -1.0 || origin[i] > 1.0 ) {
                    return null;
                }
                continue;
            }
            double t1 = (-1.0 - origin[i]) / direction[i];
            double t2 = (1.0 - origin[i]) / direction[i];
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);
            if ( tNear > tEnter ) {
                tEnter = tNear;
                enterAxis = i;
            }
            tExit = Math.min(tExit, tFar);
        }
        if ( tExit < Math.max(tEnter, 0.0) ) {
            return null;
        }
        boolean startsInside = tEnter < 0.0;
        double t = startsInside ? 0.0 : tEnter;

        //- Setup of the traversal, from the voxel where the ray starts ----
        int[] g = new int[3];
        int[] step = new int[3];
        double[] tDelta = new double[3];
        double[] tNext = new double[3];

        for ( i = 0; i < 3; i++ ) {
            double p = origin[i] + t * direction[i];
            g[i] = (int)Math.floor((p + 1.0) / cellSize[i]);
            if ( g[i] < 0 ) {
                g[i] = 0;
            }
            if ( g[i] >= size[i] ) {
                g[i] = size[i] - 1;
            }
            if ( direction[i] > PARALLEL_EPSILON ) {
                step[i] = 1;
                tDelta[i] = cellSize[i] / direction[i];
                tNext[i] = (-1.0 + (g[i] + 1) * cellSize[i] - origin[i]) / direction[i];
            }
            else if ( direction[i] < -PARALLEL_EPSILON ) {
                step[i] = -1;
                tDelta[i] = cellSize[i] / -direction[i];
                tNext[i] = (-1.0 + g[i] * cellSize[i] - origin[i]) / direction[i];
            }
            else {
                step[i] = 0;
                tDelta[i] = Double.POSITIVE_INFINITY;
                tNext[i] = Double.POSITIVE_INFINITY;
            }
        }

        boolean previousFilled = isFilled(g[0], g[1], g[2]);
        if ( previousFilled && !startsInside ) {
            return new VoxelFaceHit(t, enterAxis, direction[enterAxis] > 0 ? -1 : 1);
        }

        //- Visit the voxels crossed by the ray -----------------------------
        while ( true ) {
            int axis;
            if ( tNext[0] <= tNext[1] && tNext[0] <= tNext[2] ) {
                axis = 0;
            }
            else if ( tNext[1] <= tNext[2] ) {
                axis = 1;
            }
            else {
                axis = 2;
            }
            if ( tNext[axis] > tExit ) {
                return null;
            }
            double tCell = tNext[axis];
            g[axis] += step[axis];
            tNext[axis] += tDelta[axis];
            if ( g[axis] < 0 || g[axis] >= size[axis] ) {
                return null;
            }
            boolean filled = isFilled(g[0], g[1], g[2]);
            if ( filled && !previousFilled ) {
                return new VoxelFaceHit(tCell, axis, -step[axis]);
            }
            previousFilled = filled;
        }
    }

    /**
    Fills the surface data of a hit on a voxel face: the point, the normal of
    the face, texture coordinates spanning the volume along the two axes of
    the face, and a tangent along the first of them.
    */
    private static void fillSurfaceData(Ray hitRay, VoxelFaceHit hit,
                                        RayHit outData)
    {
        Vector3Dd p = hitRay.getOrigin().add(
            hitRay.getDirection().multiply(hit.t()));
        double[] coordinates = { p.x(), p.y(), p.z() };
        double[] normal = new double[3];
        double[] tangent = new double[3];
        int uAxis = (hit.axis() + 1) % 3;
        int vAxis = (hit.axis() + 2) % 3;

        normal[hit.axis()] = hit.side();
        tangent[uAxis] = 1.0;
        if ( outData.needsPoint() ) {
            outData.point = p;
        }
        if ( outData.needsNormal() ) {
            outData.normal = new Vector3Dd(normal[0], normal[1], normal[2]);
        }
        if ( outData.needsTextureCoordinates() ) {
            outData.u = (coordinates[uAxis] + 1.0) / 2.0;
            outData.v = (coordinates[vAxis] + 1.0) / 2.0;
        }
        if ( outData.needsTangent() ) {
            outData.tangent = new Vector3Dd(tangent[0], tangent[1], tangent[2]);
        }
    }

    /**
    Current method creates a transformation matrix that represent the
    coordinate change from voxel volume cube <-1, -1, -1>-<1, 1, 1> to
    the bounding box recieved in `minmax`.
    @param minmax
    @return a new Matrix4x4d for coordinate mapping between volume and
    world coordinates
    */
    public static Matrix4x4d
    getTransformFromVoxelFrameToMinMax(double minmax[])
    {
        Matrix4x4d M;
        Matrix4x4d S, T1, T2;
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

        S = new Matrix4x4d();
        S = S.scale(greaterScale/2, greaterScale/2, greaterScale/2);

        T1 = new Matrix4x4d();
        T1 = T1.translation(1, 1, 1);
        T2 = new Matrix4x4d();
        T2 = T2.translation(minmax[0]-(greaterScale-sx)/2,
                       minmax[1]-(greaterScale-sy)/2,
                       minmax[2]-(greaterScale-sz)/2);

        M = T2.multiply(S.multiply(T1));
        return M;
    }

    /**
    Check the general interface contract in superclass method
    Solid.doCenterOfMass
    @return new Vector3Dd containing volume center of mass
    */
    @Override
    public Vector3Dd doCenterOfMass() {
        Vector3Dd p;
        double cmx = 0;
        double cmy = 0;
        double cmz = 0;
        double mi; // Maximum mass of one voxel (linear to voxel density)
        double M = 0;  // Total mass for current voxel volume
        int x, y, z;

        for ( x = 0; x < getXSize(); x++ ) {
            for ( y = 0; y < getYSize(); y++ ) {
                for ( z = 0; z < getZSize(); z++ ) {
                    // mi goes from 0 to 1
                    mi = (double)(getVoxel(x, y, z)) / 255.0;
                    M += mi;
                    p = getVoxelPosition(x, y, z);
                    cmx += mi * p.x();
                    cmy += mi * p.y();
                    cmz += mi * p.z();
                }
            }
        }

        if ( Math.abs(M) < VSDK.EPSILON ) {
            return new Vector3Dd(0, 0, 0);
        }

        return new Vector3Dd(cmx / M, cmy / M, cmz / M);
    }

}
