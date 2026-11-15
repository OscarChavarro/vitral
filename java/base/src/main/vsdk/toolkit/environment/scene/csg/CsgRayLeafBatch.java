package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Borrowed leaf identities; owns snapshots of the classifications and paths.
Leaves are pooled: clear keeps them, and acquire reuses their storage, so a
batch reused across queries stops allocating once it has reached its size.
*/
public class CsgRayLeafBatch {
    private final ArrayList<CsgRayLeaf> pool;
    private long count;

    public CsgRayLeafBatch() { pool = new ArrayList<CsgRayLeaf>(4); count = 0; }
    public CsgRayLeafBatch(CsgRayLeafBatch other) { pool = new ArrayList<CsgRayLeaf>(4); count = 0; assign(other); }

    public CsgRayLeafBatch assign(CsgRayLeafBatch other)
    {
        if (this!=other) {
            clear();
            for (int i=0;i<other.count;++i) add(other.pool.get(i));
        }
        return this;
    }

    public void clear() { count=0; }

    /** Appends an empty leaf, or returns null when storage is exhausted. */
    public CsgRayLeaf acquire()
    {
        if (count==pool.size()) {
            CsgRayLeaf leaf=new CsgRayLeaf();
            if (!pool.add(leaf)) return null;
        }
        CsgRayLeaf leaf=pool.get((int)(count++));
        leaf.reset();
        return leaf;
    }

    public boolean add(CsgRayLeaf leaf)
    {
        CsgRayLeaf slot=acquire();
        if (slot == null) return false;
        slot.assign(leaf);
        return true;
    }

    public long size() { return count; }
    public CsgRayLeaf get(long index) { return index>=0 && index<count ? pool.get((int)index) : null; }
}
