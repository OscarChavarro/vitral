//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;

/**
Top level of a two level acceleration structure: a `BinnedSahHierarchy`
over instances, each one an object space box (the bounds of its bottom
level, for example a `BinnedSahTriangleHierarchy`) placed in the world by
an affine object to world matrix. It is the CPU counterpart of a Vulkan top
level acceleration structure over `VkAccelerationStructureInstanceKHR`, and
the Vulkan 1.4 compute path builds its top level with it.

Build: the world box of an instance encloses the eight transformed corners
of its object box, padded with `TriangleAccelerationStructure.padBox`; an
empty object box gives an empty world box (the instance is never reached).
The world to object matrices are computed once (`Matrix4x4d.inverse`).
Leaves hold `DEFAULT_MAXIMUM_LEAF_SIZE` (1) instance unless told otherwise,
as a hardware TLAS. The tree follows `doc/flatBinaryTreeContract.md`; its
entries are instance indices.

Rebuild it when the transforms change (the cost is that of a build over the
instance count; the bottom levels are kept).

Matrices follow the vitral convention: column vectors, translation in
column 3 (`get(row, 3)`); only the upper 3x4 part is used.
*/
public class BinnedSahInstanceHierarchy {
    public static final int DEFAULT_MAXIMUM_LEAF_SIZE = 1;

    private final BinnedSahHierarchy hierarchy = new BinnedSahHierarchy();
    private final ArrayList<Matrix4x4d> objectToWorldMatrices = new ArrayList<>();
    private final ArrayList<Matrix4x4d> worldToObjectMatrices = new ArrayList<>();
    /** Six values per instance: padded world box */
    private final ArrayList<Double> worldBoxes = new ArrayList<>();

    public BinnedSahInstanceHierarchy()
    {
    }

    /** `m * (p, 1)`, upper 3x4 part */
    public static Vector3Dd transformPoint(Matrix4x4d m, Vector3Dd p)
    {
        return new Vector3Dd(
            m.get(0, 0) * p.x() + m.get(0, 1) * p.y() + m.get(0, 2) * p.z() + m.get(0, 3),
            m.get(1, 0) * p.x() + m.get(1, 1) * p.y() + m.get(1, 2) * p.z() + m.get(1, 3),
            m.get(2, 0) * p.x() + m.get(2, 1) * p.y() + m.get(2, 2) * p.z() + m.get(2, 3));
    }

    /** `m * (v, 0)`, upper 3x3 part */
    public static Vector3Dd transformVector(Matrix4x4d m, Vector3Dd v)
    {
        return new Vector3Dd(
            m.get(0, 0) * v.x() + m.get(0, 1) * v.y() + m.get(0, 2) * v.z(),
            m.get(1, 0) * v.x() + m.get(1, 1) * v.y() + m.get(1, 2) * v.z(),
            m.get(2, 0) * v.x() + m.get(2, 1) * v.y() + m.get(2, 2) * v.z());
    }

    /** Box enclosing the eight transformed corners (unpadded); an empty box
    stays empty */
    public static void transformBox(double[] objectBox, Matrix4x4d m,
        double[] outWorldBox)
    {
        for ( int a = 0; a < 3; a++ ) {
            outWorldBox[a] = 1e30;
            outWorldBox[a + 3] = -1e30;
        }
        if ( objectBox[0] > objectBox[3] || objectBox[1] > objectBox[4] ||
             objectBox[2] > objectBox[5] ) {
            return;
        }
        for ( int corner = 0; corner < 8; corner++ ) {
            final Vector3Dd p = new Vector3Dd(
                (corner & 1) != 0 ? objectBox[3] : objectBox[0],
                (corner & 2) != 0 ? objectBox[4] : objectBox[1],
                (corner & 4) != 0 ? objectBox[5] : objectBox[2]);
            final Vector3Dd w = transformPoint(m, p);
            final double[] c = {w.x(), w.y(), w.z()};
            for ( int a = 0; a < 3; a++ ) {
                outWorldBox[a] = c[a] < outWorldBox[a] ? c[a] : outWorldBox[a];
                outWorldBox[a + 3] = c[a] > outWorldBox[a + 3] ? c[a] : outWorldBox[a + 3];
            }
        }
    }

