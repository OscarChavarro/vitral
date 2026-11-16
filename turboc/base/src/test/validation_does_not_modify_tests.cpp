// C++ port of Java's ValidationDoesNotModifyTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/Cpp98Support.h"
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

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidValidationEngine Validation;

static String
str(long value)
{
    return Cpp98Support::toString(value);
}

/** Java `Double.doubleToLongBits`: the bytes of the double, in hex. */
static String
bitsOf(double value)
{
    unsigned char bytes[sizeof(double)];
    char text[2 * sizeof(double) + 1];
    unsigned i;

    memcpy(bytes, &value, sizeof(double));
    for ( i = 0; i < sizeof(double); i++ ) {
        sprintf(text + 2 * i, "%02x", (unsigned)bytes[i]);
    }
    return text;
}

static String
halfEdge(_PolyhedralBoundedSolidHalfEdge* he)
{
    if ( he == NULL ) {
        return "null";
    }
    return String("h") + str(he->id);
}

/** Every list order, link, id and position of the solid, as text. */
static String
snapshot(PolyhedralBoundedSolid* solid)
{
    String text;
    long i;
    long j;
    long k;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        text = text + "F" + str(face->id);
        if ( face->parentSolid != solid ) {
            text = text + " foreign";
        }
        for ( j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            text = text + " L[start=" + halfEdge(loop->boundaryStartHalfEdge);
            if ( loop->parentFace != face ) {
                text = text + " foreign";
            }
            for ( k = 0; k < loop->halfEdgesList.size(); k++ ) {
                _PolyhedralBoundedSolidHalfEdge* he =
                    loop->halfEdgesList.get(k);
                text = text + " " + halfEdge(he);
                if ( he->parentLoop != loop ) {
                    text = text + "!";
                }
                text = text + "v";
                if ( he->startingVertex == NULL ) {
                    text = text + "null";
                }
                else {
                    text = text + str(he->startingVertex->id);
                }
                text = text + "e";
                if ( he->parentEdge == NULL ) {
                    text = text + "null";
                }
                else {
                    text = text + str(he->parentEdge->id);
                }
            }
            text = text + "]";
        }
        text = text + "\n";
    }
    for ( i = 0; i < solid->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        text = text + "E" + str(edge->id) + " " + halfEdge(edge->rightHalf) +
            " " + halfEdge(edge->leftHalf) + "\n";
    }
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* v = solid->getVerticesList().get(i);
        text = text + "V" + str(v->id) + " " +
            halfEdge(v->emanatingHalfEdge) + " " +
            bitsOf(v->position.x()) + " " + bitsOf(v->position.y()) + " " +
            bitsOf(v->position.z()) + "\n";
    }
    text = text + "maxF=" + str(solid->getMaxFaceId()) + " maxV=" +
        str(solid->getMaxVertexId());
    return text;
}

static void
assertUntouched(const String& name, PolyhedralBoundedSolid* solid,
    PolyhedralBoundedSolid* other)
{
    String before = snapshot(solid);
    String otherBefore = snapshot(other);
    String message;

    printf("%s\n", name.c_str());
    Validation::validateIntermediate(solid);
    // validateIntermediate
    EXPECT_TRUE(snapshot(solid) == before);
    Validation::validateStrict(solid);
    // validateStrict
    EXPECT_TRUE(snapshot(solid) == before);
    Validation::validateStrict(solid, &message);
    // validateStrict with message
    EXPECT_TRUE(snapshot(solid) == before);
    message = "";
    Validation::validateBooleanInputs(solid, other, &message);
    // validateBooleanInputs
    EXPECT_TRUE(snapshot(solid) == before);
    // validateBooleanInputs, other
    EXPECT_TRUE(snapshot(other) == otherBefore);
}

TEST(ValidationDoesNotModify, given_validSolids_when_everyValidatorRuns_then_nothingChanges)
{
    int sampleIndex;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        assertUntouched(String(csgSampleCorpusName(sample)) + "/A", pair[0],
            pair[1]);
        assertUntouched(String(csgSampleCorpusName(sample)) + "/B", pair[1],
            pair[0]);
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
    }
}

TEST(ValidationDoesNotModify, given_booleanResult_when_everyValidatorRuns_then_nothingChanges)
{
    PolyhedralBoundedSolid* star = PrismFixtures::prism(
        PrismFixtures::regularPolygon(5, 1.0), 1.0);
    PolyhedralBoundedSolid* prism = PrismFixtures::prism(
        PrismFixtures::regularPolygon(9, 0.8), 1.2);
    Modeler::applyTransformation(prism,
        Oracle::translation(0.2, 0.1, 0.3)
            .multiply(Oracle::rotation(0.6, 1, 2, 3)));
    PolyhedralBoundedSolid* result = Modeler::setOp(star, prism,
        Modeler::SUBTRACT);

    assertUntouched("prism - prism", result, star);
    delete star;
    delete prism;
    delete result;
}

/**
A vertex whose emanating half-edge was lost is reported as invalid and
left as it is; the explicit repair fixes it and says so.
*/
TEST(ValidationDoesNotModify, given_brokenBackPointer_when_validated_then_reportedAndNotRepaired)
{
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(
        PrismFixtures::regularPolygon(6, 1.0), 1.0);
    _PolyhedralBoundedSolidVertex* vertex = solid->getVerticesList().get(3);
    vertex->emanatingHalfEdge = NULL;
    String broken = snapshot(solid);
    String message;

    EXPECT_FALSE(Validation::validateIntermediate(solid));
    EXPECT_FALSE(Validation::validateStrict(solid, &message));
    EXPECT_TRUE(vertex->emanatingHalfEdge == NULL);
    EXPECT_TRUE(snapshot(solid) == broken);

    PolyhedralBoundedSolidRepair::Report report =
        PolyhedralBoundedSolidRepair::remakeEmanatingHalfEdgeReferences(solid);

    EXPECT_EQ(1, report.getRelinkedVertices());
    EXPECT_EQ(0, report.getRemovedVertices());
    EXPECT_TRUE(vertex->emanatingHalfEdge != NULL);
    message = "";
    EXPECT_TRUE(Validation::validateStrict(solid, &message));
    EXPECT_TRUE(PolyhedralBoundedSolidRepair
        ::remakeEmanatingHalfEdgeReferences(solid).isClean());
    delete solid;
}

/**
validateBooleanInputs used to weld the coincident vertices it found.
Now an operand with an edge of zero length is reported and left alone.
*/
TEST(ValidationDoesNotModify, given_coincidentVertices_when_booleanInputsAreValidated_then_reportedAndNotWelded)
{
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(
        PrismFixtures::regularPolygon(6, 1.0), 1.0);
    PolyhedralBoundedSolid* other = PrismFixtures::prism(
        PrismFixtures::regularPolygon(4, 1.0), 1.0);
    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(0);
    _PolyhedralBoundedSolidHalfEdge* he = edge->rightHalf;
    PolyhedralBoundedSolidEulerOperators::lmev(solid, he,
        he->mirrorHalfEdge()->next(), solid->getMaxVertexId() + 1,
        he->startingVertex->position);
    String before = snapshot(solid);
    String message;

    EXPECT_FALSE(Validation::validateBooleanInputs(solid, other, &message));

    EXPECT_TRUE(message.indexOf(String("coincident vertices")) >= 0);
    EXPECT_TRUE(snapshot(solid) == before);
    delete solid;
    delete other;
}

VITRAL_TEST_MAIN()
