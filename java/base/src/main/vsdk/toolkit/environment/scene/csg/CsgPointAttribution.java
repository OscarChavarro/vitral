package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Complete set of certified boundary owners. Paths borrow scene identities;
the version stamp tells whether they are still current.
*/
public class CsgPointAttribution {
    private final ArrayList<CsgPointWitness> owners;
    private boolean complete;
    private final CsgVersionStamp versions;

    public CsgPointAttribution() { owners = new ArrayList<CsgPointWitness>(4); complete = false; versions = new CsgVersionStamp(); }

    public CsgPointAttribution(CsgPointAttribution other)
    {
        owners = new ArrayList<CsgPointWitness>(4);
        for (int i=0;i<other.owners.size();++i) owners.add(new CsgPointWitness(other.owners.get(i)));
        complete = other.complete;
        versions = new CsgVersionStamp(other.versions);
    }

    /** cpp copy assignment. */
    public CsgPointAttribution assign(CsgPointAttribution other)
    {
        if (this!=other) {
            owners.clear();
            for (int i=0;i<other.owners.size();++i) owners.add(new CsgPointWitness(other.owners.get(i)));
            complete=other.complete; versions.assign(other.versions);
        }
        return this;
    }

    public void clear() { owners.clear(); complete=false; versions.clear(); }
    public void begin() { owners.clear(); complete=true; versions.clear(); }

    public boolean add(CsgPointWitness owner)
    { if (!complete || !owner.isAvailable() || !owners.add(new CsgPointWitness(owner))) { clear(); return false; } return true; }

    public boolean append(CsgPointAttribution other)
    {
        if (!complete || !other.isComplete()) { clear(); return false; }
        for (int i=0;i<other.owners.size();++i) if (!owners.add(new CsgPointWitness(other.owners.get(i)))) { clear(); return false; }
        return true;
    }

    public boolean prepend(CsgOperandView operand)
    {
        if (!complete) return false;
        for (int i=0;i<owners.size();++i) if (!owners.get(i).prepend(operand)) { clear(); return false; }
        return true;
    }

    public void reverseOrientation()
    { if (complete) for (int i=0;i<owners.size();++i) owners.get(i).reverseOrientation(); }

    public boolean isComplete() { return complete && owners.size()>0; }
    public long getCount() { return isComplete() ? owners.size() : 0; }

    public CsgPointWitness getOwner(long index)
    { return isComplete() && index>=0 && index<owners.size() ? owners.get((int)index) : null; }

    public CsgVersionStamp getVersions() { return versions; }
}
