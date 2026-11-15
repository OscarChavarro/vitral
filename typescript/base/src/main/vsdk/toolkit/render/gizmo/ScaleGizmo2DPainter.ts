import type { Camera } from "../../environment/camera/Camera.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Cone } from "../../environment/geometry/volume/Cone.js";
import { GizmoSolidTessellator } from "../../gui/gizmo/GizmoSolidTessellator.js";
import { ScaleGizmo } from "../../gui/gizmo/ScaleGizmo.js";
import type { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";
import { Gizmo2DProjector } from "./Gizmo2DProjector.js";

/**
Port of `vsdk.toolkit.render.gizmo.ScaleGizmo2DPainter`.

Presents a {@link ScaleGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4ScaleGizmoRenderer` draws: the shafts of the
axes and the cubes at their tips (solid), the contour of the flat handles
(lines) and the translucent interior of the handle selected.
*/
export class ScaleGizmo2DPainter {
    /// Opacity of the interior of the flat handle currently selected
    private static readonly HANDLE_OPACITY = 0.55;
    /// Depth bias (towards the viewer) of the contour of the flat handles
    private static readonly CONTOUR_DEPTH_BIAS = -0.0002;

    private constructor() {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera, transformation matrix and scale set
    @param camera camera of the viewport, with its viewport size updated
    */
    public static paint(out: Gizmo2DDrawList | null, gizmo: ScaleGizmo | null,
                        camera: Camera | null): void {
        if (out === null || gizmo === null || camera === null) {
            return;
        }
        const projector = new Gizmo2DProjector(camera, out);

        for (const element of gizmo.getElements()) {
            const g = element.getGeometry();
            const local = GizmoSolidTessellator.localTransform(element);

            if (g instanceof Cone) {
                const cone = g;
                projector.addStrip(GizmoSolidTessellator.buildShaftStrip(local,
                    cone.getBottomRadius(), cone.getTopRadius(), cone.getHeight()),
                    element.getMaterial()!.getDiffuse(), 1.0);
            }
            else if (g instanceof Box) {
                for (const face of GizmoSolidTessellator.buildBoxFaceStrips(
                         local, g.getSize())) {
                    projector.addStrip(face, element.getMaterial()!.getDiffuse(), 1.0);
                }
            }
        }

        for (const segment of gizmo.buildContourSegments()) {
            projector.addSegment(segment.start(), segment.end(), segment.color(),
                gizmo.getLineWidth(), ScaleGizmo2DPainter.CONTOUR_DEPTH_BIAS);
        }

        for (const group of ScaleGizmo.BAND_GROUPS) {
            if (gizmo.isBandHighlighted(group)) {
                projector.addStrip(gizmo.buildBandQuad(group),
                    ScaleGizmo.HANDLE_FILL_COLOR, ScaleGizmo2DPainter.HANDLE_OPACITY);
            }
        }
        if (gizmo.isUniformHighlighted()) {
            projector.addTriangles(gizmo.buildUniformTriangles(),
                ScaleGizmo.HANDLE_FILL_COLOR, ScaleGizmo2DPainter.HANDLE_OPACITY);
        }
    }
}
