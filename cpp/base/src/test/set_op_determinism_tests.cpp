#include <cstdint>
#include <cstring>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "ThrowableSimpleName.h"

/*
Determinism (plan-stage11 Phase B): the same operation on the same operands
gives the same solid, whatever ran before in the process. "Same" means the
same topology summary and the same vertex positions in the same order, bit
for bit; a typed failure must repeat as the same failure.

The kernel keeps global counters (ids) and per-operation state; a result
that depends on them shows up here as a difference between two runs.
C++ counterpart of Java's `SetOpDeterminismTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;

/** Java `Double.doubleToLongBits`. */
long long doubleToLongBits(double value)
{
    int64_t bits;
    std::memcpy(&bits, &value, sizeof(bits));
    return (long long)bits;
}

/** Releases a, b and the result (Java: garbage collector). */
java::String run(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b,
    int op)
{
    PolyhedralBoundedSolid* result;
    try {
        result = op == BooleanOracleFuzz::B_MINUS_A ?
            Modeler::setOp(b, a, Modeler::SUBTRACT) :
            Modeler::setOp(a, b, op);
    }
    catch ( const PolyhedralBoundedSolidOperationException& e ) {
        delete a;
        delete b;
        return java::String("THROWS ") + throwableSimpleName(e);
    }
    delete a;
    delete b;
    java::String text = PolyhedralBoundedSolidTopologySummary::from(result)
        .toString();
    int i;
    for ( i = 0; i < result->getVerticesList().size(); i++ ) {
        Vector3Dd p = result->getVerticesList().get(i)->position;
        text += java::String("\n") +
            java::String::format("%lld", doubleToLongBits(p.x())) + " " +
            java::String::format("%lld", doubleToLongBits(p.y())) + " " +
            java::String::format("%lld", doubleToLongBits(p.z()));
    }
    for ( i = 0; i < result->getPolygonsList().size(); i++ ) {
        text += java::String("\n") + java::String::valueOf(
            (int)result->getPolygonsList().get(i)->boundariesList.size());
    }
    delete result;
    return text;
}

java::String prismFingerprint(int op)
{
    PolyhedralBoundedSolid* a = PrismFixtures::prism(
        PrismFixtures::starPolygon(5, 1.0, 0.45), 1.0);
    PolyhedralBoundedSolid* b = PrismFixtures::prism(
        PrismFixtures::regularPolygon(9, 0.8), 1.2);
    Modeler::applyTransformation(b,
        PolyhedralBoundedSolidVolumeOracle::translation(0.2, 0.1, 0.3)
            .multiply(PolyhedralBoundedSolidVolumeOracle::rotation(0.6,
                1, 2, 3)));
    return run(a, b, op);
}

java::String fingerprint(CsgSampleCorpus sample, int op)
{
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(sample);
    return run(pair[0], pair[1], op);
}

java::String join(const java::ArrayList<java::String>& list)
{
    java::String text("[");
    for ( long i = 0; i < list.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += list[i];
    }
    return text + "]";
}

}

TEST(SetOpDeterminismTest, ReferencePairsEachOperationRunsTwiceResultsAreIdentical) {
    java::ArrayList<java::String> differences;
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        for ( int k = 0; k < 4; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            java::String first = fingerprint(sample, op);
            // Unrelated work in between: other operations move the
            // global id counters and leave their own state behind.
            fingerprint(CSG_SAMPLE_CORPUS_VALUES[
                (s + 1) % CSG_SAMPLE_CORPUS_COUNT], Modeler::UNION);
            java::String second = fingerprint(sample, op);
            if ( !(first == second) ) {
                differences.add(java::String(csgSampleCorpusName(sample)) +
                    " " + BooleanOracleFuzz::opName(op));
            }
        }
    }
    EXPECT_TRUE(differences.isEmpty())
        << "operations whose result changed between two runs: "
        << join(differences).c_str();
}

TEST(SetOpDeterminismTest, StarPrismsEachOperationRunsTwiceResultsAreIdentical) {
    java::ArrayList<java::String> differences;
    for ( int k = 0; k < 4; k++ ) {
        int op = BooleanOracleFuzz::OPERATIONS[k];
        java::String first = prismFingerprint(op);
        java::String second = prismFingerprint(op);
        if ( !(first == second) ) {
            differences.add(BooleanOracleFuzz::opName(op));
        }
    }
    EXPECT_TRUE(differences.isEmpty()) << join(differences).c_str();
}
