package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Stage-6 diagnostic and regression net (doc/plan-csg-boolean-fix-stage6.md):
moons 23/28/33/38 (all at z=9.0, one per quadrant) visually showed spurious
face subdivisions on bowl faces far away from the moon imprint, caused by the
unguarded vertex snap in {@code _PolyhedralBoundedSolidSetIntersector}
dragging remote vertices onto extended face planes.  This test quantifies the
result face structure for an OK moon (21) and the four formerly failing moons,
with and without the post-process stage (maximizeFaces +
triangulateNonPlanarFaces), and asserts that no non-planar faces remain, that
finish() never needs to ear-clip, and that the bowl region antipodal to the
moon stays bit-identical to the untouched operand.  Tagged slow.
*/
@Tag("slow")
class Stage6FaceSubdivisionDiagnosticTest
{
    private static final int[] MOTIFS = { 21, 23, 28, 33, 38 };

    @Test
    void diagnose_faceStructurePerMotif()
    {
        int i;

        for ( i = 0; i < MOTIFS.length; i++ ) {
            runOne(MOTIFS[i], true);
            runOne(MOTIFS[i], false);
        }
    }

    @Test
    void diagnose_bowlOperandPlanarityResiduals()
    {
        PolyhedralBoundedSolid[] operands;

        operands = CsgKurlanderBowlFixture
            .createBowlAndFirstStarOperands(23);
        dumpNonPlanarOrMarginalFaces("[STAGE6] bowlA", operands[0]);
    }

