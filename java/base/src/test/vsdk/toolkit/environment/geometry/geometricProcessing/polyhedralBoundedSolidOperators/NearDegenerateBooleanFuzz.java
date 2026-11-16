package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

/**
Box pairs in degenerate contact, displaced by a multiple of the kernel
tolerance (plan-stage11 Phase B). Each configuration starts from an exact
contact and moves B along the contact normal by d = k x bigEpsilon, with k
in {-100, -10, -1, -0.1, 0, 0.1, 1, 10, 100}: from a clear overlap, through
the band where the kernel must decide whether two faces coincide, to a
clear gap. The whole pair is also tested rotated, so that the decision does
not rest on axis-aligned coordinates.

<p>Outcomes of one operation:</p>
<ul>
<li>OK: a strict-valid solid whose volume is the exact one of the displaced
    boxes, within what a displacement of one tolerance can change;</li>
<li>REJECTED: a typed exception (the operation refused to answer);</li>
<li>WRONG: a returned solid with another volume, or not strict-valid. This
    is the outcome that must never happen.</li>
</ul>
 */
final class NearDegenerateBooleanFuzz
{
    static final double[] STEPS = {
        -100, -10, -1, -0.1, 0, 0.1, 1, 10, 100 };

    enum Contact {
        /** B on top of A, footprints partly overlapping. */
        STACKED_PARTIAL,
        /** B on top of A, same footprint. */
        STACKED_SAME_FOOTPRINT,
        /** B beside A with coplanar top and bottom, touching side faces. */
        SIDE_BY_SIDE,
        /** B crossing A with one face coplanar with a face of A. */
        FLUSH_FACE_CROSSING,
        /** B inside A touching one face of A from inside. */
        INNER_TANGENT,
        /** B touching A along one edge only. */
        EDGE_CONTACT
    }

    static final class Summary {
        int total;
        int answered;
        int rejected;
        int wrong;
        final Map<String, Integer> byOutcome = new TreeMap<>();
        final List<String> details = new ArrayList<>();
    }

    private NearDegenerateBooleanFuzz()
    {
    }

