package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;

/**
Two-level hierarchy of axis aligned boxes over a set of primitives, built once
before tracing. The primitives are sorted by the centroid coordinate along the
axis of widest centroid spread and cut into about the square root of their
count bins (at most 64) of consecutive primitives; each bin keeps the box that
encloses its members.

The structure is a set of flat, index-linked arrays without virtual dispatch,
in the form of a bottom-level acceleration structure over procedural boxes
(Vulkan `VK_GEOMETRY_TYPE_AABBS_KHR`): a member is identified by the caller's
primitive id, and its box is copied into the structure in bin order.

A query lists the ids of the members whose own box passes the forward slab
test, in bin order and member order inside a bin; it never omits a primitive
whose box the ray crosses.
*/
public class AabbBinHierarchy {
    private static final int MAXIMUM_BIN_COUNT = 64;

    private final ArrayList<AxisAlignedBoundingBox> binBounds = new ArrayList<>();
    private final ArrayList<Integer> binMemberStart = new ArrayList<>();
    private final ArrayList<Integer> binMemberCount = new ArrayList<>();
    private final ArrayList<Integer> memberIds = new ArrayList<>();
    private final ArrayList<AxisAlignedBoundingBox> memberBounds = new ArrayList<>();

    public AabbBinHierarchy()
    {
    }

    /** Builds the hierarchy over `boxes`, whose primitive ids are `ids`
    (same size). */
    public void build(ArrayList<AxisAlignedBoundingBox> boxes, ArrayList<Integer> ids)
    {
        binBounds.clear();
        binMemberStart.clear();
        binMemberCount.clear();
        memberIds.clear();
        memberBounds.clear();
        final long total = boxes.size();
        if (total == 0) {
            return;
        }

        Vector3Dd lo = new Vector3Dd(1e30, 1e30, 1e30);
        Vector3Dd hi = new Vector3Dd(-1e30, -1e30, -1e30);
        for (int i = 0; i < total; i++) {
            final Vector3Dd c = boxes.get(i).centroid();
            lo = new Vector3Dd(fmin(lo.x(), c.x()), fmin(lo.y(), c.y()), fmin(lo.z(), c.z()));
            hi = new Vector3Dd(fmax(hi.x(), c.x()), fmax(hi.y(), c.y()), fmax(hi.z(), c.z()));
        }
        final double spreadX = hi.x() - lo.x();
        final double spreadY = hi.y() - lo.y();
        final double spreadZ = hi.z() - lo.z();
        int axis = 0;
        if (spreadY > spreadX && spreadY >= spreadZ) {
            axis = 1;
        } else if (spreadZ > spreadX && spreadZ > spreadY) {
            axis = 2;
        }

        // Stable insertion sort of the primitives by centroid coordinate.
        ArrayList<Double> keys = new ArrayList<>((int)total);
        ArrayList<Integer> order = new ArrayList<>((int)total);
        for (int i = 0; i < total; i++) {
            final Vector3Dd c = boxes.get(i).centroid();
            final double key = axis == 0 ? c.x() : (axis == 1 ? c.y() : c.z());
            int j = i - 1;
            keys.add(key);
            order.add(i);
            while (j >= 0 && keys.get(j) > key) {
                keys.set(j + 1, keys.get(j));
                order.set(j + 1, order.get(j));
                j--;
            }
            keys.set(j + 1, key);
            order.set(j + 1, i);
        }

        int binCount = (int)Math.sqrt((double)total);
        if (binCount < 1) {
            binCount = 1;
        }
        if (binCount > MAXIMUM_BIN_COUNT) {
            binCount = MAXIMUM_BIN_COUNT;
        }
        final int base = (int)total / binCount;
        final int remainder = (int)total % binCount;
        memberIds.ensureCapacity((int)total);
        memberBounds.ensureCapacity((int)total);
        int cursor = 0;
        for (int b = 0; b < binCount; b++) {
            final int count = base + (b < remainder ? 1 : 0);
            AxisAlignedBoundingBox aggregate = AxisAlignedBoundingBox.empty();
            final int start = memberIds.size();
            for (int k = 0; k < count; k++) {
                final int primitive = order.get(cursor++);
                memberIds.add(ids.get(primitive));
                memberBounds.add(boxes.get(primitive));
                aggregate = aggregate.enclosing(boxes.get(primitive));
            }
            binBounds.add(aggregate);
            binMemberStart.add(start);
            binMemberCount.add(count);
        }
    }

    /** C `fmin`: a NaN argument yields the other one. */
    private static double fmin(double a, double b)
    {
        if (Double.isNaN(a)) return b;
        if (Double.isNaN(b)) return a;
        return Math.min(a, b);
    }

    /** C `fmax`: a NaN argument yields the other one. */
    private static double fmax(double a, double b)
    {
        if (Double.isNaN(a)) return b;
        if (Double.isNaN(b)) return a;
        return Math.max(a, b);
    }

    public long getBinCount()
    {
        return binBounds.size();
    }

    public long getMemberCount()
    {
        return memberIds.size();
    }

    public AxisAlignedBoundingBox getBinBounds(long bin)
    {
        return binBounds.get((int)bin);
    }

    /** Writes into `outIds` the ids of the members whose box passes the
    forward slab test and returns their count, or -1 when more than
    `capacity` members pass. */
    public int gatherCandidates(Ray ray, RaySlabReciprocals reciprocals,
        int[] outIds, int capacity)
    {
        int count = 0;
        for (int b = 0; b < binBounds.size(); b++) {
            if (!binBounds.get(b).intersectsRayForward(ray, reciprocals)) {
                continue;
            }
            final int start = binMemberStart.get(b);
            final int end = start + binMemberCount.get(b);
            for (int m = start; m < end; m++) {
                if (!memberBounds.get(m).intersectsRayForward(ray, reciprocals)) {
                    continue;
                }
                if (count >= capacity) {
                    return -1;
                }
                outIds[count++] = memberIds.get(m);
            }
        }
        return count;
    }
}
