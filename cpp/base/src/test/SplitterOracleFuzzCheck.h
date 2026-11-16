#ifndef __SPLITTER_ORACLE_FUZZ_CHECK__
#define __SPLITTER_ORACLE_FUZZ_CHECK__

#include "java/lang/String.h"

class PolyhedralBoundedSolid;
class InfinitePlane;

/**
Java's `SplitterOracleFuzzTest.check`, shared with
`SplitterGeneralPlaneOracleFuzzTest`: splits a copy of `base` and returns
the reason the result is wrong. C++ port note: Java returns null for a
correct case; here the empty string stands for null. The copy and every
piece are released.
*/
java::String splitterOracleCheck(PolyhedralBoundedSolid* base,
    const InfinitePlane& plane);

#endif