    /** A and B for a contact, B moved by d along the contact normal. */
    static OrientedBox[] pair(Contact contact, double d, boolean rotated)
    {
        OrientedBox a;
        OrientedBox b;

        switch ( contact ) {
            case STACKED_PARTIAL:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.4, 0.3, 1 + d, 1.4, 1.3, 1.6 + d);
                break;
            case STACKED_SAME_FOOTPRINT:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0, 0, 1 + d, 1, 1, 1.7 + d);
                break;
            case SIDE_BY_SIDE:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(1 + d, 0.2, 0, 1.8 + d, 0.9, 1);
                break;
            case FLUSH_FACE_CROSSING:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.3, -0.4, 0.5 + d, 0.8, 1.5, 1 + d);
                break;
            case INNER_TANGENT:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.2, 0.3, 0.6 + d, 0.7, 0.8, 1 + d);
                break;
            default:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(1 + d, 0.1, 1 + d, 1.9 + d, 0.9, 1.8 + d);
                break;
        }
        if ( rotated ) {
            Matrix4x4d motion = PolyhedralBoundedSolidVolumeOracle
                .translation(0.3, -0.2, 0.5).multiply(
                    PolyhedralBoundedSolidVolumeOracle.rotation(0.83, 1, 2,
                        -1.5));
            a = new OrientedBox(a.size[0], a.size[1], a.size[2],
                motion.multiply(a.transform));
            b = new OrientedBox(b.size[0], b.size[1], b.size[2],
                motion.multiply(b.transform));
        }
        return new OrientedBox[] { a, b };
    }

    private static OrientedBox box(double x0, double y0, double z0,
        double x1, double y1, double z1)
    {
        return new OrientedBox(x1 - x0, y1 - y0, z1 - z0,
            PolyhedralBoundedSolidVolumeOracle.translation(
                (x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0));
    }

    static Summary run(Contact contact, boolean rotated)
    {
        Summary summary = new Summary();
        double bigEpsilon = PolyhedralBoundedSolidNumericPolicy.forSolid(
            pair(contact, 0, rotated)[0].toSolid()).bigEpsilon();

        for ( double step : STEPS ) {
            for ( int op : BooleanOracleFuzz.OPERATIONS ) {
                String outcome = runOne(contact, step * bigEpsilon, rotated,
                    op, bigEpsilon);
                String label = contact + (rotated ? " rotated" : "") +
                    " k=" + step + " " + BooleanOracleFuzz.opName(op);
                summary.total++;
                summary.byOutcome.merge(outcome.split(" ")[0], 1,
                    Integer::sum);
                if ( outcome.startsWith("OK") ) {
                    summary.answered++;
                }
                else if ( outcome.startsWith("REJECTED") ) {
                    summary.rejected++;
                    summary.details.add(label + " " + outcome);
                }
                else {
                    summary.wrong++;
                    summary.details.add(label + " " + outcome);
                }
            }
        }
        return summary;
    }

    private static String runOne(Contact contact, double d, boolean rotated,
        int op, double bigEpsilon)
    {
        OrientedBox[] pair = pair(contact, d, rotated);
        OrientedBox first = op == BooleanOracleFuzz.B_MINUS_A ?
            pair[1] : pair[0];
        OrientedBox second = op == BooleanOracleFuzz.B_MINUS_A ?
            pair[0] : pair[1];
        int kernelOp = op == BooleanOracleFuzz.B_MINUS_A ?
            PolyhedralBoundedSolidModeler.SUBTRACT : op;
        // The expected volume comes from the pair before the rotation: a
        // rigid motion keeps volumes, and the half-space oracle is exact
        // for axis-aligned boxes even when they only touch.
        OrientedBox[] aligned = pair(contact, d, false);
        double expected = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
            op == BooleanOracleFuzz.B_MINUS_A ? aligned[1] : aligned[0],
            op == BooleanOracleFuzz.B_MINUS_A ? aligned[0] : aligned[1],
            kernelOp);
        // Faces closer than the tolerance are one face for the kernel: the
        // volume may differ by the contact area (under 2 here) times the
        // displacement, when the displacement is inside the band.
        double tolerance = 1.0e-9 + 2.0 * Math.min(Math.abs(d),
            2.0 * bigEpsilon);
        PolyhedralBoundedSolid result;

        try {
            result = PolyhedralBoundedSolidModeler.setOp(first.toSolid(),
                second.toSolid(), kernelOp);
        }
        catch ( PolyhedralBoundedSolidOperationException e ) {
            String message = String.valueOf(e.getMessage()).split("\n")[0];
            return "REJECTED " + e.getClass().getSimpleName() + ": " +
                (message.length() > 150 ? message.substring(0, 150) : message);
        }
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(
            result);
        if ( Math.abs(volume - expected) > tolerance ) {
            return "WRONG volume=" + volume + " expected=" + expected;
        }
        StringBuilder message = new StringBuilder();
        if ( result.getPolygonsList().size() > 0 &&
             !PolyhedralBoundedSolidValidationEngine.validateStrict(result,
                 message) ) {
            return "WRONG not strict-valid: " +
                message.toString().split("\n")[0];
        }
        return "OK";
    }

    /** Prints every outcome that is not OK. */
    public static void main(String[] args)
    {
        vsdk.toolkit.common.VSDK.setWithSystemExit(false);
        for ( Contact contact : Contact.values() ) {
            for ( boolean rotated : new boolean[] { false, true } ) {
                Summary s = run(contact, rotated);
                System.out.println(contact + (rotated ? " rotated" : "") +
                    ": ok " + s.answered + " rejected " + s.rejected + " wrong " +
                    s.wrong + " of " + s.total + " " + s.byOutcome);
                s.details.forEach(x -> System.out.println("  " + x));
            }
        }
    }
}
