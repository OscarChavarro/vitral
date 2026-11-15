package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Sufficient leaf facts for a definite result, interpreted with the borrowed
expression tree. Empty/unavailable means the result has no collected proof.
*/
public class CsgPointProof {
    private final ArrayList<CsgPointEvidence> evidence;
    private boolean available;
    private final CsgVersionStamp versions;

    public CsgPointProof() { evidence = new ArrayList<CsgPointEvidence>(4); available = false; versions = new CsgVersionStamp(); }

    /** cpp implicit copy constructor: the evidence values are copied. */
    public CsgPointProof(CsgPointProof other)
    {
        evidence = new ArrayList<CsgPointEvidence>(4);
        for (int i=0;i<other.evidence.size();++i) evidence.add(new CsgPointEvidence(other.evidence.get(i)));
        available = other.available;
        versions = new CsgVersionStamp(other.versions);
    }

    /** cpp implicit copy assignment. */
    public CsgPointProof assign(CsgPointProof other)
    {
        if (this != other) {
            evidence.clear();
            for (int i=0;i<other.evidence.size();++i) evidence.add(new CsgPointEvidence(other.evidence.get(i)));
            available = other.available;
            versions.assign(other.versions);
        }
        return this;
    }

    public void clear() { evidence.clear(); available=false; versions.clear(); }
    public void begin() { evidence.clear(); available=true; versions.clear(); }

    public boolean add(CsgPointEvidence value)
    { if (!available || !evidence.add(new CsgPointEvidence(value))) { clear(); return false; } return true; }

    public boolean append(CsgPointProof other)
    {
        if (!available || !other.available) { clear(); return false; }
        for (int i=0;i<other.evidence.size();++i) if (!evidence.add(new CsgPointEvidence(other.evidence.get(i)))) { clear(); return false; }
        return true;
    }

    public boolean prepend(CsgOperandView operand)
    {
        if (!available) return false;
        for (int i=0;i<evidence.size();++i)
            if (!evidence.get(i).prepend(operand)) { clear(); return false; }
        return true;
    }

    public void reverseOrientation()
    { if (available) for (int i=0;i<evidence.size();++i) evidence.get(i).reverseOrientation(); }

    public boolean isAvailable() { return available && evidence.size()>0; }
    public long getCount() { return isAvailable() ? evidence.size() : 0; }

    public CsgPointEvidence getEvidence(long index)
    { return isAvailable() && index>=0 && index<evidence.size() ? evidence.get((int)index) : null; }

    // Versions of the borrowed expression, captured by the top-level query.
    public CsgVersionStamp getVersions() { return versions; }
}
