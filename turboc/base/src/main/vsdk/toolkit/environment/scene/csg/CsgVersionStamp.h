#ifndef __CSG_VERSION_STAMP__
#define __CSG_VERSION_STAMP__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
class CsgExpression;
class CsgOperandView;
// Modification versions of every expression node and operand reachable from
// a root, recorded when a result that borrows them is built. A result is known
// to be current only when the stamp is complete (every node and operand
// reports a version) and every version is unchanged. Versions cover edits made
// through the scene and CSG APIs; geometry parameters changed behind a body's
// back are not observed. Storage is reused across captures.
class CsgVersionStamp {
private:
    ArrayList<const CsgExpression*> expressions;
    ArrayList<long long> expressionVersions;
    ArrayList<const CsgOperandView*> operands;
    ArrayList<long long> operandVersions;
    ArrayList<const CsgExpression*> path;
    BOOLEAN complete;
    BOOLEAN captured;
    BOOLEAN record(const CsgExpression& expression);
public:
    CsgVersionStamp();
    CsgVersionStamp(const CsgVersionStamp& other);
    CsgVersionStamp& operator=(const CsgVersionStamp& other);
    ~CsgVersionStamp();
    void clear();
    // False only for a cyclic expression or exhausted storage; the stamp is
    // then cleared.
    BOOLEAN capture(const CsgExpression& root);
    BOOLEAN isCaptured() const;
    BOOLEAN isComplete() const;
    BOOLEAN isCurrent() const;
};
#endif
