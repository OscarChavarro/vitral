#ifndef __POLYHEDRAL_BOUNDED_SOLID_TEST_FIXTURES__
#define __POLYHEDRAL_BOUNDED_SOLID_TEST_FIXTURES__

#include "java/util/ArrayList.h"

class PolyhedralBoundedSolid;

/**
Shared bounded-solid fixtures for Euler, validation, and set-operation
tests.

Traceability: box and MANT fixtures are drawn from [MANT1988] Ch. 6
B-Rep examples, Ch. 9 Euler-construction examples, Fig. 6.13, and Ch. 15
boolean set-operation examples.
C++ counterpart of Java's `PolyhedralBoundedSolidTestFixtures` (test code):
the solids are new, owned by the caller; the Java arrays are lists.
*/
class PolyhedralBoundedSolidTestFixtures
{
public:
    static PolyhedralBoundedSolid* createBoxSolid(double sx, double sy,
        double sz, double tx, double ty, double tz);
    static java::ArrayList<PolyhedralBoundedSolid*> createDisjointBoxPair();
    static java::ArrayList<PolyhedralBoundedSolid*> createTouchingBoxPair();
    static java::ArrayList<PolyhedralBoundedSolid*> createContainmentBoxPair();
    static PolyhedralBoundedSolid* createMant1986_1Solid();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1986_2Pair();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1988_3Pair();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1988_6_13Pair();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1988_15_1Pair();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1988_15_2LimitPair();
    static java::ArrayList<PolyhedralBoundedSolid*> createMant1988_15_2Pair(
        int situation);

private:
    PolyhedralBoundedSolidTestFixtures();
};

#endif
