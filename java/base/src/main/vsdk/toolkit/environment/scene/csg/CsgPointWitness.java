package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Borrowed root-to-leaf path for a single operand proving a point result.
Reversal records difference orientation, not a certified boundary hit.
*/
public class CsgPointWitness {
    private final ArrayList<CsgOperandView> path;
    private boolean reversed;

    public CsgPointWitness() { path = new ArrayList<CsgOperandView>(4); reversed = false; }

    public CsgPointWitness(CsgPointWitness other) { path = new ArrayList<CsgOperandView>(other.path); reversed = other.reversed; }

    /** cpp copy assignment. */
    public CsgPointWitness assign(CsgPointWitness other)
    {
        // Reuses the existing storage, see RayInstancePath.
        if (this!=other) {
            path.clear();
            for (int i=0;i<other.path.size();++i) path.add(other.path.get(i));
            reversed=other.reversed;
        }
        return this;
    }

    public void clear() { path.clear(); reversed=false; }

    public boolean prepend(CsgOperandView operand)
    {
        if (operand == null || !path.add(operand)) return false;
        for (int i=path.size()-1;i>0;--i) path.set(i,path.get(i-1));
        path.set(0,operand);
        return true;
    }

    public void reverseOrientation() { reversed=!reversed; }
    public boolean isAvailable() { return path.size()>0; }
    public boolean isReversed() { return reversed; }
    public long getLength() { return path.size(); }

    public CsgOperandView getOperand(long index)
    { return index>=0 && index<path.size() ? path.get((int)index) : null; }
}
