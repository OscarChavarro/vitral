package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Selected leaf boundary events ordered by ray parameter, then leaf order.
This is a list of visible surface events, not a solid classification.
*/
public class CsgRaySurfaceEvents {
    private final CsgRayLeafBatch leaves;
    private final ArrayList<CsgRayEventReference> events;
    private final CsgVersionStamp versions;

    public CsgRaySurfaceEvents() { leaves = new CsgRayLeafBatch(); events = new ArrayList<CsgRayEventReference>(8); versions = new CsgVersionStamp(); }

    public CsgRaySurfaceEvents(CsgRaySurfaceEvents other)
    { leaves = new CsgRayLeafBatch(other.leaves); events = new ArrayList<CsgRayEventReference>(other.events); versions = new CsgVersionStamp(other.versions); }

    public CsgRaySurfaceEvents assign(CsgRaySurfaceEvents other)
    {
        if (this!=other) {
            leaves.assign(other.leaves); versions.assign(other.versions); events.clear();
            for (int i=0;i<other.events.size();++i) events.add(other.events.get(i));
        }
        return this;
    }

    public void clear() { leaves.clear(); events.clear(); versions.clear(); }
    public CsgVersionStamp getVersions() { return versions; }
    public CsgRayLeafBatch getLeaves() { return leaves; }
    public long size() { return events.size(); }

    public CsgRayEventReference get(long index)
    { return index>=0 && index<events.size() ? events.get((int)index) : null; }

    /** Parameter of event index, or NaN when out of range. */
    public double getT(long index)
    {
        CsgRayEventReference reference=get(index);
        if (reference == null) return Double.NaN;
        CsgRayLeaf leaf=leaves.get(reference.leafIndex);
        if (leaf == null || reference.boundaryIndex<0 ||
            reference.boundaryIndex>=leaf.getClassification().boundaries.size()) return Double.NaN;
        return leaf.getClassification().boundaries.get((int)reference.boundaryIndex).t;
    }

    /** Inserts keeping the (t, leaf, boundary) order. */
    public boolean add(CsgRayEventReference reference)
    {
        CsgRayLeaf leaf=leaves.get(reference.leafIndex);
        if (leaf == null || reference.boundaryIndex<0 ||
            reference.boundaryIndex>=leaf.getClassification().boundaries.size()) return false;
        final double t=leaf.getClassification().boundaries.get((int)reference.boundaryIndex).t;
        int position=events.size();
        while (position>0) {
            final CsgRayEventReference before=events.get(position-1);
            final double beforeT=getT(position-1);
            if (beforeT<t || (beforeT==t && (before.leafIndex<reference.leafIndex ||
                (before.leafIndex==reference.leafIndex && before.boundaryIndex<reference.boundaryIndex)))) break;
            --position;
        }
        if (!events.add(reference)) return false;
        for (int i=events.size()-1;i>position;--i) events.set(i,events.get(i-1));
        events.set(position,reference);
        return true;
    }
}