    @Test
    void diagnose_backRegionVertexDisplacement()
    {
        PolyhedralBoundedSolid[] operands;
        PolyhedralBoundedSolid bowlReference;
        PolyhedralBoundedSolid result;

        bowlReference = CsgKurlanderBowlFixture
            .createBowlAndFirstStarOperands(23)[0];
        operands = CsgKurlanderBowlFixture
            .createBowlAndFirstStarOperands(23);
        result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1],
            PolyhedralBoundedSolidModeler.SUBTRACT, false, false);

        ArrayList<String> referenceLines;
        ArrayList<String> resultLines;

        referenceLines = dumpRegionVertices("[STAGE6] backRef", bowlReference);
        resultLines = dumpRegionVertices("[STAGE6] backRes", result);
        dumpNonPlanarOrMarginalFaces("[STAGE6] res23", result);

        // Stage-6 regression net: the bowl region diametrically opposite the
        // moon imprint must be bit-identical to the untouched bowl operand.
        // Before the stage-6 fix, the Generate vertex snap moved the bowl
        // vertex antipodal to the moon by ~2e-5 onto an extended face plane
        // of the moon cylinder.
        assertThat(resultLines)
            .as("back-region vertices after bowl-moon subtraction")
            .containsExactlyElementsOf(referenceLines);
    }

    /**
    Prints vertices whose cylindrical coordinates fall in the back-region
    window (azimuth 20..70 degrees, z 1.0..1.7) where the spurious face
    subdivisions of motifs 23/28/33/38 were observed.
    @return the sorted, formatted vertex lines for comparison.
    */
    private ArrayList<String> dumpRegionVertices(
        String tag, PolyhedralBoundedSolid solid)
    {
        ArrayList<String> lines = new ArrayList<String>();
        int vi;

        for ( vi = 0; vi < solid.getVerticesList().size(); vi++ ) {
            double x = solid.getVerticesList().get(vi).position.x();
            double y = solid.getVerticesList().get(vi).position.y();
            double z = solid.getVerticesList().get(vi).position.z();
            double az = Math.toDegrees(Math.atan2(y, x));
            double r = Math.sqrt(x * x + y * y);

            if ( az >= 20.0 && az <= 70.0 && z >= 1.0 && z <= 1.7 ) {
                lines.add(String.format(
                    "az=%8.3f z=%8.5f r=%8.5f p=<%.6f, %.6f, %.6f>",
                    az, z, r, x, y, z));
            }
        }
        java.util.Collections.sort(lines);
        for ( String line : lines ) {
            System.out.println(tag + " " + line);
        }
        return lines;
    }

    /**
    Prints every face whose maximum vertex distance to its containing plane
    exceeds one tenth of the solid's planarity threshold, plus whether the
    geometric validator considers it planar.  Used to detect "borderline"
    faces whose planarity verdict could flip from run to run.
    */
    private void dumpNonPlanarOrMarginalFaces(
        String tag, PolyhedralBoundedSolid solid)
    {
        int fi;

        for ( fi = 0; fi < solid.getPolygonsList().size(); fi++ ) {
            _PolyhedralBoundedSolidFace face;
            double residual;
            boolean planar;

            face = solid.getPolygonsList().get(fi);
            residual = planarityResidual(face);
            planar = PolyhedralBoundedSolidGeometricValidator
                .validateFaceIsPlanar(face);
            if ( !planar || residual > 1.0e-6 ) {
                double[] c = faceCentroidCylindrical(face);
                System.out.println(tag + " f" + face.id
                    + String.format(
                        " residual=%.3e planar=%b az=%7.1f z=%6.3f r=%6.3f n=%d",
                        residual, planar, c[0], c[1], c[2],
                        face.boundariesList.get(0).halfEdgesList.size()));
            }
        }
    }

    private double planarityResidual(_PolyhedralBoundedSolidFace face)
    {
        double worst = 0.0;
        int li;
        int k;

        if ( face.getContainingPlane() == null ) {
            return Double.NaN;
        }
        for ( li = 0; li < face.boundariesList.size(); li++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(li);
            for ( k = 0; k < loop.halfEdgesList.size(); k++ ) {
                double d = Math.abs(face.getContainingPlane().pointDistance(
                    loop.halfEdgesList.get(k).startingVertex.position));
                if ( d > worst ) {
                    worst = d;
                }
            }
        }
        return worst;
    }

    private double[] faceCentroidCylindrical(_PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        double cx = 0.0;
        double cy = 0.0;
        double cz = 0.0;
        int n = outer.halfEdgesList.size();
        int k;

        for ( k = 0; k < n; k++ ) {
            cx += outer.halfEdgesList.get(k).startingVertex.position.x();
            cy += outer.halfEdgesList.get(k).startingVertex.position.y();
            cz += outer.halfEdgesList.get(k).startingVertex.position.z();
        }
        cx /= n;
        cy /= n;
        cz /= n;
        return new double[] {
            Math.toDegrees(Math.atan2(cy, cx)),
            cz,
            Math.sqrt(cx * cx + cy * cy)
        };
    }

    private void runOne(int motifIndex, boolean maximizeResultFaces)
    {
        PolyhedralBoundedSolid[] operands;
        PolyhedralBoundedSolid result;
        int bowlFaces;
        String tag;

        operands = CsgKurlanderBowlFixture
            .createBowlAndFirstStarOperands(motifIndex);
        bowlFaces = operands[0].getPolygonsList().size();
        result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1],
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false, maximizeResultFaces);

        tag = "[STAGE6] motif=" + motifIndex
            + " postProcess=" + (maximizeResultFaces ? "ON " : "OFF");

        assertThat(result)
            .as("result of motif " + motifIndex)
            .isNotNull();
        assertThat(result.getPolygonsList().size())
            .as("result face count of motif " + motifIndex)
            .isGreaterThan(0);

        TreeMap<Integer, Integer> histogram = new TreeMap<Integer, Integer>();
        ArrayList<String> bigFaces = new ArrayList<String>();
        int nonPlanarCount = 0;
        int fi;

        for ( fi = 0; fi < result.getPolygonsList().size(); fi++ ) {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop outer;
            int size;
            boolean planar;

            face = result.getPolygonsList().get(fi);
            outer = face.boundariesList.get(0);
            size = outer.halfEdgesList.size();
            histogram.merge(size, 1, Integer::sum);
            planar = PolyhedralBoundedSolidGeometricValidator
                .validateFaceIsPlanar(face);
            if ( !planar ) {
                nonPlanarCount++;
            }
            if ( size >= 8 || face.boundariesList.size() > 1 ) {
                bigFaces.add("f" + face.id + ":n=" + size
                    + ":loops=" + face.boundariesList.size()
                    + ":planar=" + planar);
            }
        }

        System.out.println(tag
            + " bowlFaces=" + bowlFaces
            + " resultFaces=" + result.getPolygonsList().size()
            + " triangulated=" + _PolyhedralBoundedSolidSetFinisher
                .getLastTriangulatedFaceCount()
            + " nonPlanar=" + nonPlanarCount
            + " histogram=" + histogram);
        if ( !bigFaces.isEmpty() ) {
            System.out.println(tag + " bigFaces=" + bigFaces);
        }
        if ( !maximizeResultFaces ) {
            dumpTriangleCentroids(tag, result);
        }

        // Stage-6 regression net: subtracting a single moon must not leave any
        // non-planar face in the result, and finish() must not need to
        // ear-clip any face. Before the stage-6 fix (containment-guarded
        // vertex snap in _PolyhedralBoundedSolidSetIntersector), motifs
        // 23/28/33/38 produced 14 ear-clipped faces from bowl vertices that
        // the Generate snap had dragged onto far-away face planes.
        assertThat(nonPlanarCount)
            .as("non-planar faces in result of motif " + motifIndex)
            .isZero();
        assertThat(_PolyhedralBoundedSolidSetFinisher
                .getLastTriangulatedFaceCount())
            .as("faces ear-clipped by finish() for motif " + motifIndex)
            .isZero();
    }

    /**
    Prints the centroid of every triangular face in cylindrical coordinates
    (azimuth in degrees around the bowl axis, z, radial distance) so that
    spurious triangles far away from the moon imprint can be spotted.
    */
    private void dumpTriangleCentroids(
        String tag, PolyhedralBoundedSolid result)
    {
        int fi;

        for ( fi = 0; fi < result.getPolygonsList().size(); fi++ ) {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop outer;

            face = result.getPolygonsList().get(fi);
            outer = face.boundariesList.get(0);
            if ( outer.halfEdgesList.size() != 3 ) {
                continue;
            }

            double cx = 0.0;
            double cy = 0.0;
            double cz = 0.0;
            int k;

            for ( k = 0; k < 3; k++ ) {
                cx += outer.halfEdgesList.get(k).startingVertex.position.x();
                cy += outer.halfEdgesList.get(k).startingVertex.position.y();
                cz += outer.halfEdgesList.get(k).startingVertex.position.z();
            }
            cx /= 3.0;
            cy /= 3.0;
            cz /= 3.0;

            double azimuthDeg = Math.toDegrees(Math.atan2(cy, cx));
            double radial = Math.sqrt(cx * cx + cy * cy);

            System.out.println(tag + " tri f" + face.id
                + String.format(" az=%7.1f z=%6.3f r=%6.3f",
                    azimuthDeg, cz, radial));
        }
    }
}
