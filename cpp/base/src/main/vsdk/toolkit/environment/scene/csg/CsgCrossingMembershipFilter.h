#ifndef __CSG_CROSSING_MEMBERSHIP_FILTER__
#define __CSG_CROSSING_MEMBERSHIP_FILTER__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryContainment.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventReference.h"
#include "vsdk/toolkit/environment/scene/csg/CsgMembershipTest.h"
class CsgExpression;
class CsgOperandView;
class CsgPointWorkspace;
class CsgRayLeafBatch;
class CsgRaySurfaceEvents;
class Ray;
class RayCrossingQuery;
class Vector3Dd;
// The POV-Ray 1.0 CSG heuristic: leaf boundary events are filtered by the
// membership of the event point in sibling operands. A union keeps every
// operand event, so internal surfaces remain. An intersection keeps an event
// of operand i when every other operand contains the point. A difference keeps
// a minuend event when no subtrahend contains it, and a subtrahend event when
// the minuend contains it and no other subtrahend does. Membership is
// measured in the coordinates of the node whose operands are tested, with the
// test chosen by setMembershipTest (see CsgMembershipTest): by default the
// sign of each primitive at the point, as POV-Ray 1.0 does, and otherwise the
// certified classification of the tolerance ball (also the fallback when the
// sign cannot be taken). Boundary counts as contained for an intersection
// operand and for the complement of a subtrahend, as in POV. An event is kept
// only when membership is decided and dropped when one sibling excludes it;
// otherwise the query fails with that sibling's status.
class CsgCrossingMembershipFilter {
private:
    static CsgMembershipTest membershipTest;
    // Deepest nesting of expressions that the sign test follows
    static const int MAX_SIGN_DEPTH = 64;
    // Appends the kept events of the expression to the workspace event stack.
    static RaySolidQueryStatus select(const CsgExpression& expression, const Ray& ray,
        double tolerance, const CsgRayLeafBatch& leaves, long int& leafIndex,
        CsgPointWorkspace& workspace);
    static RaySolidQueryStatus decide(const CsgExpression& expression, long int operandIndex,
        const Vector3Dd& point, double tolerance, CsgPointWorkspace& workspace, bool& keep);
    static RaySolidQueryStatus toStatus(GeometryContainment containment);
    // The membership of a sibling operand under the configured test.
    static GeometryContainment classifySibling(const CsgOperandView& sibling,
        const Vector3Dd& point, double tolerance, CsgPointWorkspace& workspace);
    // POV-Ray 1.0 inside test of an operand: Inside, Outside or Boundary, or
    // Indeterminate when it cannot decide (the certified test is used then).
    static GeometryContainment signContainment(const CsgOperandView& operand,
        const Vector3Dd& point, int depth);
public:
    // SIGN by default
    static void setMembershipTest(CsgMembershipTest test);
    static CsgMembershipTest getMembershipTest();
    static RaySolidQueryStatus select(const CsgExpression& expression, const Ray& ray,
        const RayCrossingQuery& query, double tolerance, CsgRaySurfaceEvents& result);
    // The same selection with caller-owned scratch storage: after warm-up it
    // allocates nothing beyond the leaf queries.
    static RaySolidQueryStatus select(const CsgExpression& expression, const Ray& ray,
        const RayCrossingQuery& query, double tolerance, CsgPointWorkspace& workspace,
        CsgRaySurfaceEvents& result);
    // Certified world classification of a single operand in its parent space.
    static GeometryContainment classifyOperand(const CsgOperandView& operand,
        const Vector3Dd& point, double tolerance);
};
#endif
