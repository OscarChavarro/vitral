#include <cstdint>
#include <cstring>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidRepair.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"

/*
Validation only reads (plan-stage11 Phase C). Until then every validation
rebuilt the vertex back-pointers and removed the vertices it found without
half-edges, so a validator could turn an invalid solid into a valid one and
report it as valid.

The snapshot compared here is the whole structure: the order of the face,
loop, half-edge, edge and vertex lists, every link between nodes, every id
and every position.
C++ counterpart of Java's `ValidationDoesNotModifyTest`.
*/
namespace {

typedef PolyhedralBoundedSolidValidationEngine Validation;

/** Java `Double.doubleToLongBits`, as text. */
java::String bits(double value)
{
    int64_t b;
    std::memcpy(&b, &value, sizeof(b));
    return java::String::format("%lld", (long long)b);
}

java::String halfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    return he == nullptr ? java::String("null") :
        java::String("h") + java::String::valueOf(he->id);
}

/** Every list order, link, id and position of the solid, as text. */
java::String snapshot(PolyhedralBoundedSolid* solid)
{
    java::String text;
    int i;
    int j;
    int k;
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        text += java::String("F") + java::String::valueOf(face->id) +
            (face->parentSolid == solid ? "" : " foreign");
        for ( j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            text += java::String(" L[start=") +
                halfEdge(loop->boundaryStartHalfEdge) +
                (loop->parentFace == face ? "" : " foreign");
            for ( k = 0; k < loop->halfEdgesList.size(); k++ ) {
                _PolyhedralBoundedSolidHalfEdge* he =
                    loop->halfEdgesList.get(k);
                text += java::String(" ") + halfEdge(he) +
                    (he->parentLoop == loop ? "" : "!") + "v" +
                    (he->startingVertex == nullptr ? java::String("null") :
                        java::String::valueOf(he->startingVertex->id)) +
                    "e" + (he->parentEdge == nullptr ? java::String("null") :
                        java::String::valueOf(he->parentEdge->id));
            }
            text += "]";
        }
        text += "\n";
    }
    for ( i = 0; i < solid->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        text += java::String("E") + java::String::valueOf(edge->id) + " " +
            halfEdge(edge->rightHalf) + " " + halfEdge(edge->leftHalf) + "\n";
    }
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = solid->getVerticesList().get(i);
        text += java::String("V") + java::String::valueOf(v->id) + " " +
            halfEdge(v->emanatingHalfEdge) + " " + bits(v->position.x()) +
            " " + bits(v->position.y()) + " " + bits(v->position.z()) + "\n";
    }
    text += java::String("maxF=") +
        java::String::valueOf(solid->getMaxFaceId()) + " maxV=" +
        java::String::valueOf(solid->getMaxVertexId());
    return text;
}

void assertUntouched(const java::String& name, PolyhedralBoundedSolid* solid,
    PolyhedralBoundedSolid* other)
{
    java::String before = snapshot(solid);
    java::String otherBefore = snapshot(other);
    java::String message;
    Validation::validateIntermediate(solid);
    EXPECT_STREQ(before.c_str(), snapshot(solid).c_str())
        << name.c_str() << " validateIntermediate";
    Validation::validateStrict(solid);
    EXPECT_STREQ(before.c_str(), snapshot(solid).c_str())
        << name.c_str() << " validateStrict";
    Validation::validateStrict(solid, &message);
    EXPECT_STREQ(before.c_str(), snapshot(solid).c_str())
        << name.c_str() << " validateStrict with message";
    java::String inputs;
    Validation::validateBooleanInputs(solid, other, &inputs);
    EXPECT_STREQ(before.c_str(), snapshot(solid).c_str())
        << name.c_str() << " validateBooleanInputs";
    EXPECT_STREQ(otherBefore.c_str(), snapshot(other).c_str())
        << name.c_str() << " validateBooleanInputs, other";
}

}

