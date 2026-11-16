package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidStructuralCopy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Seeded oracle fuzz for the splitter (Fable5.2Analysis.md Stage E). Every
solid of the reference corpus is split by planes through its own vertices:
random normals, axis normals and diagonal normals. The L-shaped hollow brick
also gets 40 planes through its two reflex vertices, where a face is
crossed twice at the same vertex.

<p>A case is correct when the pieces conserve the volume, each piece is on
the side its list names, each is strict-valid, and no piece is empty while
the plane really cuts the solid. The count of correct cases is a ratchet:
raise it when a fix lands, never lower it.</p>

<p>When the section is pinched at a reflex vertex (two section regions
meeting at the vertex), the splitter splits the self-touching section loop
into two faces; regions that meet only at the vertex come out as separate
solids.</p>

<p>Traceability: [MANT1988] Ch. 14, Sections 14.4-14.8.</p>
 */
class SplitterOracleFuzzTest
{
    /**
    Before Stage E: 37 / 256. Stage E: 252 / 256. Pinched sections
    resolved (2026-11-16): 256 / 256.
    */
    private static final int MINIMUM_CORRECT = 256;

    private static final double TOLERANCE = 1.0e-6;

    @Test
    void given_corpusSolidsAndPlanesThroughVertices_when_split_then_correctCountDoesNotRegress()
    {
        Random random = new Random(7);
        List<String> failures = new ArrayList<>();
        Map<String, Integer> byReason = new TreeMap<>();
        List<Vector3Dd[]> reflexPlanes = new ArrayList<>();
        Vector3Dd[] reflexVertices = {
            new Vector3Dd(0.2, 0.8, 0), new Vector3Dd(0.2, 0.8, 0.2) };
        int total = 0;
        int correct = 0;
        int k;

        for ( k = 0; k < 40; k++ ) {
            reflexPlanes.add(new Vector3Dd[] {
                new Vector3Dd(random.nextGaussian(), random.nextGaussian(),
                    0.5 * random.nextGaussian()),
                reflexVertices[k % 2] });
        }
        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            PolyhedralBoundedSolid[] pair =
                CsgSampleCorpusFixtures.createPair(sample);
            for ( int which = 0; which < 2; which++ ) {
                String name = sample + (which == 0 ? "/A" : "/B");
                PolyhedralBoundedSolid base = pair[which];
                boolean lShape = name.equals("HOLLOW_BRICK/A");
                int vertexCount = base.getVerticesList().size();
                for ( k = 0; k < (lShape ? 52 : 12); k++ ) {
                    Vector3Dd p = base.getVerticesList().get(
                        random.nextInt(vertexCount)).position;
                    Vector3Dd n;
                    if ( k >= 12 ) {
                        n = reflexPlanes.get(k - 12)[0];
                        p = reflexPlanes.get(k - 12)[1];
                    }
                    else if ( k % 3 == 0 ) {
                        n = new Vector3Dd(random.nextGaussian(),
                            random.nextGaussian(), random.nextGaussian());
                    }
                    else if ( k % 3 == 1 ) {
                        int axis = random.nextInt(3);
                        n = new Vector3Dd(axis == 0 ? 1 : 0,
                            axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
                    }
                    else {
                        n = new Vector3Dd(random.nextInt(3) - 1,
                            random.nextInt(3) - 1, 1);
                    }
                    if ( n.length() < 1.0e-6 ) {
                        continue;
                    }
                    total++;
                    String reason = check(base,
                        new InfinitePlane(n.normalized(), p));
                    if ( reason == null ) {
                        correct++;
                    }
                    else {
                        byReason.merge(reason, 1, Integer::sum);
                        failures.add(name + " k=" + k + " " + reason +
                            " n=" + n + " p=" + p);
                    }
                }
            }
        }

        System.out.println("split fuzz: " + correct + " / " + total + " " +
            byReason);
        failures.forEach(f -> System.out.println("  " + f));
        assertThat(correct).as("correct split results must not regress")
            .isGreaterThanOrEqualTo(MINIMUM_CORRECT);
    }

    private static String check(PolyhedralBoundedSolid base,
        InfinitePlane plane)
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidStructuralCopy.copy(base);
        ArrayList<PolyhedralBoundedSolid> above = new ArrayList<>();
        ArrayList<PolyhedralBoundedSolid> below = new ArrayList<>();
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(base);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double total = 0.0;
        int i;

        for ( i = 0; i < base.getVerticesList().size(); i++ ) {
            double d = plane.pointDistance(
                base.getVerticesList().get(i).position);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        boolean cuts = min < -TOLERANCE && max > TOLERANCE;

        try {
            PolyhedralBoundedSolidModeler.split(solid, plane, above, below);
        }
        catch ( Throwable t ) {
            return "exception " + t.getClass().getSimpleName();
        }

        boolean strict = true;
        boolean empty = false;
        boolean sides = true;
        for ( int side = 1; side >= -1; side -= 2 ) {
            for ( PolyhedralBoundedSolid piece : side > 0 ? above : below ) {
                strict &= PolyhedralBoundedSolidValidationEngine
                    .validateStrict(piece, new StringBuilder());
                total += PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
                empty |= cuts && piece.getPolygonsList().size() == 0;
                for ( i = 0; i < piece.getVerticesList().size(); i++ ) {
                    if ( side * plane.pointDistance(piece.getVerticesList()
                             .get(i).position) < -TOLERANCE ) {
                        sides = false;
                    }
                }
            }
        }
        if ( Math.abs(total - volume) > TOLERANCE * Math.max(1.0, volume) ) {
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
        return null;
    }
}
