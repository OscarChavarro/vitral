import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import { Arrow } from "../../environment/geometry/volume/Arrow.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Cone } from "../../environment/geometry/volume/Cone.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { GizmoSolidTessellator } from "../../gui/gizmo/GizmoSolidTessellator.js";
import { ReferenceFrameGizmo } from "../../gui/gizmo/ReferenceFrameGizmo.js";
import type { TranslateGizmo } from "../../gui/gizmo/TranslateGizmo.js";
import type { ViewportElementScaler } from "../../gui/viewport/ViewportElementScaler.js";
import { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";
import { Gizmo2DProjector } from "./Gizmo2DProjector.js";

/**
Port of `vsdk.toolkit.render.gizmo.TranslateGizmo2DPainter`.

Presents a {@link TranslateGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4TranslateGizmoRenderer` draws: the lines of the
axes and plane handles (as quads facing the camera), the cones of the axis
heads (with a darker base) and the translucent quad of the selected plane
handle; plus the labels of the axes `Jogl4ViewportWindow` draws next to their
tips (yellow when the axis is highlighted).
*/
export class TranslateGizmo2DPainter {
    private static readonly CONE_BASE_SHADE = 0.5;
    private static readonly HIGHLIGHT_COLOR = new ColorRgb(1, 1, 0);
    // Sizes, in pixels, designed for legacy resolutions
    private static readonly BASE_LABEL_FONT_SIZE = 14;
    private static readonly BASE_LABEL_OFFSET_X = -3;
    private static readonly BASE_LABEL_OFFSET_Y = 12;
    private static readonly AXES = new ReferenceFrameGizmo();

    private constructor() {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    public static paint(out: Gizmo2DDrawList | null, gizmo: TranslateGizmo | null,
                        camera: Camera | null, scaler: ViewportElementScaler): void {
        if (out === null || gizmo === null || camera === null) {
            return;
        }
        const projector = new Gizmo2DProjector(camera, out);
        const elements = gizmo.getElements3dsmax();

        //- Opaque elements -----------------------------------------------
        for (const segment of gizmo.getLineSegments()) {
            const strip = gizmo.buildLineStrip(segment);
            if (strip !== null) {
                projector.addStrip(strip, segment.color(), 1.0);
            }
        }
        for (const element of elements) {
            const g = element.getGeometry();
            if (g instanceof Cone) {
                TranslateGizmo2DPainter.addCone(projector, element, g);
            }
        }

        //- Translucent elements ------------------------------------------
        for (const element of elements) {
            const g = element.getGeometry();
            if (g instanceof Box) {
                const box = g;
                const quad = GizmoSolidTessellator.buildPlaneQuad(
                    GizmoSolidTessellator.localTransform(element),
                    box.getSize().x(), box.getSize().y());
                projector.addStrip(quad, element.getMaterial()!.getDiffuse(),
                    element.getMaterial()!.getOpacity());
            }
        }

        TranslateGizmo2DPainter.addLabels(projector, gizmo, scaler);
    }

    private static addCone(projector: Gizmo2DProjector, element: SimpleBody,
                           cone: Cone): void {
        const radius = cone.getBottomRadius();
        const c = element.getMaterial()!.getDiffuse();
        const local = GizmoSolidTessellator.localTransform(element);
        const shade = TranslateGizmo2DPainter.CONE_BASE_SHADE;
        const dark = new ColorRgb(c.r()*shade, c.g()*shade, c.b()*shade);
        const baseFan = GizmoSolidTessellator.buildConeBaseFan(local, radius);
        const perimeter = baseFan.slice(1);

        projector.addPolygon(perimeter, dark, 1.0);
        projector.addFanTriangles(GizmoSolidTessellator.buildConeSideFan(
            local, radius, cone.getHeight()), c, 1.0);
    }

    /**
    Labels of the axes, a little beyond the tip of each one.
    */
    private static addLabels(projector: Gizmo2DProjector,
                             gizmo: TranslateGizmo,
                             scaler: ViewportElementScaler): void {
        const fontSize = scaler.scaleSize(TranslateGizmo2DPainter.BASE_LABEL_FONT_SIZE);
        const offsetX = JavaMath.round(scaler.scaleLength(TranslateGizmo2DPainter.BASE_LABEL_OFFSET_X));
        const offsetY = JavaMath.round(scaler.scaleLength(TranslateGizmo2DPainter.BASE_LABEL_OFFSET_Y));
        const things = gizmo.getElements();

        for (let i = 0; i < things.length && i < ReferenceFrameGizmo.NUMBER_OF_AXES; i++) {
            const r = things[i]!;
            const g = r.getGeometry();

            if (g === null) {
                continue;
            }
            let tip = 1;
            if (g instanceof Arrow) {
                const a = g;
                tip = (a.getHeadLength() + a.getBaseLength()) * 1.1;
            }
            const R = new Matrix4x4d().translation(r.getPosition())
                .multiply(r.getRotation());
            const p = projector.projectPoint(R.multiply(new Vector3Dd(0, 0, tip)));

            if (p !== null) {
                const highlighted = ColorRgb.distance(TranslateGizmo2DPainter.HIGHLIGHT_COLOR,
                    r.getMaterial()!.getDiffuse()) < VSDK.EPSILON;
                projector.getGizmo2DDrawList().addOverlayLabel(
                    Math.trunc(p[0]!) + offsetX, Math.trunc(p[1]!) + offsetY,
                    TranslateGizmo2DPainter.AXES.getAxisLabel(i),
                    highlighted ? TranslateGizmo2DPainter.HIGHLIGHT_COLOR
                        : TranslateGizmo2DPainter.AXES.getAxisColor(i),
                    fontSize, Gizmo2DDrawList.Alignment.LOWER_LEFT);
            }
        }
    }
}
