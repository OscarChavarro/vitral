#ifndef __PBS_TEST_FIXTURES__
#define __PBS_TEST_FIXTURES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"

class PolyhedralBoundedSolid;

/**
Shared fixtures of the polyhedral tests. C++ counterpart of Java's
`PolyhedralBoundedSolidTestFixtures` (test code). The Java arrays of two
solids are lists of two new solids, owned by the caller.
*/
class PolyhedralBoundedSolidTestFixtures
{
public:
    static PolyhedralBoundedSolid* createBoxSolid(double sx, double sy,
        double sz, double tx, double ty, double tz);
    static ArrayList<PolyhedralBoundedSolid*> createDisjointBoxPair();
    static ArrayList<PolyhedralBoundedSolid*> createTouchingBoxPair();
    static ArrayList<PolyhedralBoundedSolid*> createContainmentBoxPair();
    static PolyhedralBoundedSolid* createMant1986_1Solid();
    static ArrayList<PolyhedralBoundedSolid*> createMant1986_2Pair();
    static ArrayList<PolyhedralBoundedSolid*> createMant1988_3Pair();
    static ArrayList<PolyhedralBoundedSolid*> createMant1988_6_13Pair();
    static ArrayList<PolyhedralBoundedSolid*> createMant1988_15_1Pair();
    static ArrayList<PolyhedralBoundedSolid*> createMant1988_15_2LimitPair();
    static ArrayList<PolyhedralBoundedSolid*> createMant1988_15_2Pair(
        int situation);

    /** C++ helper: deletes the solids of a pair (Java leaves them to GC). */
    static void dispose(ArrayList<PolyhedralBoundedSolid*>& pair);

private:
    PolyhedralBoundedSolidTestFixtures();
    static ArrayList<PolyhedralBoundedSolid*> pairOf(
        PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b);
};

#endif
