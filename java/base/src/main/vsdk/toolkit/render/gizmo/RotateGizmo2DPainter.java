package vsdk.toolkit.render.gizmo;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Presents a {@link RotateGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4RotateGizmoRenderer` draws: the visible part of
its three axis rings and the camera ring (as quads facing the camera, with
the colors the gizmo gives them) and, while a ring is dragged, the
translucent sector of the swept arc, plus the label with its angle that
`Jogl4ViewportWindow` draws next to the middle of the arc.
*/
public class RotateGizmo2DPainter
{
    private static final double ARC_OPACITY = 0.35;
    // Size, in pixels, designed for legacy resolutions
    private static final int BASE_ARC_LABEL_FONT_SIZE = 16;

    private RotateGizmo2DPainter()
    {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    public static void paint(Gizmo2DDrawList out, RotateGizmo gizmo,
                             Camera camera, ViewportElementScaler scaler)
    {
        if ( out == null || gizmo == null || camera == null ) {
            return;
        }
        Gizmo2DProjector projector = new Gizmo2DProjector(camera, out);

        for ( int ring = 0; ring < RotateGizmo.RING_COUNT; ring++ ) {
            ColorRgb color = gizmo.getRingColor(ring);

            for ( Vector3Dd[] strip : gizmo.buildRingStrips(ring) ) {
                projector.addStrip(strip, color, 1.0);
            }
        }
        projector.addStrip(gizmo.buildCameraRingStrip(), gizmo.getCameraRingColor(), 1.0);

        //- Translucent arc and its label ---------------------------------
        projector.addFanAsPolygon(gizmo.buildArcFan(), gizmo.getArcColor(), ARC_OPACITY);

        Vector3Dd anchor = gizmo.getArcLabelPosition();
        if ( anchor != null ) {
            double[] p = projector.projectPoint(anchor);

            if ( p != null ) {
                out.addOverlayLabel((int)p[0], (int)p[1],
                    InputGizmo.format(gizmo.getArcSweepInDegrees(),
                        RotateGizmo.ANGLE_DECIMALS) + "°",
                    gizmo.getArcColor(), scaler.scaleSize(BASE_ARC_LABEL_FONT_SIZE),
                    Gizmo2DDrawList.Alignment.CENTER);
            }
        }
    }
}
