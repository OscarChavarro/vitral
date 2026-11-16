#include <cmath>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"

/*
The box tests of the face geometry snapshot (Fable5.2Analysis.md Stage G)
are only allowed to skip work: whenever `projectionExcludes` answers
true, the point-in-face test must answer OUTSIDE, and whenever the
point-in-face test accepts a point, that point lies inside the face box
grown by `containmentMargin`. Checked on every face of the reference
corpus with points on the face plane, near its vertices and edges, and far
away.
C++ counterpart of Java's `PolyhedralBoundedSolidFaceGeometryTest`.
*/
namespace {

const double TOLERANCE = 1.0e-5;

/**
Mixes points around vertices (to exercise the LIMIT tolerances), random
points in the solid box, and points far outside it.
*/
Vector3Dd samplePoint(java::Random& random, PolyhedralBoundedSolid* solid,
    const double* box, int k)
{
    double size = std::fmax(box[3] - box[0],
        std::fmax(box[4] - box[1], box[5] - box[2]));
    if ( k % 3 == 0 ) {
        Vector3Dd v = solid->getVerticesList().get(
            random.nextInt(solid->getVerticesList().size()))->position;
        double r = (random.nextInt(4) + 1) * TOLERANCE;
        // Java evaluates the arguments left to right
        double gx = random.nextGaussian();
        double gy = random.nextGaussian();
        double gz = random.nextGaussian();
        return v.add(Vector3Dd(gx, gy, gz).multiply(r));
    }
    double grow = k % 3 == 1 ? 0.1 : 2.0;
    double x = box[0] - grow * size + random.nextDouble() *
        (1 + 2 * grow) * (box[3] - box[0]);
    double y = box[1] - grow * size + random.nextDouble() *
        (1 + 2 * grow) * (box[4] - box[1]);
    double z = box[2] - grow * size + random.nextDouble() *
        (1 + 2 * grow) * (box[5] - box[2]);
    return Vector3Dd(x, y, z);
}

}

TEST(PolyhedralBoundedSolidFaceGeometryTest, CorpusFacesBoxRejectsThenPointInFaceSaysOutside) {
    java::Random random(11);
    int rejected = 0;
    int accepted = 0;

    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        java::ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        for ( long w = 0; w < pair.size(); w++ ) {
            PolyhedralBoundedSolid* solid = pair[w];
            double* box = solid->getMinMax();
            for ( int i = 0; i < solid->getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace* face =
                    solid->getPolygonsList().get(i);
                PolyhedralBoundedSolidFaceGeometry* geometry =
                    PolyhedralBoundedSolidFaceGeometry::of(face);
                if ( geometry->getPlane() == nullptr ) {
                    delete geometry;
                    continue;
                }
                for ( int k = 0; k < 200; k++ ) {
                    Vector3Dd p = samplePoint(random, solid, box, k);
                    p = geometry->getPlane()->projectPoint(p);
                    int status = face->testPointInside(p, TOLERANCE,
                        geometry->getPlane());
                    if ( geometry->projectionExcludes(p, TOLERANCE) ) {
                        rejected++;
                        EXPECT_EQ(Geometry::OUTSIDE, status)
                            << csgSampleCorpusName(sample) << " face "
                            << face->id << " point " << p.x() << ", "
                            << p.y() << ", " << p.z();
                    }
                    if ( status != Geometry::OUTSIDE ) {
                        accepted++;
                    }
                }
                delete geometry;
            }
            delete[] box;
            delete solid;
        }
    }
    EXPECT_GT(rejected, 1000) << "the box test must reject something";
    EXPECT_GT(accepted, 100) << "the sample must hit faces";
}
