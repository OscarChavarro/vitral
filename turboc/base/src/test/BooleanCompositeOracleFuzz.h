#ifndef __BOOLEAN_COMPOSITE_FUZZ__
#define __BOOLEAN_COMPOSITE_FUZZ__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "CompositeShapeOracle.h"

class PolyhedralBoundedSolid;

/**
Seeded fuzzing of the set operations over non-convex operands (unions and
differences of boxes under rigid motions) against `CompositeShapeOracle`.
C++ counterpart of Java's `BooleanCompositeOracleFuzz` (test code); its
`main` is not ported. The Java lambdas of the expressions are expression
codes, and the `Map<Outcome, Integer>` an array indexed by outcome.
*/
class BooleanCompositeOracleFuzz
{
public:
    enum Family
    {
        NON_CONVEX_AXIS_ALIGNED,
        NON_CONVEX_ROTATED,
        NON_CONVEX_ROTATED_SCALE_1E_MINUS_3,
        NON_CONVEX_ROTATED_SCALE_1E_PLUS_3,
        CHAINS,
        // Appended last: the seed of a family is its ordinal.
        NON_CONVEX_COPLANAR_BOTTOMS,
        FAMILY_COUNT
    };
    static int casesOf(Family family);
    static const char* familyName(Family family);

    enum Outcome
    {
        OK,
        REJECTED_NON_MANIFOLD,
        WRONG_VOLUME,
        STRICT_REJECTED,
        NOT_COMMUTATIVE,
        EXCEPTION,
        OUTCOME_COUNT
    };
    static const char* outcomeName(Outcome outcome);

    class CaseResult
    {
    public:
        Family family;
        int index;
        String label;
        Outcome outcome;
        /** Java `String detail` that may be null (empty here). */
        String detail;

        CaseResult();
        String toString() const;
        BOOLEAN operator==(const CaseResult& other) const;
    };

    class FamilySummary
    {
    public:
        int total;
        int correct;
        int byOutcome[OUTCOME_COUNT];
        ArrayList<CaseResult> failures;
        ArrayList<CaseResult> rejected;

        FamilySummary();
        /** Java `byOutcome.toString()` of the LinkedHashMap. */
        String byOutcomeText() const;
    };

    static FamilySummary runFamily(Family family);

    static CompositeShapeOracle::Shape randomShape(Random& random,
        double scale);
    static Matrix4x4d randomRotation(Random& random);
    static Matrix4x4d randomOffset(Random& random, double scale);

    /**
    The operands of case `index` of `family`, as the fuzz generates them
    (for reproducing one case in isolation).
    */
    static ArrayList<CompositeShapeOracle::Shape> shapesOf(Family family,
        int index);

    /**
    Strict setOp. A chain whose first step yields the empty solid passes it
    on as an operand: the empty set is a valid operand of the next step.
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* setOp(PolyhedralBoundedSolid* a,
        PolyhedralBoundedSolid* b, int op);

private:
    /** The Java `Expression` lambdas of `runPair` and `runChains`. */
    enum ExpressionCode
    {
        PAIR_UNION,
        PAIR_INTERSECTION,
        PAIR_A_MINUS_B,
        PAIR_B_MINUS_A,
        CHAIN_UNION_MINUS,
        CHAIN_INTERSECTION_UNION,
        CHAIN_MINUS_UNION
    };

    BooleanCompositeOracleFuzz();
    static int seedOf(Family family);
    static double scaleOf(Family family);
    static void scaleBoxes(ArrayList<CompositeBox>& boxes, double scale);
    static ArrayList<CompositeShapeOracle::Shape> pairShapes(Family family,
        Random& random);
    static ArrayList<CompositeShapeOracle::Shape> coplanarPair(
        const CompositeShapeOracle::Shape& a,
        const CompositeShapeOracle::Shape& b, Random& random);
    static ArrayList<CompositeShapeOracle::Shape> chainShapes(
        Random& random);
    static void runPair(Family family, int index, Random& random,
        ArrayList<CaseResult>& results);
    static void runChains(Family family, int index, Random& random,
        ArrayList<CaseResult>& results);
    static PolyhedralBoundedSolid* copy(PolyhedralBoundedSolid* s);
    static String checkOperands(
        const ArrayList<CompositeShapeOracle::Shape>& shapes);
    static void build(const ArrayList<CompositeShapeOracle::Shape>& shapes,
        ArrayList<PolyhedralBoundedSolid*>& solids);
    static PolyhedralBoundedSolid* apply(ExpressionCode code,
        ArrayList<PolyhedralBoundedSolid*>& ops,
        ArrayList<PolyhedralBoundedSolid*>& garbage);
    static CaseResult evaluate(Family family, int index,
        const String& label, const String& operandProblem,
        const ArrayList<CompositeShapeOracle::Shape>& shapes,
        CompositeMembership membership, double tolerance,
        ExpressionCode expression);
    static CaseResult commutativity(Family family, int index,
        const String& label, const String& operandProblem,
        const ArrayList<CompositeShapeOracle::Shape>& shapes, int op);
    static String describe(PolyhedralBoundedSolid* solid);
    static CaseResult result(Family family, int index, const String& label);
    static String firstLine(const String& text);
};

inline BOOLEAN
vsdkEquals(const BooleanCompositeOracleFuzz::CaseResult &a,
    const BooleanCompositeOracleFuzz::CaseResult &b)
{
    return a == b;
}

#endif
