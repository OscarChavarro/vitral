#ifndef __CSG_TREE__
#define __CSG_TREE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"

// An application-built ordered node. Operands are borrowed and must remain
// alive. addOperand rejects cycles visible through the current expression graph.
// A nested tree is identity-placed; evaluators and ownership are separate.
class CsgTree : public CsgExpression, public CsgOperandView {
private:
    CsgOperation::Value operation;
    ArrayList<const CsgOperandView*> operands;
    long long structureVersion;
    BOOLEAN canAttach(const CsgOperandView& operand) const;
    CsgTree(const CsgTree&);
    CsgTree& operator=(const CsgTree&);
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    explicit CsgTree(CsgOperation::Value operation);
    virtual ~CsgTree();
    virtual CsgOperation::Value getOperation() const;
    virtual long int getOperandCount() const;
    virtual const CsgOperandView* getOperand(long int index) const;
    BOOLEAN addOperand(const CsgOperandView& operand);
    // Replaces the operation (e.g. a De Morgan complement); a structure edit.
    void setOperation(CsgOperation::Value value);
    virtual BOOLEAN isSameSolid(const CsgOperandView& other) const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual const CsgExpression* getChildExpression() const;
    virtual BOOLEAN isStructuralTraversalSafe() const;
    virtual BOOLEAN queryStructureVersion(long long& version) const;
    virtual BOOLEAN queryModificationVersion(long long& version) const;
};
#endif
