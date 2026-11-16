// C++ port of Java's PolyhedralBoundedSolidFaceGeometryTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

static const double TOLERANCE = 1.0e-5;

static double
maximumOf(double a, double b)
{
    return a > b ? a : b;
}

/**
Mixes points around vertices (to exercise the LIMIT tolerances), random
points in the solid box, and points far outside it.
*/
static Vector3Dd
samplePoint(Random& random, PolyhedralBoundedSolid* solid, double* box,
    int k)
{
    double size = maximumOf(box[3] - box[0],
        maximumOf(box[4] - box[1], box[5] - box[2]));
    if ( k % 3 == 0 ) {
        Vector3Dd v = solid->getVerticesList().get(
            random.nextInt((int)solid->getVerticesList().size()))->position;
        double r = (random.nextInt(4) + 1) * TOLERANCE;
        // Java evaluates the arguments from left to right
        double gx = random.nextGaussian();
        double gy = random.nextGaussian();
        double gz = random.nextGaussian();
        return v.add(Vector3Dd(gx, gy, gz).multiply(r));
    }
    double grow = k % 3 == 1 ? 0.1 : 2.0;
    double x = box[0] - grow * size + random.nextDouble() * (1 + 2 * grow) *
        (box[3] - box[0]);
    double y = box[1] - grow * size + random.nextDouble() * (1 + 2 * grow) *
        (box[4] - box[1]);
    double z = box[2] - grow * size + random.nextDouble() * (1 + 2 * grow) *
        (box[5] - box[2]);
    return Vector3Dd(x, y, z);
}

TEST(PolyhedralBoundedSolidFaceGeometry, given_corpusFaces_when_boxRejects_then_pointInFaceSaysOutside)
{
    Random random(11);
    int rejected = 0;
    int accepted = 0;
    int sampleIndex;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        long s;
        for ( s = 0; s < pair.size(); s++ ) {
            PolyhedralBoundedSolid* solid = pair[s];
            double* box = solid->getMinMax();
            long i;
            for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace* face =
                    solid->getPolygonsList().get(i);
                PolyhedralBoundedSolidFaceGeometry* geometry =
                    PolyhedralBoundedSolidFaceGeometry::of(face);
                if ( geometry->getPlane() == NULL ) {
                    delete geometry;
                    continue;
                }
                int k;
                for ( k = 0; k < 200; k++ ) {
                    Vector3Dd p = samplePoint(random, solid, box, k);
                    p = geometry->getPlane()->projectPoint(p);
                    int status = face->testPointInside(p, TOLERANCE,
                        geometry->getPlane());
                    if ( geometry->projectionExcludes(p, TOLERANCE) ) {
                        rejected++;
                        if ( status != Geometry::OUTSIDE ) {
                            printf("%s face %d point (%g, %g, %g)\n",
                                csgSampleCorpusName(sample), face->id,
                                p.x(), p.y(), p.z());
                        }
                        EXPECT_EQ((int)Geometry::OUTSIDE, status);
                    }
                    if ( status != Geometry::OUTSIDE ) {
                        accepted++;
                    }
                }
                delete geometry;
            }
            delete[] box;
        }
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
    }
    // the box test must reject something
    EXPECT_GT(rejected, 1000);
    // the sample must hit faces
    EXPECT_GT(accepted, 100);
}

VITRAL_TEST_MAIN()
