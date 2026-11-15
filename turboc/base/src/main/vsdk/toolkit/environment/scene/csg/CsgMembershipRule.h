#ifndef __CSG_MEMBERSHIP_RULE__
#define __CSG_MEMBERSHIP_RULE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperation.h"
// Boolean rules shared by every CSG evaluator over an ordered expression.
class CsgMembershipRule {
public:
    // [ROTH1982] Table 3, folded left to right over the operands: the value of
    // operand 0 starts the fold and each later operand is combined with it, so
    // a difference subtracts operands 1..N from operand 0.
    static BOOLEAN combine(CsgOperation::Value operation, BOOLEAN accumulated, BOOLEAN operandInside);
    // Whether no later operand can change an accumulated fold value: true for
    // a union already inside and for an intersection or difference already
    // outside. Point evaluators stop querying operands there.
    static BOOLEAN isDecided(CsgOperation::Value operation, BOOLEAN accumulated);
    // POV crossing-membership rule: whether an event of operand eventOperand
    // must lie inside sibling (true) or outside it (false). Union events are
    // never filtered, so it is only meaningful when filtersEvents is true.
    static BOOLEAN filtersEvents(CsgOperation::Value operation);
    static BOOLEAN requiresContainment(CsgOperation::Value operation, long int eventOperand, long int sibling);
};

inline BOOLEAN CsgMembershipRule::combine(CsgOperation::Value operation, BOOLEAN accumulated, BOOLEAN operandInside)
{
    if (operation==CsgOperation::UNION) return accumulated || operandInside;
    if (operation==CsgOperation::INTERSECTION) return accumulated && operandInside;
    return accumulated && !operandInside;
}

inline BOOLEAN CsgMembershipRule::isDecided(CsgOperation::Value operation, BOOLEAN accumulated)
{
    return operation==CsgOperation::UNION ? accumulated : (BOOLEAN)!accumulated;
}

inline BOOLEAN CsgMembershipRule::filtersEvents(CsgOperation::Value operation)
{
    return operation!=CsgOperation::UNION;
}

inline BOOLEAN CsgMembershipRule::requiresContainment(CsgOperation::Value operation, long int eventOperand, long int sibling)
{
    (void)eventOperand;
    return operation==CsgOperation::INTERSECTION || (operation==CsgOperation::DIFFERENCE && sibling==0);
}
#endif
