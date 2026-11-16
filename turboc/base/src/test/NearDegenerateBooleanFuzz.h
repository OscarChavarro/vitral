#ifndef __NEAR_DEGENERATE_FUZZ__
#define __NEAR_DEGENERATE_FUZZ__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/**
Set operations over box contacts displaced by a few multiples of the
kernel tolerance, around the exact contact. C++ counterpart of Java's
`NearDegenerateBooleanFuzz` (test code); its `main` is not ported. The
`TreeMap<String, Integer>` of outcomes is two parallel lists kept sorted
by key, and the returned `OrientedBox[]` pair an out-parameter.
*/
class NearDegenerateBooleanFuzz
{
public:
    enum { STEP_COUNT = 9 };
    static const double STEPS[STEP_COUNT];

    enum Contact
    {
        /** B on top of A, footprints partly overlapping. */
        STACKED_PARTIAL,
        /** B on top of A, same footprint. */
        STACKED_SAME_FOOTPRINT,
        /** B beside A with coplanar top and bottom, touching side faces. */
        SIDE_BY_SIDE,
        /** B crossing A with one face coplanar with a face of A. */
        FLUSH_FACE_CROSSING,
        /** B inside A touching one face of A from inside. */
        INNER_TANGENT,
        /** B touching A along one edge only. */
        EDGE_CONTACT,
        CONTACT_COUNT
    };
    static const char* contactName(Contact contact);

    class Summary
    {
    public:
        int total;
        int answered;
        int rejected;
        int wrong;
        ArrayList<String> outcomeKeys;
        ArrayList<int> outcomeCounts;
        ArrayList<String> details;

        Summary();
        void merge(const String& key);
    };

    /** A and B for a contact, B moved by d along the contact normal. */
    static void pair(Contact contact, double d, BOOLEAN rotated,
        PolyhedralBoundedSolidVolumeOracle::OrientedBox& a,
        PolyhedralBoundedSolidVolumeOracle::OrientedBox& b);

    static Summary run(Contact contact, BOOLEAN rotated);

private:
    NearDegenerateBooleanFuzz();
    static PolyhedralBoundedSolidVolumeOracle::OrientedBox box(double x0,
        double y0, double z0, double x1, double y1, double z1);
    static String runOne(Contact contact, double d, BOOLEAN rotated,
        int op, double bigEpsilon);
};

#endif
