package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;

/**
Combined membership of a whole expression along one ray. Boundary i of the
solid is copied from its first owner. The owners of boundary i are the leaf
events whose transition alone produces that membership change or, when none
does, every changed leaf oriented like that change; there are none only when
neither exists. References index the leaf snapshots kept here. The version
stamp records the borrowed operands as they were when the result was built.
Storage is reused across queries.
*/
public class CsgRayClassification {
    private final CsgRayLeafBatch leaves;
    private final RaySolidClassification solid;
    private final ArrayList<CsgRayEventReference> owners;
    private final ArrayList<Long> ownerStarts;
    private final CsgVersionStamp versions;

    public CsgRayClassification()
    {
        leaves = new CsgRayLeafBatch(); solid = new RaySolidClassification();
        owners = new ArrayList<CsgRayEventReference>(8); ownerStarts = new ArrayList<Long>(8); versions = new CsgVersionStamp();
    }

    public CsgRayClassification(CsgRayClassification other) { this(); assign(other); }

    public CsgRayClassification assign(CsgRayClassification other)
    {
        if (this!=other) {
            leaves.assign(other.leaves); solid.assign(other.solid); versions.assign(other.versions);
            owners.clear(); ownerStarts.clear();
            for (int i=0;i<other.owners.size();++i) owners.add(other.owners.get(i));
            for (int i=0;i<other.ownerStarts.size();++i) ownerStarts.add(other.ownerStarts.get(i));
        }
        return this;
    }

    public void clear() { leaves.clear(); solid.clear(); owners.clear(); ownerStarts.clear(); versions.clear(); }
    public CsgRayLeafBatch getLeaves() { return leaves; }
    public RaySolidClassification getSolid() { return solid; }
    public CsgVersionStamp getVersions() { return versions; }

    /** Starts the owner list of the next boundary. */
    public boolean beginOwners() { return ownerStarts.add((long)owners.size()); }

    public boolean addOwner(CsgRayEventReference reference)
    { return ownerStarts.size()>0 && owners.add(reference); }

    public long getOwnerGroupCount() { return ownerStarts.size(); }

    public long getOwnerCount(long boundaryIndex)
    {
        if (boundaryIndex<0 || boundaryIndex>=ownerStarts.size()) return 0;
        final long end=boundaryIndex+1<ownerStarts.size() ? ownerStarts.get((int)boundaryIndex+1) : owners.size();
        return end-ownerStarts.get((int)boundaryIndex);
    }

    public CsgRayEventReference getOwner(long boundaryIndex, long ownerIndex)
    {
        if (ownerIndex<0 || ownerIndex>=getOwnerCount(boundaryIndex)) return null;
        return owners.get((int)(ownerStarts.get((int)boundaryIndex)+ownerIndex));
    }
}