TEST(ValidationDoesNotModifyTest, ValidSolidsEveryValidatorRunsNothingChanges) {
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        java::ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        assertUntouched(java::String(csgSampleCorpusName(sample)) + "/A",
            pair[0], pair[1]);
        assertUntouched(java::String(csgSampleCorpusName(sample)) + "/B",
            pair[1], pair[0]);
        delete pair[0];
        delete pair[1];
    }
}

TEST(ValidationDoesNotModifyTest, BooleanResultEveryValidatorRunsNothingChanges) {
    PolyhedralBoundedSolid* star = PrismFixtures::prism(
        PrismFixtures::regularPolygon(5, 1.0), 1.0);
    PolyhedralBoundedSolid* prism = PrismFixtures::prism(
        PrismFixtures::regularPolygon(9, 0.8), 1.2);
    PolyhedralBoundedSolidModeler::applyTransformation(prism,
        PolyhedralBoundedSolidVolumeOracle::translation(0.2, 0.1, 0.3)
            .multiply(PolyhedralBoundedSolidVolumeOracle::rotation(0.6,
                1, 2, 3)));
    PolyhedralBoundedSolid* result = PolyhedralBoundedSolidModeler::setOp(
        star, prism, PolyhedralBoundedSolidModeler::SUBTRACT);
    assertUntouched("prism - prism", result, star);
    delete result;
    delete star;
    delete prism;
}

/*
A vertex whose emanating half-edge was lost is reported as invalid and left
as it is; the explicit repair fixes it and says so.
*/
TEST(ValidationDoesNotModifyTest, BrokenBackPointerValidatedReportedAndNotRepaired) {
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(
        PrismFixtures::regularPolygon(6, 1.0), 1.0);
    _PolyhedralBoundedSolidVertex* vertex = solid->getVerticesList().get(3);
    vertex->emanatingHalfEdge = nullptr;
    java::String broken = snapshot(solid);
    java::String message;
    EXPECT_FALSE(Validation::validateIntermediate(solid));
    EXPECT_FALSE(Validation::validateStrict(solid, &message));
    EXPECT_EQ(nullptr, vertex->emanatingHalfEdge);
    EXPECT_STREQ(broken.c_str(), snapshot(solid).c_str());
    PolyhedralBoundedSolidRepair::Report report =
        PolyhedralBoundedSolidRepair::remakeEmanatingHalfEdgeReferences(
            solid);
    EXPECT_EQ(1, report.getRelinkedVertices());
    EXPECT_EQ(0, report.getRemovedVertices());
    EXPECT_NE(nullptr, vertex->emanatingHalfEdge);
    java::String after;
    EXPECT_TRUE(Validation::validateStrict(solid, &after));
    EXPECT_TRUE(PolyhedralBoundedSolidRepair
        ::remakeEmanatingHalfEdgeReferences(solid).isClean());
    delete solid;
}

/*
validateBooleanInputs used to weld the coincident vertices it found. Now an
operand with an edge of zero length is reported and left alone.
*/
TEST(ValidationDoesNotModifyTest, CoincidentVerticesBooleanInputsAreValidatedReportedAndNotWelded) {
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(
        PrismFixtures::regularPolygon(6, 1.0), 1.0);
    PolyhedralBoundedSolid* other = PrismFixtures::prism(
        PrismFixtures::regularPolygon(4, 1.0), 1.0);
    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(0);
    _PolyhedralBoundedSolidHalfEdge* he = edge->rightHalf;
    PolyhedralBoundedSolidEulerOperators::lmev(solid, he,
        he->mirrorHalfEdge()->next(), solid->getMaxVertexId() + 1,
        he->startingVertex->position);
    java::String before = snapshot(solid);
    java::String message;
    EXPECT_FALSE(Validation::validateBooleanInputs(solid, other, &message));
    EXPECT_TRUE(message.contains("coincident vertices")) << message.c_str();
    EXPECT_STREQ(before.c_str(), snapshot(solid).c_str());
    delete solid;
    delete other;
}
