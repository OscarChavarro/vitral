#ifndef __BOOLEAN_ORACLE_FUZZ__
#define __BOOLEAN_ORACLE_FUZZ__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/**
Seeded box-pair fuzz over the six families of Fable5.2Analysis.md Section 1,
judged by `PolyhedralBoundedSolidVolumeOracle`: a result counts as
correct only when setOp returns without exception, the result passes
validateStrict and its divergence-theorem volume matches the exact
inclusion-exclusion volume. Each pair runs A u B, A n B, A \ B and B \ A.

The same generator is used by the slow gate test.
C++ counterpart of Java's `BooleanOracleFuzz` (test code). Its `main`
(a table printer, not a test) is not ported. Java `enum` values are kept
in order; `FAMILY_COUNT` and `OUTCOME_COUNT` stand for `values()`.
*/
class BooleanOracleFuzz
{
public:
    typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;

    static const int B_MINUS_A = 4;
    static const int OPERATIONS[4];

    enum Family
    {
        AXIS_ALIGNED_GENERAL_POSITION,
        B_ROTATED_ABOUT_Z,
        B_ROTATED_ABOUT_RANDOM_AXIS,
        RANDOM_AXIS_TRANSLATED_TO_1000,
        INTEGER_GRID,
        COPLANAR_TOP_BOTTOM_ROTATED_45
    };
    static const int FAMILY_COUNT = 6;
    static const Family FAMILIES[FAMILY_COUNT];
    static int pairsOf(Family family);
    static const char* familyName(Family family);

    enum Outcome
    {
        OK,
        /** Union of solids touching only along edges or at vertices,
        rejected by design as non-manifold (B-04): a correct answer. */
        REJECTED_NON_MANIFOLD,
        WRONG_VOLUME,
        STRICT_REJECTED,
        EXCEPTION
    };
    static const int OUTCOME_COUNT = 5;
    static const char* outcomeName(Outcome outcome);

    class CaseResult
    {
    public:
        Family family;
        int pairIndex;
        int operation;
        Outcome outcome;
        java::String path;
        java::String detail;

        CaseResult();
        CaseResult(Family family, int pairIndex, int op, Outcome outcome,
            const java::String& path, const java::String& detail);
        bool operator==(const CaseResult& other) const;
        java::String toString() const;
    };

    class FamilySummary
    {
    public:
        int total;
        int correct;
        /** Included in `correct`. */
        int okByRejection;
        int byOutcome[OUTCOME_COUNT];
        java::ArrayList<CaseResult> failures;

        FamilySummary();
        /** Java `byOutcome.toString()` of the LinkedHashMap. */
        java::String byOutcomeText() const;
    };

    class BoxPair
    {
    public:
        OrientedBox v[2];

        BoxPair();
        BoxPair(const OrientedBox& a, const OrientedBox& b);
        const OrientedBox& operator[](int i) const;
        bool operator==(const BoxPair& other) const;
    };

    static java::String opName(int op);

    /** Fixed-seed pairs of one family; the seed depends only on the family. */
    static java::ArrayList<BoxPair> pairs(Family family);

    static CaseResult run(Family family, int pairIndex, const BoxPair& pair,
        int op);

    static FamilySummary runFamily(Family family);

private:
    BooleanOracleFuzz();
    static double uniform(java::Random& random, double lo, double hi);
    static Matrix4x4d randomAxisRotation(java::Random& random);
    static BoxPair pair(Family family, java::Random& random);
    /** Box with integer corners in [0, 3]^3: coplanar faces, shared edges. */
    static OrientedBox gridBox(java::Random& random);
    static java::String firstLine(const java::String& s);
};

#endif
