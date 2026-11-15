#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
#include "vsdk/toolkit/environment/scene/csg/CsgTree.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpressionEquivalence.h"
BOOLEAN CsgExpressionEquivalence::same(const CsgOperandView& a, const CsgOperandView& b)
{
    ArrayList<const CsgExpression*> pathA,pathB;
    return sameView(a,b,pathA,pathB);
}
BOOLEAN CsgExpressionEquivalence::sameView(const CsgOperandView& a, const CsgOperandView& b,
    ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB)
{
    const CsgExpression* childA=a.getChildExpression();
    const CsgExpression* childB=b.getChildExpression();
    if (!childA || !childB) return !childA && !childB && a.isSameSolid(b) && b.isSameSolid(a);
    // Arbitrary application views may add inversion, clipping or other
    // semantics not represented by their child and placement. Only the known
    // shared structural views can be compared by unpacking their children.
    const BOOLEAN structuralA=a.getClass()==&CsgTree::CLASS || a.getClass()==&CsgPlacedExpression::CLASS;
    const BOOLEAN structuralB=b.getClass()==&CsgTree::CLASS || b.getClass()==&CsgPlacedExpression::CLASS;
    if (!structuralA || !structuralB) return FALSE;
    Matrix4x4d forwardA,inverseA,forwardB,inverseB;
    if (a.queryAffinePlacement(forwardA,inverseA)!=AffinePlacementStatus::Valid ||
        b.queryAffinePlacement(forwardB,inverseB)!=AffinePlacementStatus::Valid) return FALSE;
    for (int i=0;i<4;++i) for (int j=0;j<4;++j)
        if (forwardA.get(i,j)!=forwardB.get(i,j)) return FALSE;
    return sameExpression(*childA,*childB,pathA,pathB);
}
BOOLEAN CsgExpressionEquivalence::collectOperand(const CsgOperandView& view, CsgOperation::Value operation,
    ArrayList<const CsgExpression*>& path, ArrayList<const CsgOperandView*>& output)
{ long i;
    const CsgExpression* child=view.getChildExpression();
    const BOOLEAN knownView=view.getClass()==&CsgTree::CLASS || view.getClass()==&CsgPlacedExpression::CLASS;
    if (!child || !knownView || child->getClass()!=&CsgTree::CLASS || child->getOperation()!=operation)
        return output.add(&view);
    Matrix4x4d forward,inverse;
    if (view.queryAffinePlacement(forward,inverse)!=AffinePlacementStatus::Valid) return FALSE;
    BOOLEAN identity=TRUE;
    for (i =0;i<4;++i) for (int j=0;j<4;++j)
        if (forward.get(i,j)!=(i==j ? 1.0 : 0.0)) identity=FALSE;
    if (!identity) return output.add(&view);
    if (path.contains(child) || child->getOperandCount()<=0 || !path.add(child)) return FALSE;
    BOOLEAN valid=TRUE;
    for (i =0;i<child->getOperandCount() && valid;++i) {
        const CsgOperandView* item=child->getOperand(i);
        valid=item && collectOperand(*item,operation,path,output);
    }
    path.remove(path.size()-1);
    return valid;
}
BOOLEAN CsgExpressionEquivalence::sameSets(const ArrayList<const CsgOperandView*>& a,
    const ArrayList<const CsgOperandView*>& b,
    ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB)
{
    // Union and intersection are idempotent as solid-set operations. Check
    // both directions without consuming matches so duplicates are harmless.
    for (int direction=0;direction<2;++direction) {
        const ArrayList<const CsgOperandView*>& from=direction==0 ? a : b;
        const ArrayList<const CsgOperandView*>& to=direction==0 ? b : a;
        for (long int i=0;i<from.size();++i) {
            BOOLEAN found=FALSE;
            for (long int j=0;j<to.size() && !found;++j)
                found=direction==0 ? sameView(*from[i],*to[j],pathA,pathB) :
                    sameView(*to[j],*from[i],pathA,pathB);
            if (!found) return FALSE;
        }
    }
    return TRUE;
}
BOOLEAN CsgExpressionEquivalence::sameExpression(const CsgExpression& a, const CsgExpression& b,
    ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB)
{ long int i;
    if (pathA.contains(&a) || pathB.contains(&b)) return FALSE;
    if (a.getClass()!=&CsgTree::CLASS || b.getClass()!=&CsgTree::CLASS) return FALSE;
    const CsgOperation::Value op=a.getOperation();
    if (a.getOperandCount()<=0 || b.getOperandCount()<=0 || op!=b.getOperation() ||
        (op!=CsgOperation::UNION && op!=CsgOperation::INTERSECTION && op!=CsgOperation::DIFFERENCE)) return FALSE;
    if (!pathA.add(&a)) return FALSE;
    if (!pathB.add(&b)) { pathA.remove(pathA.size()-1); return FALSE; }
    BOOLEAN equal=TRUE;
    long int start=0;
    if (op==CsgOperation::DIFFERENCE) {
        const CsgOperandView* firstA=a.getOperand(0); const CsgOperandView* firstB=b.getOperand(0);
        equal=firstA && firstB && sameView(*firstA,*firstB,pathA,pathB);
        start=1;
    }
    ArrayList<const CsgOperandView*> itemsA,itemsB;
    const CsgOperation::Value associative=op==CsgOperation::DIFFERENCE ? CsgOperation::UNION : op;
    for (i =start;equal && i<a.getOperandCount();++i) {
        const CsgOperandView* item=a.getOperand(i);
        equal=item && collectOperand(*item,associative,pathA,itemsA);
    }
    for (i =start;equal && i<b.getOperandCount();++i) {
        const CsgOperandView* item=b.getOperand(i);
        equal=item && collectOperand(*item,associative,pathB,itemsB);
    }
    if (equal) equal=sameSets(itemsA,itemsB,pathA,pathB);
    pathA.remove(pathA.size()-1); pathB.remove(pathB.size()-1);
    return equal;
}
