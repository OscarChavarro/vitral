package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.Calligraphic2DBuffer;

public final class LightGizmoOmniBillboard {
    private static final int NUMBER_OF_SIDES = 32;
    private static final int NUMBER_OF_RAYS = 8;
    private static final double CIRCLE_RADIUS = 0.2;
    private static final double RAY_INNER_RADIUS = 0.3;
    private static final double RAY_OUTER_RADIUS = 0.5;

    private LightGizmoOmniBillboard() {
    }

    public static Calligraphic2DBuffer createLinePattern()
    {
        Calligraphic2DBuffer lines = new Calligraphic2DBuffer();

        final double cx = 0.5;
        final double cy = 0.5;

        for ( int i = 0; i < NUMBER_OF_SIDES; i++ ) {
            double a0 = 2.0 * Math.PI * i / NUMBER_OF_SIDES;
            double a1 = 2.0 * Math.PI * (i + 1) / NUMBER_OF_SIDES;

            double x0 = cx + CIRCLE_RADIUS * Math.cos(a0);
            double y0 = cy + CIRCLE_RADIUS * Math.sin(a0);
            double x1 = cx + CIRCLE_RADIUS * Math.cos(a1);
            double y1 = cy + CIRCLE_RADIUS * Math.sin(a1);

            lines.add2DLine(x0, y0, x1, y1);
        }

        for ( int i = 0; i < NUMBER_OF_RAYS; i++ ) {
            double a = 2.0 * Math.PI * i / NUMBER_OF_RAYS;

            double x0 = cx + RAY_INNER_RADIUS * Math.cos(a);
            double y0 = cy + RAY_INNER_RADIUS * Math.sin(a);
            double x1 = cx + RAY_OUTER_RADIUS * Math.cos(a);
            double y1 = cy + RAY_OUTER_RADIUS * Math.sin(a);

            lines.add2DLine(new Vector3Dd(x0, y0, 0.0), new Vector3Dd(x1, y1, 0.0));
        }

        return lines;
    }
}
