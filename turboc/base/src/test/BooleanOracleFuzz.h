#ifndef __BOOLEAN_ORACLE_FUZZ__
#define __BOOLEAN_ORACLE_FUZZ__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/**
Seeded box-pair fuzzing of the boolean set operations against the volume
oracle. C++ counterpart of Java's `BooleanOracleFuzz` (test code); its
`main` (a table printer, not a test) is not ported. Java `enum` values are
kept as C++ enums with name functions, and the `Map<Outcome, Integer>` of a
summary is an array indexed by outcome.
*/
class BooleanOracleFuzz
{
public:
    enum { B_MINUS_A = 4, OPERATION_COUNT = 4 };
    static const int OPERATIONS[OPERATION_COUNT];

    enum Family
    {
        AXIS_ALIGNED_GENERAL_POSITION,
        B_ROTATED_ABOUT_Z,
        B_ROTATED_ABOUT_RANDOM_AXIS,
        RANDOM_AXIS_TRANSLATED_TO_1000,
        INTEGER_GRID,
        COPLANAR_TOP_BOTTOM_ROTATED_45,
        FAMILY_COUNT
    };
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
        EXCEPTION,
        OUTCOME_COUNT
    };
    static const char* outcomeName(Outcome outcome);

    class CaseResult
    {
    public:
        Family family;
        int pairIndex;
        int operation;
        Outcome outcome;
        /** Java `String path` that may be null. */
        String path;
        BOOLEAN hasPath;
        String detail;

        CaseResult();
        CaseResult(Family family, int pairIndex, int op, Outcome outcome,
            const String* path, const String& detail);
        String toString() const;
        BOOLEAN operator==(const CaseResult& other) const;
    };

    class FamilySummary
    {
    public:
        int total;
        int correct;
        /** Included in `correct`. */
        int okByRejection;
        int byOutcome[OUTCOME_COUNT];
        ArrayList<CaseResult> failures;

        FamilySummary();
        /** Java `byOutcome.toString()` of the LinkedHashMap. */
        String byOutcomeText() const;
    };

    /** Java `OrientedBox[]` of two boxes: A is `[0]`, B is `[1]`. */
    class BoxPair
    {
    public:
        PolyhedralBoundedSolidVolumeOracle::OrientedBox firstBox;
        PolyhedralBoundedSolidVolumeOracle::OrientedBox secondBox;

        BoxPair();
        BoxPair(const PolyhedralBoundedSolidVolumeOracle::OrientedBox& a,
            const PolyhedralBoundedSolidVolumeOracle::OrientedBox& b);
        BOOLEAN operator==(const BoxPair& other) const;
    };

    static String opName(int op);

    /** Fixed-seed pairs of one family; the seed depends only on the family. */
    static ArrayList<BoxPair> pairs(Family family);

    static CaseResult run(Family family, int pairIndex, const BoxPair& pair,
        int op);

    static FamilySummary runFamily(Family family);

    static String firstLine(const String& s);

private:
    BooleanOracleFuzz();
    static double uniform(Random& random, double lo, double hi);
    static Matrix4x4d randomAxisRotation(Random& random);
    static BoxPair pair(Family family, Random& random);
    static PolyhedralBoundedSolidVolumeOracle::OrientedBox gridBox(
        Random& random);
};

inline BOOLEAN
vsdkEquals(const BooleanOracleFuzz::CaseResult &a,
    const BooleanOracleFuzz::CaseResult &b)
{
    return a == b;
}

inline BOOLEAN
vsdkEquals(const BooleanOracleFuzz::BoxPair &a,
    const BooleanOracleFuzz::BoxPair &b)
{
    return a == b;
}

#endif
