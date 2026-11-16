#ifndef __CSG_SAMPLE_CORPUS_FIXTURES__
#define __CSG_SAMPLE_CORPUS_FIXTURES__

#include "java/util/ArrayList.h"
#include "CsgSampleCorpus.h"

class PolyhedralBoundedSolid;

/**
Builds reusable operand pairs for the boolean set-operation corpus.

Traceability: [MANT1988] Ch. 12 sweep/generator examples for primitive
construction and Ch. 15 boolean examples for pairwise set-operation
scenarios.
C++ counterpart of Java's `CsgSampleCorpusFixtures` (test code): the two
operands are new solids owned by the caller.
*/
class CsgSampleCorpusFixtures
{
public:
    static java::ArrayList<PolyhedralBoundedSolid*> createPair(
        CsgSampleCorpus sample);

private:
    CsgSampleCorpusFixtures();
    static java::ArrayList<PolyhedralBoundedSolid*> createMoonBlockPair();
    static PolyhedralBoundedSolid* createTranslatedCylinder(double radius,
        double height);
    static java::ArrayList<PolyhedralBoundedSolid*> createStackedBlocksPair();
    static java::ArrayList<PolyhedralBoundedSolid*> createHollowBrickPair();
    static java::ArrayList<PolyhedralBoundedSolid*> createCrossPair();
    static PolyhedralBoundedSolid* createTranslatedBox(double sx, double sy,
        double sz, double tx, double ty, double tz);
};

#endif