    /**
    `build(objectBoxes, objectToWorld, DEFAULT_MAXIMUM_LEAF_SIZE)` (cpp
    default argument)
    */
    public void build(ArrayList<Double> objectBoxes,
        ArrayList<Matrix4x4d> objectToWorld)
    {
        build(objectBoxes, objectToWorld, DEFAULT_MAXIMUM_LEAF_SIZE);
    }

    /**
    @param objectBoxes six values per instance (lo x, y, z, hi x, y, z) in
    object space; lo > hi on an axis marks an empty instance
    @param objectToWorld one matrix per instance
    @param maximumLeafSize see `BinnedSahHierarchy.build`
    */
    public void build(ArrayList<Double> objectBoxes,
        ArrayList<Matrix4x4d> objectToWorld, int maximumLeafSize)
    {
        int count = objectBoxes.size() / 6;
        if ( objectToWorld.size() < count ) {
            count = objectToWorld.size();
        }
        objectToWorldMatrices.clear();
        worldToObjectMatrices.clear();
        worldBoxes.clear();
        worldBoxes.ensureCapacity(6 * (count > 0 ? count : 1));
        double[] objectBox = new double[6];
        double[] world = new double[6];
        double[] lo = new double[3];
        double[] hi = new double[3];
        for ( int i = 0; i < count; i++ ) {
            final Matrix4x4d m = objectToWorld.get(i);
            // cpp stores copies of the matrices
            objectToWorldMatrices.add(new Matrix4x4d(m));
            worldToObjectMatrices.add(m.inverse());
            for ( int k = 0; k < 6; k++ ) {
                objectBox[k] = objectBoxes.get(6 * i + k);
            }
            transformBox(objectBox, m, world);
            if ( world[0] <= world[3] ) {
                // [WALD2007].3.2: the build only sees these boxes; padded so
                // rounding of the transform never hides a hit.
                for ( int a = 0; a < 3; a++ ) {
                    lo[a] = world[a];
                    hi[a] = world[a + 3];
                }
                TriangleAccelerationStructure.padBox(lo, hi);
                for ( int a = 0; a < 3; a++ ) {
                    world[a] = lo[a];
                    world[a + 3] = hi[a];
                }
            }
            for ( int k = 0; k < 6; k++ ) {
                worldBoxes.add(world[k]);
            }
        }
        hierarchy.build(worldBoxes, maximumLeafSize);
    }

    /** The world ray in the object space of an instance (see the visitor) */
    public Ray toObjectSpace(int instance, Ray worldRay)
    {
        final Matrix4x4d m = worldToObjectMatrices.get(instance);
        return Ray.fromAffine(transformPoint(m, worldRay.getOrigin()),
            transformVector(m, worldRay.getDirection()));
    }

    /**
    Closest hit traversal of the world ray: calls the visitor for every
    instance whose world box the ray enters before `inOutBestT[0]`, nearest
    first, with the ray in object space. Never misses an instance: when the
    traversal stack would overflow (not reachable, see
    `BinnedSahHierarchy.traverseClosest`) every instance is visited.
    @param inOutBestT one-element input/output holder
    */
    public void traverseClosest(Ray worldRay, BinnedSahInstanceVisitor visitor,
        double[] inOutBestT)
    {
        final _BinnedSahInstanceAdapter adapter =
            new _BinnedSahInstanceAdapter(this, worldRay, visitor);
        if ( !hierarchy.traverseClosest(worldRay, adapter, inOutBestT) ) {
            // Stack overflow: visiting every instance still gives the closest
            // hit (the visitor only lowers inOutBestT).
            for ( int i = 0; i < getInstanceCount(); i++ ) {
                adapter.visit(i, inOutBestT);
            }
        }
    }

    public int getInstanceCount()
    {
        return objectToWorldMatrices.size();
    }

    public BinnedSahHierarchy getHierarchy()
    {
        return hierarchy;
    }

    public Matrix4x4d getObjectToWorld(int instance)
    {
        return objectToWorldMatrices.get(instance);
    }

    public Matrix4x4d getWorldToObject(int instance)
    {
        return worldToObjectMatrices.get(instance);
    }

    /** Padded world box of an instance: six values */
    public void getWorldBox(int instance, double[] outBox)
    {
        for ( int k = 0; k < 6; k++ ) {
            outBox[k] = worldBoxes.get(6 * instance + k);
        }
    }
}
