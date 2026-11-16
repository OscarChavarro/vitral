// C++ port of Java's SplitterOracleFuzzTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "SplitterOracleFuzz.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

/**
Before Stage E: 37 / 256. Stage E: 252 / 256. Pinched sections
resolved (2026-11-16): 256 / 256.
*/
static const int MINIMUM_CORRECT = 256;

/** Java `TreeMap<String, Integer>.merge(reason, 1, Integer::sum)`. */
static void
countReason(ArrayList<String>& reasons, ArrayList<int>& counts,
    const String& reason)
{
    long i;

    for ( i = 0; i < reasons.size(); i++ ) {
        if ( reasons[i] == reason ) {
            counts[i]++;
            return;
        }
    }
    // Sorted insertion, as the keys of a TreeMap
    for ( i = 0; i < reasons.size() && reasons[i] < reason;
          i++ ) {
    }
    reasons.add(i, reason);
    counts.add(i, 1);
}

static String
reasonsText(const ArrayList<String>& reasons, const ArrayList<int>& counts)
{
    String text = "{";
    char number[32];
    long i;

    for ( i = 0; i < reasons.size(); i++ ) {
        if ( i > 0 ) {
            text = text + ", ";
        }
        sprintf(number, "%d", counts[i]);
        text = text + reasons[i] + "=" + number;
    }
    return text + "}";
}

static String
vectorText(const Vector3Dd& v)
{
    char text[100];

    sprintf(text, "(%.17g, %.17g, %.17g)", v.x(), v.y(), v.z());
    return text;
}

TEST(SplitterOracleFuzz, given_corpusSolidsAndPlanesThroughVertices_when_split_then_correctCountDoesNotRegress)
{
    Random random(7);
    ArrayList<String> failures;
    ArrayList<String> reasons;
    ArrayList<int> counts;
    ArrayList<Vector3Dd> reflexNormals;
    ArrayList<Vector3Dd> reflexPoints;
    Vector3Dd reflexVertices[2];
    int total = 0;
    int correct = 0;
    int k;
    int sampleIndex;
    long f;

    reflexVertices[0] = Vector3Dd(0.2, 0.8, 0);
    reflexVertices[1] = Vector3Dd(0.2, 0.8, 0.2);
    for ( k = 0; k < 40; k++ ) {
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = 0.5 * random.nextGaussian();
        reflexNormals.add(Vector3Dd(x, y, z));
        reflexPoints.add(reflexVertices[k % 2]);
    }
    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        int which;
        for ( which = 0; which < 2; which++ ) {
            String name = String(csgSampleCorpusName(sample)) +
                (which == 0 ? "/A" : "/B");
            PolyhedralBoundedSolid* base = pair[which];
            BOOLEAN lShape = name == "HOLLOW_BRICK/A";
            int vertexCount = (int)base->getVerticesList().size();
            for ( k = 0; k < (lShape ? 52 : 12); k++ ) {
                Vector3Dd p = base->getVerticesList().get(
                    random.nextInt(vertexCount))->position;
                Vector3Dd n;
                if ( k >= 12 ) {
                    n = reflexNormals[k - 12];
                    p = reflexPoints[k - 12];
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
                String reason = SplitterOracleFuzz::check(base,
                    InfinitePlane(n.normalized(), p));
                if ( reason.length() == 0 ) {
                    correct++;
                }
                else {
                    char number[32];
                    sprintf(number, "%d", k);
                    countReason(reasons, counts, reason);
                    failures.add(name + " k=" + number + " " + reason +
                        " n=" + vectorText(n) + " p=" + vectorText(p));
                }
            }
        }
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
    }

    printf("split fuzz: %d / %d %s\n", correct, total,
        reasonsText(reasons, counts).c_str());
    for ( f = 0; f < failures.size(); f++ ) {
        printf("  %s\n", failures[f].c_str());
    }
    // correct split results must not regress
    EXPECT_GE(correct, MINIMUM_CORRECT);
}

VITRAL_TEST_MAIN()
