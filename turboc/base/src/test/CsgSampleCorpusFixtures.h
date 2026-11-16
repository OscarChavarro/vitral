#ifndef __CSG_SAMPLE_CORPUS_FIXTURES__
#define __CSG_SAMPLE_CORPUS_FIXTURES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "CsgSampleCorpus.h"

class PolyhedralBoundedSolid;

/**
Operand pairs of the `CsgSampleCorpus` samples. C++ counterpart of Java's
`CsgSampleCorpusFixtures` (test code); the pairs are lists of two new
solids, owned by the caller.
*/
class CsgSampleCorpusFixtures
{
public:
    static ArrayList<PolyhedralBoundedSolid*> createPair(
        CsgSampleCorpus sample);

private:
    CsgSampleCorpusFixtures();
    static ArrayList<PolyhedralBoundedSolid*> createMoonBlockPair();
    static PolyhedralBoundedSolid* createTranslatedCylinder(double radius,
        double height);
    static ArrayList<PolyhedralBoundedSolid*> createStackedBlocksPair();
    static ArrayList<PolyhedralBoundedSolid*> createHollowBrickPair();
    static ArrayList<PolyhedralBoundedSolid*> createCrossPair();
    static PolyhedralBoundedSolid* createTranslatedBox(double sx, double sy,
        double sz, double tx, double ty, double tz);
};

#endif
