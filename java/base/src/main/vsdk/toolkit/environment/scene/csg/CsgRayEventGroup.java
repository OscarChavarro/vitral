package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/** Events with exactly equal binary64 ray parameters, with stable leaf order. */
public class CsgRayEventGroup {
    private double t;
    // CsgRayEventReference values are never mutated, so they are shared.
    private final ArrayList<CsgRayEventReference> references;

    public CsgRayEventGroup() { t = 0; references = new ArrayList<CsgRayEventReference>(4); }
    public CsgRayEventGroup(double value) { t = value; references = new ArrayList<CsgRayEventReference>(4); }
    public CsgRayEventGroup(CsgRayEventGroup other) { t = other.t; references = new ArrayList<CsgRayEventReference>(other.references); }

    public CsgRayEventGroup assign(CsgRayEventGroup other)
    { if (this!=other) { t=other.t; references.clear(); references.addAll(other.references); } return this; }

    public double getT() { return t; }
    public boolean add(CsgRayEventReference reference) { return references.add(reference); }
    public long size() { return references.size(); }
    public CsgRayEventReference get(long index)
    { return index>=0 && index<references.size() ? references.get((int)index) : null; }
}
