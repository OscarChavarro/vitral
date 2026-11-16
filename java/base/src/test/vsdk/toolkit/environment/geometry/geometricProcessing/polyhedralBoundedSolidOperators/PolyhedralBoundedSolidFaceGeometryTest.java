package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.Random;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidFaceGeometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

import static org.assertj.core.api.Assertions.assertThat;

/**
The box tests of the face geometry snapshot (Fable5.2Analysis.md Stage G)
are only allowed to skip work: whenever {@code projectionExcludes} answers
true, the point-in-face test must answer OUTSIDE, and whenever the
point-in-face test accepts a point, that point lies inside the face box
grown by {@code containmentMargin}. Checked on every face of the reference
corpus with points on the face plane, near its vertices and edges, and far
away.
 */
class PolyhedralBoundedSolidFaceGeometryTest
{
    private static final double TOLERANCE = 1.0e-5;

    @Test
    void given_corpusFaces_when_boxRejects_then_pointInFaceSaysOutside()
    {
        Random random = new Random(11);
        int rejected = 0;
        int accepted = 0;

        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            for ( PolyhedralBoundedSolid solid :
                      CsgSampleCorpusFixtures.createPair(sample) ) {
                double[] box = solid.getMinMax();
                for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
                    _PolyhedralBoundedSolidFace face =
                        solid.getPolygonsList().get(i);
                    PolyhedralBoundedSolidFaceGeometry geometry =
                        PolyhedralBoundedSolidFaceGeometry.of(face);
                    if ( geometry.getPlane() == null ) {
                        continue;
                    }
                    for ( int k = 0; k < 200; k++ ) {
                        Vector3Dd p = samplePoint(random, solid, box, k);
                        p = geometry.getPlane().projectPoint(p);
                        int status = face.testPointInside(p, TOLERANCE,
                            geometry.getPlane());
                        if ( geometry.projectionExcludes(p, TOLERANCE) ) {
                            rejected++;
                            assertThat(status).as("%s face %d point %s",
                                sample, face.id, p)
                                .isEqualTo(Geometry.OUTSIDE);
                        }
                        if ( status != Geometry.OUTSIDE ) {
                            accepted++;
                        }
                    }
                }
            }
        }
        assertThat(rejected).as("the box test must reject something")
            .isGreaterThan(1000);
        assertThat(accepted).as("the sample must hit faces").isGreaterThan(100);
    }

    /**
    Mixes points around vertices (to exercise the LIMIT tolerances), random
    points in the solid box, and points far outside it.
    */
    private static Vector3Dd samplePoint(Random random,
        PolyhedralBoundedSolid solid, double[] box, int k)
    {
        double size = Math.max(box[3] - box[0],
            Math.max(box[4] - box[1], box[5] - box[2]));
        if ( k % 3 == 0 ) {
            Vector3Dd v = solid.getVerticesList().get(
                random.nextInt(solid.getVerticesList().size())).position;
            double r = (random.nextInt(4) + 1) * TOLERANCE;
            return v.add(new Vector3Dd(random.nextGaussian(),
                random.nextGaussian(), random.nextGaussian()).multiply(r));
        }
        double grow = k % 3 == 1 ? 0.1 : 2.0;
        return new Vector3Dd(
            box[0] - grow * size + random.nextDouble() * (1 + 2 * grow) *
                (box[3] - box[0]),
            box[1] - grow * size + random.nextDouble() * (1 + 2 * grow) *
                (box[4] - box[1]),
            box[2] - grow * size + random.nextDouble() * (1 + 2 * grow) *
                (box[5] - box[2]));
    }
}
