#include <cfloat>
#include <cmath>
#include <cstdio>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "SplitterOracleFuzzCheck.h"
#include "ThrowableSimpleName.h"

/*
Seeded oracle fuzz for the splitter (Fable5.2Analysis.md Stage E). Every
solid of the reference corpus is split by planes through its own vertices:
random normals, axis normals and diagonal normals. The L-shaped hollow
brick also gets 40 planes through its two reflex vertices, where a face is
crossed twice at the same vertex.

A case is correct when the pieces conserve the volume, each piece is on
the side its list names, each is strict-valid, and no piece is empty while
the plane really cuts the solid. The count of correct cases is a ratchet:
raise it when a fix lands, never lower it.

When the section is pinched at a reflex vertex (two section regions meeting
at the vertex), the splitter splits the self-touching section loop into
two faces; regions that meet only at the vertex come out as separate
solids.

Traceability: [MANT1988] Ch. 14, Sections 14.4-14.8.
C++ counterpart of Java's `SplitterOracleFuzzTest`.
*/
namespace {

/**
Before Stage E: 37 / 256. Stage E: 252 / 256. Pinched sections resolved
(2026-11-16): 256 / 256.
*/
const int MINIMUM_CORRECT = 256;
const double TOLERANCE = 1.0e-6;

java::String vectorText(const Vector3Dd& v)
{
    return java::String::format("(%g, %g, %g)", v.x(), v.y(), v.z());
}

java::String byReasonText(const java::TreeMap<java::String, int>& byReason)
{
    java::String text("{");
    for ( long i = 0; i < byReason.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += byReason.getKey(i) + "=" +
            java::String::valueOf(byReason.getValue(i));
    }
    return text + "}";
}

struct ReflexPlane
{
    Vector3Dd normal;
    Vector3Dd point;
};

}

java::String splitterOracleCheck(PolyhedralBoundedSolid* base,
    const InfinitePlane& plane)
{
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidStructuralCopy::copy(base);
    java::ArrayList<PolyhedralBoundedSolid*> above;
    java::ArrayList<PolyhedralBoundedSolid*> below;
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(base);
    double min = DBL_MAX;
    double max = -DBL_MAX;
    double total = 0.0;
    int i;

    for ( i = 0; i < base->getVerticesList().size(); i++ ) {
        double d = plane.pointDistance(
            base->getVerticesList().get(i)->position);
        min = std::fmin(min, d);
        max = std::fmax(max, d);
    }
    bool cuts = min < -TOLERANCE && max > TOLERANCE;
    try {
        PolyhedralBoundedSolidModeler::split(solid, plane, above, below);
    }
    catch ( const java::Throwable& t ) {
        delete solid;
        for ( long j = 0; j < above.size(); j++ ) delete above[j];
        for ( long j = 0; j < below.size(); j++ ) delete below[j];
        return java::String("exception ") + throwableSimpleName(t);
    }
    bool strict = true;
    bool empty = false;
    bool sides = true;
    for ( int side = 1; side >= -1; side -= 2 ) {
        java::ArrayList<PolyhedralBoundedSolid*>& pieces =
            side > 0 ? above : below;
        for ( long j = 0; j < pieces.size(); j++ ) {
            PolyhedralBoundedSolid* piece = pieces[j];
            java::String message;
            strict &= PolyhedralBoundedSolidValidationEngine::validateStrict(
                piece, &message);
            total += PolyhedralBoundedSolidVolumeOracle::solidVolume(piece);
            empty |= cuts && piece->getPolygonsList().size() == 0;
            for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
                if ( side * plane.pointDistance(piece->getVerticesList()
                         .get(i)->position) < -TOLERANCE ) {
                    sides = false;
                }
            }
        }
    }
    // C++ ownership: the copy and the pieces are released
    delete solid;
    for ( long j = 0; j < above.size(); j++ ) delete above[j];
    for ( long j = 0; j < below.size(); j++ ) delete below[j];
    if ( std::fabs(total - volume) > TOLERANCE * std::fmax(1.0, volume) ) {
        return "volume";
    }
    if ( empty ) {
        return "empty piece";
    }
    if ( !sides ) {
        return "side";
    }
    if ( !strict ) {
        return "strict";
    }
    return "";
}

TEST(SplitterOracleFuzzTest, CorpusSolidsAndPlanesThroughVerticesSplitCorrectCountDoesNotRegress) {
    java::Random random(7);
    java::ArrayList<java::String> failures;
    java::TreeMap<java::String, int> byReason;
    java::ArrayList<ReflexPlane> reflexPlanes;
    Vector3Dd reflexVertices[2] = {
        Vector3Dd(0.2, 0.8, 0), Vector3Dd(0.2, 0.8, 0.2) };
    int total = 0;
    int correct = 0;
    int k;

    for ( k = 0; k < 40; k++ ) {
        // Java evaluates the arguments left to right
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = 0.5 * random.nextGaussian();
        ReflexPlane plane = { Vector3Dd(x, y, z), reflexVertices[k % 2] };
        reflexPlanes.add(plane);
    }
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        java::ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        for ( int which = 0; which < 2; which++ ) {
            java::String name = java::String(csgSampleCorpusName(sample)) +
                (which == 0 ? "/A" : "/B");
            PolyhedralBoundedSolid* base = pair[which];
            bool lShape = name == "HOLLOW_BRICK/A";
            int vertexCount = base->getVerticesList().size();
            for ( k = 0; k < (lShape ? 52 : 12); k++ ) {
                Vector3Dd p = base->getVerticesList().get(
                    random.nextInt(vertexCount))->position;
                Vector3Dd n;
                if ( k >= 12 ) {
                    n = reflexPlanes.get(k - 12).normal;
                    p = reflexPlanes.get(k - 12).point;
                }
                else if ( k % 3 == 0 ) {
                    double x = random.nextGaussian();
                    double y = random.nextGaussian();
                    double z = random.nextGaussian();
                    n = Vector3Dd(x, y, z);
                }
                else if ( k % 3 == 1 ) {
                    int axis = random.nextInt(3);
                    n = Vector3Dd(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0,
                        axis == 2 ? 1 : 0);
                }
                else {
                    int x = random.nextInt(3) - 1;
                    int y = random.nextInt(3) - 1;
                    n = Vector3Dd(x, y, 1);
                }
                if ( n.length() < 1.0e-6 ) {
                    continue;
                }
                total++;
                java::String reason = splitterOracleCheck(base,
                    InfinitePlane(n.normalized(), p));
                if ( reason.length() == 0 ) {
                    correct++;
                }
                else {
                    byReason.put(reason, byReason.getOrDefault(reason, 0) + 1);
                    failures.add(name + " k=" + java::String::valueOf(k) +
                        " " + reason + " n=" + vectorText(n) + " p=" +
                        vectorText(p));
                }
            }
        }
        delete pair[0];
        delete pair[1];
    }
    printf("split fuzz: %d / %d %s\n", correct, total,
        byReasonText(byReason).c_str());
    for ( long i = 0; i < failures.size(); i++ ) {
        printf("  %s\n", failures[i].c_str());
    }
    EXPECT_GE(correct, MINIMUM_CORRECT)
        << "correct split results must not regress";
}
