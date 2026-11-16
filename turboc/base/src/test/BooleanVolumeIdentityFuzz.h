#ifndef __BOOLEAN_IDENTITY_FUZZ__
#define __BOOLEAN_IDENTITY_FUZZ__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PrismFixtures.h"

/**
Seeded fuzzing of the set operations over prisms against the volume
identities |A u B| = |A| + |B| - |A n B|, |A - B| = |A| - |A n B| and
|B - A| = |B| - |A n B|. C++ counterpart of Java's
`BooleanVolumeIdentityFuzz` (test code); its `main` is not ported. The
`TreeMap<String, Integer>` of outcomes is two parallel lists kept sorted
by key.
*/
class BooleanVolumeIdentityFuzz
{
public:
    static const double TOLERANCE;
    enum { CASES_PER_FAMILY = 16 };

    enum Family
    {
        /** Two regular prisms; 5 to 24 sides. */
        CONVEX_PRISMS,
        /** A star prism and a regular prism. */
        STAR_AND_CONVEX,
        /** Two star prisms. */
        STAR_PRISMS,
        FAMILY_COUNT
    };
    static const char* familyName(Family family);

    class Summary
    {
    public:
        int total;
        int correct;
        ArrayList<String> outcomeKeys;
        ArrayList<int> outcomeCounts;
        ArrayList<String> failures;

        Summary();
        void merge(const String& key);
        BOOLEAN containsKey(const char* key) const;
        /** Java `byOutcome.toString()` of the TreeMap. */
        String byOutcomeText() const;
    };

    static Summary runFamily(Family family);
    static String check(const PrismPolygon& baseA, double heightA,
        const PrismPolygon& baseB, double heightB,
        const Matrix4x4d& placement);

private:
    BooleanVolumeIdentityFuzz();
    static PrismPolygon base(Family family, int which, Random& random);
    static Matrix4x4d placement(Random& random);
    static PolyhedralBoundedSolid* a(const PrismPolygon& base,
        double height);
    static PolyhedralBoundedSolid* b(const PrismPolygon& base,
        double height, const Matrix4x4d& placement);
    static double operationVolume(PolyhedralBoundedSolid* first,
        PolyhedralBoundedSolid* second, int op);
    static String firstLine(const String& message);
};

#endif
