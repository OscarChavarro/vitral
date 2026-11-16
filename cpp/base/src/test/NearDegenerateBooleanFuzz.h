#ifndef __NEAR_DEGENERATE_BOOLEAN_FUZZ__
#define __NEAR_DEGENERATE_BOOLEAN_FUZZ__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/TreeMap.h"
#include "BooleanOracleFuzz.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/**
Box pairs in degenerate contact, displaced by a multiple of the kernel
tolerance (plan-stage11 Phase B). Each configuration starts from an exact
contact and moves B along the contact normal by d = k x bigEpsilon, with k
in {-100, -10, -1, -0.1, 0, 0.1, 1, 10, 100}: from a clear overlap, through
the band where the kernel must decide whether two faces coincide, to a
clear gap. The whole pair is also tested rotated, so that the decision does
not rest on axis-aligned coordinates.

Outcomes of one operation:
- OK: a strict-valid solid whose volume is the exact one of the displaced
  boxes, within what a displacement of one tolerance can change;
- REJECTED: a typed exception (the operation refused to answer);
- WRONG: a returned solid with another volume, or not strict-valid. This
  is the outcome that must never happen.

C++ counterpart of Java's `NearDegenerateBooleanFuzz` (test code). Its
`main` (a table printer, not a test) is not ported.
*/
class NearDegenerateBooleanFuzz
{
public:
    typedef BooleanOracleFuzz::BoxPair BoxPair;

    static const int STEP_COUNT = 9;
    static const double STEPS[STEP_COUNT];

    enum Contact {
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
        EDGE_CONTACT
    };
    static const char* contactName(Contact contact);

    class Summary {
    public:
        int total;
        int answered;
        int rejected;
        int wrong;
        java::TreeMap<java::String, int> byOutcome;
        java::ArrayList<java::String> details;

        Summary();
        /** Java `details.toString()`. */
        java::String detailsText() const;
    };

    /** A and B for a contact, B moved by d along the contact normal. */
    static BoxPair pair(Contact contact, double d, bool rotated);

    static Summary run(Contact contact, bool rotated);

private:
    NearDegenerateBooleanFuzz();
    static PolyhedralBoundedSolidVolumeOracle::OrientedBox box(double x0,
        double y0, double z0, double x1, double y1, double z1);
    static java::String runOne(Contact contact, double d, bool rotated,
        int op, double bigEpsilon);
};

#endif
