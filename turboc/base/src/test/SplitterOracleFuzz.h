#ifndef __SPLITTER_ORACLE_FUZZ__
#define __SPLITTER_ORACLE_FUZZ__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class InfinitePlane;
class PolyhedralBoundedSolid;

/**
Java's `SplitterOracleFuzzTest.check`, shared with the general-plane split
fuzz (C++ port note: the test programs of the port are separate
executables, so the shared check is a helper of its own).
*/
class SplitterOracleFuzz
{
public:
    /**
    Splits a copy of `base` by `plane` and checks the pieces against the
    volume oracle.
    @return the reason of the failure, or an empty string (Java null) when
    the split is correct
    */
    static String check(PolyhedralBoundedSolid* base,
        const InfinitePlane& plane);

private:
    SplitterOracleFuzz();
};

#endif
