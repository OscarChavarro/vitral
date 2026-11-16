import type { Camera } from "../../environment/camera/Camera.js";
import { InputGizmo } from "../../gui/gizmo/InputGizmo.js";
import { RotateGizmo } from "../../gui/gizmo/RotateGizmo.js";
import type { ViewportElementScaler } from "../../gui/viewport/ViewportElementScaler.js";
import { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";
import { Gizmo2DProjector } from "./Gizmo2DProjector.js";

/**
Port of `vsdk.toolkit.render.gizmo.RotateGizmo2DPainter`.

Presents a {@link RotateGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4RotateGizmoRenderer` draws: the visible part of
its three axis rings and the camera ring (as quads facing the camera, with
the colors the gizmo gives them) and, while a ring is dragged, the
translucent sector of the swept arc, plus the label with its angle that
`Jogl4ViewportWindow` draws next to the middle of the arc.
*/
export class RotateGizmo2DPainter {
    private static readonly ARC_OPACITY = 0.35;
    // Size, in pixels, designed for legacy resolutions
    private static readonly BASE_ARC_LABEL_FONT_SIZE = 16;

    private constructor() {}

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    public static paint(
        out: Gizmo2DDrawList | null,
        gizmo: RotateGizmo | null,
        camera: Camera | null,
        scaler: ViewportElementScaler,
    ): void {
        if (out === null || gizmo === null || camera === null) {
            return;
        }
        const projector = new Gizmo2DProjector(camera, out);

        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            const color = gizmo.getRingColor(ring);
            for (const strip of gizmo.buildRingStrips(ring)) {
                projector.addStrip(strip, color, 1.0);
            }
        }
        projector.addStrip(gizmo.buildCameraRingStrip(), gizmo.getCameraRingColor(), 1.0);

        //- Translucent arc and its label ---------------------------------
        projector.addFanAsPolygon(gizmo.buildArcFan(), gizmo.getArcColor(), RotateGizmo2DPainter.ARC_OPACITY);

        const anchor = gizmo.getArcLabelPosition();
        if (anchor !== null) {
            const p = projector.projectPoint(anchor);
            if (p !== null) {
                out.addOverlayLabel(
                    Math.trunc(p[0]!),
                    Math.trunc(p[1]!),
                    InputGizmo.format(gizmo.getArcSweepInDegrees(), RotateGizmo.ANGLE_DECIMALS) + "°",
                    gizmo.getArcColor(),
                    scaler.scaleSize(RotateGizmo2DPainter.BASE_ARC_LABEL_FONT_SIZE),
                    Gizmo2DDrawList.Alignment.CENTER,
                );
            }
        }
    }
}
