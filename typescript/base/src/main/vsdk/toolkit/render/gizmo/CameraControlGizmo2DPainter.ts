//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,          =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import type { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { Tube } from "../../environment/geometry/volume/Tube.js";
import type { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { CameraControlGizmo } from "../../gui/gizmo/CameraControlGizmo.js";
import type { ViewportElementScaler } from "../../gui/viewport/ViewportElementScaler.js";
import { Calligraphic2DBuffer } from "../../media/Calligraphic2DBuffer.js";
import { HiddenLineRenderer } from "../hiddenLine/HiddenLineRenderer.js";
import { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";

/**
Presents a `CameraControlGizmo` (a ViewCube) as 2D primitives of a
`Gizmo2DDrawList`, for drawing surfaces without a 3D API, in the HUD area of
the gizmo:
  - The edges of its 27 parts (26 boxes and the compass tube), as boundary
    representations, with the hidden lines removed by the algorithm of Appel
    [APPE1967] (`HiddenLineRenderer.executeAppelAlgorithm`), seen from the
    HUD camera of the gizmo. Only the visible lines are presented (contours
    thicker), as overlay lines, so they are exact.
  - Under them, the faces of the parts that face the viewer, filled with the
    color of each part (the highlight of the region under the cursor, and
    the translucency of an inactive gizmo), sorted with the painter's
    algorithm [FOLE1992].15.5.
  - Over everything, the labels of the faces of the cube that face the
    viewer.

The boundary representations are exported once, and the lines are kept
while the orientation and size of the gizmo do not change, as the hidden
line removal is the expensive part.

Use one painter (and one draw list) for each gizmo, and draw that list after
the one of the other gizmos, as the HUD is over everything.
*/
export class CameraControlGizmo2DPainter {
    private static readonly COMPASS_SLICES: number = CameraControlGizmo.COMPASS_SLICES;
    private static readonly BASE_LABEL_FONT_SIZE: number = 9;
    private static readonly MIN_LABEL_VISIBILITY: number = 0.3;

    private cachedGizmo: CameraControlGizmo | null;
    /// Bodies with the boundary representations of the parts of the gizmo
    private readonly brepParts: ArrayList<SimpleBody>;
    private cachedRotation: Matrix4x4d | null;
    private cachedSize: number;
    private contourLines: Calligraphic2DBuffer;
    private visibleLines: Calligraphic2DBuffer;

    public constructor() {
        this.cachedGizmo = null;
        this.brepParts = new ArrayList<SimpleBody>();
        this.cachedRotation = null;
        this.cachedSize = -1;
        this.contourLines = new Calligraphic2DBuffer();
        this.visibleLines = new Calligraphic2DBuffer();
    }

    /**
    Adds the primitives that present the gizmo, in pixels of its viewport.
    @param list list receiving the primitives
    @param gizmo gizmo to present, already placed (see
    `CameraControlGizmo.placeInViewport`) and with its HUD camera updated
    @param scaler scaler of the viewport set (sizes of lines and labels), or
    null for base sizes
    */
    public paint(list: Gizmo2DDrawList, gizmo: CameraControlGizmo, scaler: ViewportElementScaler | null): void {
        if (!gizmo.isVisible()) {
            return;
        }
        this.prepareBreps(gizmo);
        this.updateHiddenLines(gizmo);

        const hud: Camera = gizmo.getHudCamera();
        const projection: Matrix4x4d = hud.calculateProjectionMatrix();
        const viewDirection: Vector3Dd = hud.getFront();
        const opacity = gizmo.getOpacity();

        //- Faces, depth sorted ------------------------------------------
        for (let r = 0; r < CameraControlGizmo.NUMBER_OF_PARTS; r++) {
            const color = gizmo.getPartColor(r);
            if (r === CameraControlGizmo.COMPASS_REGION) {
                CameraControlGizmo2DPainter.addCompassFaces(list, gizmo, projection, viewDirection, color, opacity);
            } else {
                CameraControlGizmo2DPainter.addBrepFaces(
                    list,
                    gizmo,
                    this.brepParts.get(r),
                    projection,
                    viewDirection,
                    color,
                    opacity,
                );
            }
        }

        //- Visible lines from the hidden line removal --------------------
        const lineWidth = scaler !== null ? scaler.scaleLength(1.0) : 1.0;
        const lineColor = gizmo.getLineColor();
        CameraControlGizmo2DPainter.addLines(list, gizmo, this.visibleLines, lineColor, lineWidth);
        CameraControlGizmo2DPainter.addLines(list, gizmo, this.contourLines, lineColor, 1.5 * lineWidth);

        //- Labels --------------------------------------------------------
        const fontSize =
            scaler !== null
                ? scaler.scaleSize(CameraControlGizmo2DPainter.BASE_LABEL_FONT_SIZE)
                : CameraControlGizmo2DPainter.BASE_LABEL_FONT_SIZE;
        for (const face of gizmo.getFaceRegions()) {
            if (gizmo.faceVisibility(face) < CameraControlGizmo2DPainter.MIN_LABEL_VISIBILITY) {
                continue;
            }
            const p = gizmo.projectToViewport(gizmo.getFaceLabelPosition(face));
            list.addOverlayLabel(
                p[0]!,
                p[1]!,
                gizmo.getFaceLabel(face)!,
                gizmo.getLabelColor(),
                fontSize,
                Gizmo2DDrawList.Alignment.CENTER,
            );
        }
    }

    //= Caches ============================================================

    private prepareBreps(gizmo: CameraControlGizmo): void {
        if (this.cachedGizmo === gizmo) {
            return;
        }
        this.cachedGizmo = gizmo;
        this.cachedRotation = null;
        this.brepParts.clear();
        for (let p = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++) {
            const part = gizmo.getParts().get(p);
            const brep = gizmo.exportPartToPolyhedralBoundedSolid(p);
            const body = new SimpleBody();
            body.setGeometry(brep);
            body.setPosition(part.getPosition());
            body.setRotation(new Matrix4x4d());
            body.setRotationInverse(new Matrix4x4d());
            body.setScale(new Vector3Dd(1, 1, 1));
            body.setMaterial(part.getMaterial());
            this.brepParts.add(body);
        }
    }

    /**
    [APPE1967] over the parts, only when the orientation or size changed.
    */
    private updateHiddenLines(gizmo: CameraControlGizmo): void {
        const hud = gizmo.getHudCamera();
        const rotation = hud.getRotation();

        if (
            this.cachedRotation !== null &&
            this.cachedSize === gizmo.getSizeInPixels() &&
            this.cachedRotation.epsilonEquals(rotation, 1e-9)
        ) {
            return;
        }
        this.cachedRotation = rotation;
        this.cachedSize = gizmo.getSizeInPixels();
        this.contourLines = new Calligraphic2DBuffer();
        this.visibleLines = new Calligraphic2DBuffer();
        const hiddenLines = new Calligraphic2DBuffer();

        HiddenLineRenderer.executeAppelAlgorithm(
            this.brepParts,
            hud,
            this.contourLines,
            this.visibleLines,
            hiddenLines,
        );
    }

    //= Primitives ========================================================

    private static addLines(
        list: Gizmo2DDrawList,
        gizmo: CameraControlGizmo,
        lines: Calligraphic2DBuffer,
        color: ColorRgb,
        width: number,
    ): void {
        const size = gizmo.getSizeInPixels();

        for (let i = 0; i < lines.getNumLines(); i++) {
            const line = lines.get2DLine(i);
            list.addOverlayLine(
                gizmo.getHudX() + ((line[0].x() + 1.0) / 2.0) * size,
                gizmo.getHudY() + ((1.0 - line[0].y()) / 2.0) * size,
                gizmo.getHudX() + ((line[1].x() + 1.0) / 2.0) * size,
                gizmo.getHudY() + ((1.0 - line[1].y()) / 2.0) * size,
                color,
                width,
            );
        }
    }

    /**
    A little of shading, so the sides of the cube seen at once differ: the
    color is dimmed by how much the face turns away from the viewer.
    */
    private static lambert(normal: Vector3Dd, viewDirection: Vector3Dd): number {
        const c = -normal.normalized().dotProduct(viewDirection.normalized());
        return 0.65 + 0.35 * JavaMath.max(0.0, c);
    }

    /**
    Adds the polygon of a face if it faces the viewer.
    */
    private static addPolygon(
        list: Gizmo2DDrawList,
        gizmo: CameraControlGizmo,
        projection: Matrix4x4d,
        viewDirection: Vector3Dd,
        vertices: Vector3Dd[],
        normal: Vector3Dd,
        color: ColorRgb,
        opacity: number,
    ): void {
        if (normal.dotProduct(viewDirection) >= 0.0) {
            return;
        }
        const n = vertices.length;
        const x: number[] = new Array<number>(n).fill(0);
        const y: number[] = new Array<number>(n).fill(0);
        let depth = 0;
        const size = gizmo.getSizeInPixels();

        for (let i = 0; i < n; i++) {
            const ndc = projection.multiply(new Vector4Dd(vertices[i]!)).dividedByW();
            x[i] = gizmo.getHudX() + ((ndc.x() + 1.0) / 2.0) * size;
            y[i] = gizmo.getHudY() + ((1.0 - ndc.y()) / 2.0) * size;
            depth += ndc.z();
        }
        list.addPolygon(
            x,
            y,
            depth / n,
            color.multiply(CameraControlGizmo2DPainter.lambert(normal, viewDirection)),
            opacity,
        );
    }

    private static addBrepFaces(
        list: Gizmo2DDrawList,
        gizmo: CameraControlGizmo,
        body: SimpleBody,
        projection: Matrix4x4d,
        viewDirection: Vector3Dd,
        color: ColorRgb,
        opacity: number,
    ): void {
        const solid = body.getGeometry() as PolyhedralBoundedSolid;
        const offset = body.getPosition();

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i)!;
            if (face.boundariesList.size() !== 1) {
                continue;
            }
            const points = new ArrayList<Vector3Dd>();
            const start: _PolyhedralBoundedSolidHalfEdge = face.boundariesList.get(0)!.boundaryStartHalfEdge!;
            let he: _PolyhedralBoundedSolidHalfEdge = start;
            do {
                points.add(he.startingVertex.position.add(offset));
                he = he.next()!;
            } while (he !== start);
            CameraControlGizmo2DPainter.addPolygon(
                list,
                gizmo,
                projection,
                viewDirection,
                points.toArray(),
                face.getContainingPlane()!.getNormal(),
                color,
                opacity,
            );
        }
    }

    /**
    The compass is filled with quads computed from its parameters, as its
    caps (annuli) are not simple polygons.
    */
    private static addCompassFaces(
        list: Gizmo2DDrawList,
        gizmo: CameraControlGizmo,
        projection: Matrix4x4d,
        viewDirection: Vector3Dd,
        color: ColorRgb,
        opacity: number,
    ): void {
        const part = gizmo.getParts().get(CameraControlGizmo.COMPASS_REGION);
        const tube = part.getGeometry() as Tube;
        const ri = tube.getInternalRadius();
        const re = tube.getExternalRadius();
        const z0 = part.getPosition().z();
        const z1 = z0 + tube.getHeight();
        const slices = CameraControlGizmo2DPainter.COMPASS_SLICES;

        for (let i = 0; i < slices; i++) {
            const a0 = (2 * JavaMath.PI * i) / slices;
            const a1 = (2 * JavaMath.PI * (i + 1)) / slices;
            const c0 = JavaMath.cos(a0),
                s0 = JavaMath.sin(a0);
            const c1 = JavaMath.cos(a1),
                s1 = JavaMath.sin(a1);
            const cm = JavaMath.cos((a0 + a1) / 2),
                sm = JavaMath.sin((a0 + a1) / 2);

            // Caps
            CameraControlGizmo2DPainter.addPolygon(
                list,
                gizmo,
                projection,
                viewDirection,
                [
                    new Vector3Dd(ri * c0, ri * s0, z1),
                    new Vector3Dd(re * c0, re * s0, z1),
                    new Vector3Dd(re * c1, re * s1, z1),
                    new Vector3Dd(ri * c1, ri * s1, z1),
                ],
                new Vector3Dd(0, 0, 1),
                color,
                opacity,
            );
            CameraControlGizmo2DPainter.addPolygon(
                list,
                gizmo,
                projection,
                viewDirection,
                [
                    new Vector3Dd(ri * c0, ri * s0, z0),
                    new Vector3Dd(ri * c1, ri * s1, z0),
                    new Vector3Dd(re * c1, re * s1, z0),
                    new Vector3Dd(re * c0, re * s0, z0),
                ],
                new Vector3Dd(0, 0, -1),
                color,
                opacity,
            );
            // Walls
            CameraControlGizmo2DPainter.addPolygon(
                list,
                gizmo,
                projection,
                viewDirection,
                [
                    new Vector3Dd(re * c0, re * s0, z0),
                    new Vector3Dd(re * c1, re * s1, z0),
                    new Vector3Dd(re * c1, re * s1, z1),
                    new Vector3Dd(re * c0, re * s0, z1),
                ],
                new Vector3Dd(cm, sm, 0),
                color,
                opacity,
            );
            CameraControlGizmo2DPainter.addPolygon(
                list,
                gizmo,
                projection,
                viewDirection,
                [
                    new Vector3Dd(ri * c1, ri * s1, z0),
                    new Vector3Dd(ri * c0, ri * s0, z0),
                    new Vector3Dd(ri * c0, ri * s0, z1),
                    new Vector3Dd(ri * c1, ri * s1, z1),
                ],
                new Vector3Dd(-cm, -sm, 0),
                color,
                opacity,
            );
        }
    }
}
