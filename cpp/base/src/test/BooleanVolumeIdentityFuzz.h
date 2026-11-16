#ifndef __BOOLEAN_VOLUME_IDENTITY_FUZZ__
#define __BOOLEAN_VOLUME_IDENTITY_FUZZ__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "java/util/TreeMap.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PrismFixtures.h"

class PolyhedralBoundedSolid;

/**
Seeded fuzz that needs no oracle: for any two solids the four results must
satisfy

    |A u B| = |A| + |B| - |A n B|
    |A - B| = |A| - |A n B|
    |B - A| = |B| - |A n B|

with 0 <= |A n B| <= min(|A|, |B|). The operands are prisms: regular ones
of N sides (polyhedral cylinders) and star prisms (non-convex), B rotated
about a random axis so that no face of one is parallel to a face of the
other. The volume of each operand is also checked against the area of its
base polygon, so the prisms themselves are known to be right.

A case is correct when the four operations return strict-valid solids
and the three identities hold.
C++ counterpart of Java's `BooleanVolumeIdentityFuzz` (test code). Its
`main` (a table printer, not a test) is not ported.
*/
class BooleanVolumeIdentityFuzz
{
public:
    static const double TOLERANCE;

    enum Family {
        /** Two regular prisms; 5 to 24 sides. */
        CONVEX_PRISMS,
        /** A star prism and a regular prism. */
        STAR_AND_CONVEX,
        /** Two star prisms. */
        STAR_PRISMS
    };
    static const int FAMILY_COUNT = 3;
    static const Family FAMILIES[FAMILY_COUNT];
    static const char* familyName(Family family);

    class Summary {
    public:
        int total;
        int correct;
        java::TreeMap<java::String, int> byOutcome;
        java::ArrayList<java::String> failures;

        Summary();
        /** Java `byOutcome.toString()` of the TreeMap. */
        java::String byOutcomeText() const;
    };

    static const int CASES_PER_FAMILY = 16;

    static Summary runFamily(Family family);

    static java::String check(const PrismPolygon& baseA, double heightA,
        const PrismPolygon& baseB, double heightB,
        const Matrix4x4d& placement);

private:
    BooleanVolumeIdentityFuzz();
    static PrismPolygon base(Family family, int which, java::Random& random);
    /** B is rotated about a random axis and moved less than its size. */
    static Matrix4x4d placement(java::Random& random);
    static PolyhedralBoundedSolid* a(const PrismPolygon& base, double height);
    static PolyhedralBoundedSolid* b(const PrismPolygon& base, double height,
        const Matrix4x4d& placement);
    /** Volume of setOp(first, second, op); releases the three solids. */
    static double volumeOf(PolyhedralBoundedSolid* first,
        PolyhedralBoundedSolid* second, int op);
    static java::String firstLine(const java::String& message);
};

#endif
