package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
Sufficient structural equality; unknown or cyclic expressions are rejected.
*/
public class CsgExpressionEquivalence {
    private static boolean sameView(CsgOperandView a, CsgOperandView b, ArrayList<CsgExpression> pathA, ArrayList<CsgExpression> pathB) {
    final CsgExpression childA =a.getChildExpression();
    final CsgExpression childB =b.getChildExpression();
    if (childA == null || childB == null) return childA == null && childB == null && a.isSameSolid(b) && b.isSameSolid(a);
    // Arbitrary application views may add inversion, clipping or other
    // semantics not represented by their child and placement. Only the known
    // shared structural views can be compared by unpacking their children.
    final boolean structuralA=a.getClass()==CsgTree.class || a.getClass()==CsgPlacedExpression.class;
    final boolean structuralB=b.getClass()==CsgTree.class || b.getClass()==CsgPlacedExpression.class;
    if (!structuralA || !structuralB) return false;
    Matrix4x4d[] forwardA = {null};
    Matrix4x4d[] inverseA = {null};
    Matrix4x4d[] forwardB = {null};
    Matrix4x4d[] inverseB = {null};
    if (a.queryAffinePlacement(forwardA,inverseA)!=AffinePlacementStatus.Valid ||
        b.queryAffinePlacement(forwardB,inverseB)!=AffinePlacementStatus.Valid) return false;
    for (int i=0;i<4;++i) for (int j=0;j<4;++j)
        if (forwardA[0].get(i,j)!=forwardB[0].get(i,j)) return false;
    return sameExpression(childA,childB,pathA,pathB);
}

    private static boolean collectOperand(CsgOperandView view, CsgOperation operation, ArrayList<CsgExpression> path, ArrayList<CsgOperandView> output) {
    final CsgExpression child =view.getChildExpression();
    final boolean knownView=view.getClass()==CsgTree.class || view.getClass()==CsgPlacedExpression.class;
    if (child == null || !knownView || child.getClass()!=CsgTree.class || child.getOperation()!=operation)
        return output.add(view);
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    if (view.queryAffinePlacement(forward,inverse)!=AffinePlacementStatus.Valid) return false;
    boolean identity=true;
    for (int i=0;i<4;++i) for (int j=0;j<4;++j)
        if (forward[0].get(i,j)!=(i==j ? 1.0 : 0.0)) identity=false;
    if (!identity) return output.add(view);
    if (path.contains(child) || child.getOperandCount()<=0 || !path.add(child)) return false;
    boolean valid=true;
    for (long i=0;i<child.getOperandCount() && valid;++i) {
        final CsgOperandView item =child.getOperand(i);
        valid=item != null && collectOperand(item,operation,path,output);
    }
    path.remove(path.size()-1);
    return valid;
}

    private static boolean sameSets(ArrayList<CsgOperandView> a, ArrayList<CsgOperandView> b, ArrayList<CsgExpression> pathA, ArrayList<CsgExpression> pathB) {
    // Union and intersection are idempotent as solid-set operations. Check
    // both directions without consuming matches so duplicates are harmless.
    for (int direction=0;direction<2;++direction) {
        final ArrayList<CsgOperandView> from=direction==0 ? a : b;
        final ArrayList<CsgOperandView> to=direction==0 ? b : a;
        for (long i=0;i<from.size();++i) {
            boolean found=false;
            for (long j=0;j<to.size() && !found;++j)
                found=direction==0 ? sameView(from.get((int)(i)),to.get((int)(j)),pathA,pathB) :
                    sameView(to.get((int)(j)),from.get((int)(i)),pathA,pathB);
            if (!found) return false;
        }
    }
    return true;
}

    private static boolean sameExpression(CsgExpression a, CsgExpression b, ArrayList<CsgExpression> pathA, ArrayList<CsgExpression> pathB) {
    if (pathA.contains(a) || pathB.contains(b)) return false;
    if (a.getClass()!=CsgTree.class || b.getClass()!=CsgTree.class) return false;
    final CsgOperation op=a.getOperation();
    if (a.getOperandCount()<=0 || b.getOperandCount()<=0 || op!=b.getOperation() ||
        (op!=CsgOperation.UNION && op!=CsgOperation.INTERSECTION && op!=CsgOperation.DIFFERENCE)) return false;
    if (!pathA.add(a)) return false;
    if (!pathB.add(b)) { pathA.remove(pathA.size()-1); return false; }
    boolean equal=true;
    long start=0;
    if (op==CsgOperation.DIFFERENCE) {
        final CsgOperandView firstA =a.getOperand(0); final CsgOperandView firstB =b.getOperand(0);
        equal=firstA != null && firstB != null && sameView(firstA,firstB,pathA,pathB);
        start=1;
    }
    ArrayList<CsgOperandView> itemsA = new ArrayList<CsgOperandView>(), itemsB = new ArrayList<CsgOperandView>();
    final CsgOperation associative=op==CsgOperation.DIFFERENCE ? CsgOperation.UNION : op;
    for (long i=start;equal && i<a.getOperandCount();++i) {
        final CsgOperandView item =a.getOperand(i);
        equal=item != null && collectOperand(item,associative,pathA,itemsA);
    }
    for (long i=start;equal && i<b.getOperandCount();++i) {
        final CsgOperandView item =b.getOperand(i);
        equal=item != null && collectOperand(item,associative,pathB,itemsB);
    }
    if (equal) equal=sameSets(itemsA,itemsB,pathA,pathB);
    pathA.remove(pathA.size()-1); pathB.remove(pathB.size()-1);
    return equal;
}

    public static boolean same(CsgOperandView a, CsgOperandView b) {
    ArrayList<CsgExpression> pathA = new ArrayList<CsgExpression>(), pathB = new ArrayList<CsgExpression>();
    return sameView(a,b,pathA,pathB);
}
}

