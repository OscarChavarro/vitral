package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

public class CsgRayEventBatch {
    private final ArrayList<CsgRayEventGroup> groups;

    public CsgRayEventBatch() { groups = new ArrayList<CsgRayEventGroup>(4); }
    public CsgRayEventBatch(CsgRayEventBatch other) { groups = new ArrayList<CsgRayEventGroup>(4); assign(other); }

    public CsgRayEventBatch assign(CsgRayEventBatch other)
    {
        if (this!=other) { groups.clear(); for (int i=0;i<other.groups.size();++i) groups.add(new CsgRayEventGroup(other.groups.get(i))); }
        return this;
    }

    public void clear() { groups.clear(); }
    public boolean add(CsgRayEventGroup group) { return groups.add(new CsgRayEventGroup(group)); }
    public long size() { return groups.size(); }
    public CsgRayEventGroup get(long index)
    { return index>=0 && index<groups.size() ? groups.get((int)index) : null; }
}
