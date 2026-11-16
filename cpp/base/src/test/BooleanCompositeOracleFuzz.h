#ifndef __BOOLEAN_COMPOSITE_ORACLE_FUZZ__
#define __BOOLEAN_COMPOSITE_ORACLE_FUZZ__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "CompositeShapeOracle.h"

class PolyhedralBoundedSolid;

/**
Seeded fuzz of booleans on non-convex operands and on chains of booleans,
checked against `CompositeShapeOracle` (Fable5.2Analysis.md Section 8,
"further tests worth writing"):
- non-convex operands (L, U, stepped block, frame with a through hole),
  axis-aligned in general position and rigidly rotated;
- chains, where the result of one boolean is an operand of the next:
  (A u B) \ C, (A n B) u C, A \ (B u C);
- scale: the rotated non-convex family at 10^-3 and at 10^3;
- commutativity of u and n: same volume and same topology summary.

A case is correct when every result is strict-valid and has the oracle
volume. A non-manifold rejection is correct only in the axis-aligned family:
after a random rigid motion the operands are in general position, where a
touching contact has probability zero, so a rejection there means the
kernel built a broken result. The scaled families replay the cases of the
rotated family. `BooleanCompositeOracleFuzzGateTest` is the ratchet.

C++ counterpart of Java's `BooleanCompositeOracleFuzz` (test code). Its
`main` (a table printer, not a test) is not ported. The Java `Expression`
lambdas are function pointers that return a new solid and release their
intermediate results.
*/
class BooleanCompositeOracleFuzz
{
public:
    typedef CompositeShapeOracle::Shape Shape;
    typedef java::ArrayList<Shape> ShapeList;

    enum Family
    {
        NON_CONVEX_AXIS_ALIGNED,
        NON_CONVEX_ROTATED,
        NON_CONVEX_ROTATED_SCALE_1E_MINUS_3,
        NON_CONVEX_ROTATED_SCALE_1E_PLUS_3,
        CHAINS,
        // Appended last: the seed of a family is its ordinal.
        NON_CONVEX_COPLANAR_BOTTOMS
    };
    static const int FAMILY_COUNT = 6;
    static const Family FAMILIES[FAMILY_COUNT];
    static int casesOf(Family family);
    static const char* familyName(Family family);

    enum Outcome
    {
        OK,
        REJECTED_NON_MANIFOLD,
        WRONG_VOLUME,
        STRICT_REJECTED,
        NOT_COMMUTATIVE,
        EXCEPTION
    };
    static const int OUTCOME_COUNT = 6;
    static const char* outcomeName(Outcome outcome);

    class CaseResult
    {
    public:
        Family family;
        int index;
        java::String label;
        Outcome outcome;
        bool hasDetail;
        java::String detail;

        CaseResult();
        bool operator==(const CaseResult& other) const;
        java::String toString() const;
    };

    class FamilySummary
    {
    public:
        int total;
        int correct;
        int byOutcome[OUTCOME_COUNT];
        java::ArrayList<CaseResult> failures;
        java::ArrayList<CaseResult> rejected;

        FamilySummary();
        /** Java `byOutcome.toString()` of the LinkedHashMap. */
        java::String byOutcomeText() const;
    };

    typedef PolyhedralBoundedSolid* (*Expression)(
        PolyhedralBoundedSolid** operands);

    static FamilySummary runFamily(Family family);

    static Shape randomShape(java::Random& random, double scale);
    static Matrix4x4d randomRotation(java::Random& random);
    static Matrix4x4d randomOffset(java::Random& random, double scale);

    /**
    The operands of case `index` of `family`, as the fuzz generates them
    (for reproducing one case in isolation).
    */
    static ShapeList shapesOf(Family family, int index);

    /**
    Strict setOp. A chain whose first step yields the empty solid passes it
    on as an operand: the empty set is a valid operand of the next step.
    Returns a new solid, owned by the caller; the operands are not changed.
    */
    static PolyhedralBoundedSolid* setOp(PolyhedralBoundedSolid* a,
        PolyhedralBoundedSolid* b, int op);

private:
    BooleanCompositeOracleFuzz();
    static void scaleBoxes(java::ArrayList<CompositeBox>& boxes,
        double scale);
    static long long seedOf(Family family);
    static double scaleOf(Family family);
    static ShapeList pairShapes(Family family, java::Random& random);
    static ShapeList coplanarPair(const Shape& a, const Shape& b,
        java::Random& random);
    static ShapeList chainShapes(java::Random& random);
    static java::ArrayList<CaseResult> runPair(Family family, int index,
        java::Random& random);
    static java::ArrayList<CaseResult> runChains(Family family, int index,
        java::Random& random);
    static PolyhedralBoundedSolid* copy(PolyhedralBoundedSolid* s);
    /** false (Java null) when the operands are right. */
    static bool checkOperands(const ShapeList& shapes,
        java::String* problem);
    static PolyhedralBoundedSolid** build(const ShapeList& shapes);
    static void release(PolyhedralBoundedSolid** solids, long count);
    static CaseResult evaluate(Family family, int index,
        const java::String& label, bool hasOperandProblem,
        const java::String& operandProblem, const ShapeList& shapes,
        CompositeShapeOracle::Membership membership, double tolerance,
        Expression expression);
    static CaseResult commutativity(Family family, int index,
        const java::String& label, bool hasOperandProblem,
        const java::String& operandProblem, const ShapeList& shapes, int op);
    static java::String describe(PolyhedralBoundedSolid* solid);
    static CaseResult result(Family family, int index,
        const java::String& label);
    static java::String firstLine(const java::String& text);
};

#endif
