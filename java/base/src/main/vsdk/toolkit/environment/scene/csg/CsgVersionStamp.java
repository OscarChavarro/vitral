package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Modification versions of every expression node and operand reachable from
a root, recorded when a result that borrows them is built. A result is known
to be current only when the stamp is complete (every node and operand
reports a version) and every version is unchanged. Versions cover edits made
through the scene and CSG APIs; geometry parameters changed behind a body's
back are not observed. Storage is reused across captures.
*/
public class CsgVersionStamp {
    private final ArrayList<CsgExpression> expressions = new ArrayList<CsgExpression>();

    private final ArrayList<Long> expressionVersions = new ArrayList<Long>();

    private final ArrayList<CsgOperandView> operands = new ArrayList<CsgOperandView>();

    private final ArrayList<Long> operandVersions = new ArrayList<Long>();

    private final ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();

    private boolean complete;

    private boolean captured;

    private boolean record(CsgExpression expression) {
    if (path.contains(expression) || !path.add(expression)) return false;
    long[] version = {0};
    if (!expression.queryStructureVersion(version)) complete=false;
    if (!expressions.add(expression) || !expressionVersions.add(version[0])) return false;
    for (long i=0;i<expression.getOperandCount();++i) {
        final CsgOperandView operand =expression.getOperand(i);
        if (operand == null) { complete=false; continue; }
        long[] operandVersion = {0};
        if (!operand.queryModificationVersion(operandVersion)) complete=false;
        if (!operands.add(operand) || !operandVersions.add(operandVersion[0])) return false;
        final CsgExpression child =operand.getChildExpression();
        if (child != null && !record(child)) return false;
    }
    path.remove(path.size()-1);
    return true;
}

    public CsgVersionStamp() {
        this.complete = false;
        this.captured = false;}

    public CsgVersionStamp(CsgVersionStamp other) {
        this.complete = false;
        this.captured = false; this.assign(other); }

    /** cpp copy assignment. */
    public CsgVersionStamp assign(CsgVersionStamp other) {
    if (this!=other) {
        clear();
        for (long i=0;i<other.expressions.size();++i) {
            expressions.add(other.expressions.get((int)(i))); expressionVersions.add(other.expressionVersions.get((int)(i)));
        }
        for (long i=0;i<other.operands.size();++i) {
            operands.add(other.operands.get((int)(i))); operandVersions.add(other.operandVersions.get((int)(i)));
        }
        complete=other.complete; captured=other.captured;
    }
    return this;
}

    public void clear() {
    expressions.clear(); expressionVersions.clear(); operands.clear(); operandVersions.clear();
    path.clear(); complete=false; captured=false;
}

    // False only for a cyclic expression or exhausted storage; the stamp is
    // then cleared.
    public boolean capture(CsgExpression root) {
    clear();
    complete=true;
    if (!record(root)) { clear(); return false; }
    path.clear();
    captured=true;
    return true;
}

    public boolean isCaptured() { return captured; }

    public boolean isComplete() { return captured && complete; }

    public boolean isCurrent() {
    if (!isComplete()) return false;
    for (long i=0;i<expressions.size();++i) {
        long[] version = {0};
        if (!expressions.get((int)(i)).queryStructureVersion(version) || version[0]!=expressionVersions.get((int)(i))) return false;
    }
    for (long i=0;i<operands.size();++i) {
        long[] version = {0};
        if (!operands.get((int)(i)).queryModificationVersion(version) || version[0]!=operandVersions.get((int)(i))) return false;
    }
    return true;
}
}

