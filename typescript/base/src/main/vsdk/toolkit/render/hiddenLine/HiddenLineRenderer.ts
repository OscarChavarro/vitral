//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisivility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =

// Java classes
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";

// VitralSDK classes
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import type { Intersection } from "../../environment/geometry/element/Intersection.js";
import { Triangle } from "../../environment/geometry/element/Triangle.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import { Volume } from "../../environment/geometry/volume/Volume.js";
import { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { Calligraphic2DBuffer } from "../../media/Calligraphic2DBuffer.js";
import { RenderingElement } from "../RenderingElement.js";

/**
Package-private in Java (`class _AppelEdgeSegment` at the top of
`HiddenLineRenderer.java`); exported from this module only so that the
symbol stays traceable, and not re-exported by the package barrel.
*/
export class _AppelEdgeSegment extends RenderingElement {
    /// Distance from start to end with respect to line parameter
    public lineParameter = 0.0;
    /// Change in quantitative invisibility when the edge crosses this boundary,
    /// computed at detection from Appel's image-space side rule. Coincident
    /// crossings are merged by SUMMING their deltaQI so none is lost. 0 for the
    /// synthetic t=0 / t=1 bounds.
    public deltaQI = 0;

    public compareTo(other: _AppelEdgeSegment): number {
        if (this.lineParameter < other.lineParameter - VSDK.EPSILON) return -1;
        else if (this.lineParameter > other.lineParameter + VSDK.EPSILON) return 1;
        return 0;
    }
}

/**
Package-private in Java (`class _AppelEdgeCache` in `HiddenLineRenderer.java`);
exported from this module only for traceability, as `_AppelEdgeSegment` is.
*/
export class _AppelEdgeCache extends RenderingElement {
    public static readonly HIDDEN_LINE = 0;
    public static readonly VISIBLE_LINE = 1;
    public static readonly CONTOUR_LINE = 2;

    public edgeType = 0;
    /// True if current line starts on the end of a previous one in the same
    /// solid and with the same quantitative invisibility. When this happens,
    /// quantitative invisibility can be acumulated in the edge sequence,
    /// otherwise must be calculated.
    public onSequence = false;
    public start: Vector3Dd = null as unknown as Vector3Dd;
    public end: Vector3Dd = null as unknown as Vector3Dd;
    /// direction = end - start
    public direction: Vector3Dd = null as unknown as Vector3Dd;
    public ownerBody: SimpleBody | null = null;
    /// `visibleEdgeForContourLine` contains an explicit reference to the
    /// planar surface marked as "S" on figure [APPE1967].5.
    public visibleEdgeForContourLine: _PolyhedralBoundedSolidFace | null = null;
    public visibleEdgeBody: SimpleBody | null = null;
    public leftFace: _PolyhedralBoundedSolidFace | null = null;
    public rightFace: _PolyhedralBoundedSolidFace | null = null;
    public edgeIndex = 0;

    public setStart(s: Vector3Dd): void {
        this.start = new Vector3Dd(s);
    }

    public setEnd(e: Vector3Dd): void {
        this.end = new Vector3Dd(e);
    }
}

/**
This class implements the Appel's algorithm for hidden line rendering. :)

Port of `vsdk.toolkit.render.hiddenLine.HiddenLineRenderer`. The edge cache,
the contour sweep-plane splitting, the coincident-boundary merge and the
per-sub-segment midpoint quantitative-invisibility classification are the Java
ones, statement by statement. Java's public nested dump records are the
members of the `HiddenLineRenderer` namespace below, and Java's
`String.format("%.5f", ...)` in the diagnostic log becomes `toFixed(5)`, which
only the `DEBUG_EDGE_INDEX` trace prints.
*/
export class HiddenLineRenderer extends RenderingElement {
    /// Diagnostic: when >= 0, processLineToBeDrawn logs the contour-crossing /
    /// segment-splitting decisions for the cached edge with this edgeIndex.
    public static DEBUG_EDGE_INDEX = -1;

    private static debugSplit(edgeIndex: number, message: string): void {
        if (edgeIndex === HiddenLineRenderer.DEBUG_EDGE_INDEX) {
            console.log("[AppelSplit e" + edgeIndex + "] " + message);
        }
    }

    public static isFaceVisibleFromCamera(face: _PolyhedralBoundedSolidFace, camera: Camera): number {
        const iv: Vector3Dd = new Vector3Dd(1, 0, 0);
        let viewingVector: Vector3Dd = camera.getRotation().multiply(iv);
        const n: Vector3Dd = face.getContainingPlane()!.getNormal().normalized();
        let dot: number;

        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            viewingVector = viewingVector.normalized();
            dot = n.dotProduct(viewingVector);
            if (dot > VSDK.EPSILON) {
                return -1;
            } else if (dot < -VSDK.EPSILON) {
                return 1;
            } else {
                return 0;
            }
        }

        const cameraPosition: Vector3Dd = camera.getPosition();
        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop = face.boundariesList.get(i)!;
            let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
            const heStart: _PolyhedralBoundedSolidHalfEdge | null = he;

            do {
                he = he!.next();
                if (he === null) {
                    // Loop is not closed.
                    break;
                }
                const p: Vector3Dd = he.startingVertex.position;
                const t: Vector3Dd = p.subtract(cameraPosition).multiply(-1).normalized();
                if (t.dotProduct(n) > 0.0) {
                    return 1;
                }
            } while (he !== heStart);
        }
        return -1;
    }

    private static transformToWorld(body: SimpleBody | null, localPoint: Vector3Dd): Vector3Dd {
        if (body === null) {
            return localPoint;
        }
        return body.getTransformationMatrix().multiply(localPoint);
    }

    private static transformToLocal(body: SimpleBody | null, worldPoint: Vector3Dd): Vector3Dd {
        if (body === null) {
            return worldPoint;
        }

        const translatedPoint: Vector3Dd = worldPoint.subtract(body.getPosition());
        const rotatedPoint: Vector3Dd = body.getRotationInverse().multiply(translatedPoint);
        const scale: Vector3Dd = body.getScale();

        return new Vector3Dd(
            Math.abs(scale.x()) > VSDK.EPSILON ? rotatedPoint.x() / scale.x() : 0.0,
            Math.abs(scale.y()) > VSDK.EPSILON ? rotatedPoint.y() / scale.y() : 0.0,
            Math.abs(scale.z()) > VSDK.EPSILON ? rotatedPoint.z() / scale.z() : 0.0,
        );
    }

    /**
    World-space containing plane of a face, with a reliably OUTWARD normal.

    The orientation is taken from the face's maintained plane
    (`getContainingPlane()`, a Newell fit over the whole boundary loop), not
    from the cross product of the first three loop vertices: the latter flips to
    an inward normal at a reflex (concave) corner, which mislabels front/back
    faces and the occluding face of a contour. The outward object-space normal
    is mapped to world space with the body's normal transform (orientation
    preserving), and an arbitrary loop vertex provides the in-plane point.

    @param face face whose world plane is requested
    @param body body the face belongs to (null = identity transform)
    @return the world-space plane with an outward normal, or null if undefined
    */
    private static getWorldContainingPlane(
        face: _PolyhedralBoundedSolidFace | null,
        body: SimpleBody | null,
    ): InfinitePlane | null {
        if (face === null) {
            return null;
        }

        const localPlane: InfinitePlane | null = face.getContainingPlane();
        if (localPlane === null) {
            return null;
        }

        let pointOnFace: Vector3Dd | null = null;
        for (let i = 0; i < face.boundariesList.size() && pointOnFace === null; i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            const he: _PolyhedralBoundedSolidHalfEdge | null = loop !== null ? loop.boundaryStartHalfEdge : null;
            if (he !== null && he.startingVertex !== null) {
                pointOnFace = he.startingVertex.position;
            }
        }
        if (pointOnFace === null) {
            return null;
        }

        const worldPoint: Vector3Dd = HiddenLineRenderer.transformToWorld(body, pointOnFace);
        const worldNormal: Vector3Dd =
            body === null ? localPlane.getNormal() : body.transformNormalToWorld(localPlane.getNormal());
        if (worldNormal.length() <= VSDK.EPSILON) {
            return null;
        }
        return new InfinitePlane(worldNormal.normalized(), worldPoint);
    }

    private static isFaceVisibleFromCameraTransformed(
        face: _PolyhedralBoundedSolidFace | null,
        body: SimpleBody | null,
        camera: Camera | null,
    ): number {
        if (face === null || camera === null) {
            return 0;
        }

        const iv: Vector3Dd = new Vector3Dd(1, 0, 0);
        let viewingVector: Vector3Dd = camera.getRotation().multiply(iv);
        const plane: InfinitePlane | null = HiddenLineRenderer.getWorldContainingPlane(face, body);
        if (plane === null) {
            return 0;
        }
        const n: Vector3Dd = plane.getNormal().normalized();

        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            viewingVector = viewingVector.normalized();
            const dot: number = n.dotProduct(viewingVector);
            if (dot > VSDK.EPSILON) {
                return -1;
            } else if (dot < -VSDK.EPSILON) {
                return 1;
            }
            return 0;
        }

        const cameraPosition: Vector3Dd = camera.getPosition();
        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
            const heStart: _PolyhedralBoundedSolidHalfEdge = he;
            do {
                he = he.next();
                if (he === null || he.startingVertex === null) {
                    break;
                }
                const p: Vector3Dd = HiddenLineRenderer.transformToWorld(body, he.startingVertex.position);
                const t: Vector3Dd = p.subtract(cameraPosition).multiply(-1).normalized();
                if (t.dotProduct(n) > 0.0) {
                    return 1;
                }
            } while (he !== heStart);
        }
        return -1;
    }

    /**
    Given a set of solids, this method computes the "edge cache": a list
    of edges, where every edge gets one of three classifications:
    HIDDEN_LINE, VISIBLE_LINE or CONTOUR_LINE.
    Edges are classified according to the visibility of its participating
    faces. Note that current implementation supposes that every given
    edge is shared by exactly two planar surfaces, and that no pair of
    edges intersects. This assumption is implied here by first converting
    the solid to a polyhedral bounded representation (BREP). As VitralSDK
    BREP ensures that assumptions, current implementation is solid with
    that data structure.

    Original [APPE1967] paper makes no assumption on data representation as
    long that the representation is able to answer the question of what pair
    or surfaces share an edge, so this Vitral SDK implementation is more
    restrictive that the one of the original paper, but it is expected to me
    also more robust.
    */
    private static buildCache(
        solids: ArrayList<SimpleBody>,
        body: SimpleBody,
        cache: ArrayList<_AppelEdgeCache>,
        contourCache: ArrayList<_AppelEdgeCache>,
        camera: Camera,
    ): void {
        let g: Geometry | object | null = body.getGeometry();

        if (g === null) {
            return;
        }

        if (!(g instanceof Volume)) {
            return;
        }
        g = g.exportToPolyhedralBoundedSolid();
        if (g === null) {
            return;
        }

        solids.add(body);
        const solid: PolyhedralBoundedSolid = g as PolyhedralBoundedSolid;

        let i: number;
        let l = 0;

        let face1: _PolyhedralBoundedSolidFace;
        let face2: _PolyhedralBoundedSolidFace;
        let f1: boolean;
        let f2: boolean;
        let materialLine: _AppelEdgeCache;
        let prevEnd: Vector3Dd = new Vector3Dd();

        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const e: _PolyhedralBoundedSolidEdge = solid.getEdgesList().get(i)!;

            const start: number = e.getStartingVertexId();
            const end: number = e.getEndingVertexId();
            if (start >= 0 && end >= 0) {
                const startPosition: Vector3Dd | null = e.leftHalf!.startingVertex.position;
                const endPosition: Vector3Dd | null = e.rightHalf!.startingVertex.position;
                if (startPosition !== null && endPosition !== null) {
                    //--------------------------------------------------------
                    face1 = e.leftHalf!.parentLoop.parentFace;
                    face2 = e.rightHalf!.parentLoop.parentFace;
                    f1 = HiddenLineRenderer.isFaceVisibleFromCameraTransformed(face1, body, camera) >= 0;
                    f2 = HiddenLineRenderer.isFaceVisibleFromCameraTransformed(face2, body, camera) >= 0;

                    //--------------------------------------------------------
                    materialLine = new _AppelEdgeCache();
                    materialLine.setStart(HiddenLineRenderer.transformToWorld(body, startPosition));
                    materialLine.setEnd(HiddenLineRenderer.transformToWorld(body, endPosition));
                    materialLine.direction = materialLine.end.subtract(materialLine.start);
                    materialLine.ownerBody = body;
                    materialLine.leftFace = face1;
                    materialLine.rightFace = face2;
                    materialLine.edgeIndex = i;
                    if (l > 0 && Vector3Dd.distance(prevEnd, materialLine.start) < VSDK.EPSILON) {
                        materialLine.onSequence = true;
                    } else {
                        materialLine.onSequence = false;
                    }
                    if (!f1 && !f2) {
                        // Totally hidden lines
                        materialLine.edgeType = _AppelEdgeCache.HIDDEN_LINE;
                    } else if ((f1 && !f2) || (!f1 && f2)) {
                        // Contour lines
                        materialLine.edgeType = _AppelEdgeCache.CONTOUR_LINE;
                        if (f1) {
                            materialLine.visibleEdgeForContourLine = face1;
                        } else {
                            materialLine.visibleEdgeForContourLine = face2;
                        }
                        materialLine.visibleEdgeBody = body;
                        contourCache.add(materialLine);
                    } else {
                        // Visible non contour lines
                        materialLine.edgeType = _AppelEdgeCache.VISIBLE_LINE;
                    }
                    cache.add(materialLine);
                    //--------------------------------------------------------
                    prevEnd = Vector3Dd.copyOf(materialLine.end);
                    l++;
                }
            }
        }
    }

    private static computeMidpointQuantitativeInvisibility(
        solids: ArrayList<SimpleBody>,
        camera: Camera,
        midpoint: Vector3Dd,
    ): number {
        // Single source of truth: delegate to the per-body kernel QI
        // (SimpleBody -> PolyhedralBoundedSolid.computeQuantitativeInvisibility),
        // which already resolves grazing lines of sight robustly. The previous
        // world-space reimplementation that lived here diverged from the kernel
        // (different LIMIT handling and normal source) and was removed so both
        // the visibility decision and the diagnostics share one computation.
        let qi = 0;
        for (let i = 0; i < solids.size(); i++) {
            qi += solids.get(i).computeQuantitativeInvisibility(camera.getPosition(), midpoint);
        }
        return qi;
    }

    private static isUnitInterval(t: number): boolean {
        return t >= VSDK.EPSILON && t <= 1.0 - VSDK.EPSILON;
    }

    public static edgeTypeName(edgeType: number): string {
        switch (edgeType) {
            case _AppelEdgeCache.HIDDEN_LINE:
                return "hidden";
            case _AppelEdgeCache.CONTOUR_LINE:
                return "contour";
            case _AppelEdgeCache.VISIBLE_LINE:
                return "visible";
            default:
                return "unknown";
        }
    }

    private static createEdgeDump(edge: _AppelEdgeCache): HiddenLineRenderer.AppelEdgeDump {
        const dump = new HiddenLineRenderer.AppelEdgeDump();
        dump.edgeIndex = edge.edgeIndex;
        dump.edgeType = edge.edgeType;
        dump.edgeTypeName = HiddenLineRenderer.edgeTypeName(edge.edgeType);
        dump.face1Id = edge.leftFace !== null ? edge.leftFace.id : -1;
        dump.face2Id = edge.rightFace !== null ? edge.rightFace.id : -1;
        dump.start = new Vector3Dd(edge.start);
        dump.end = new Vector3Dd(edge.end);
        return dump;
    }

    /**
    This method takes an edge that is candidate to be visible, breaks it into
    segments and for each segment determines visibility. Visible segments
    are reported in `outVisibleContourLineSet` and
    `outVisibleNonContourLineSet`, and hidden segments are reported on
    `outHiddenLineSet`.
    PRE:
      - Current edge is known to correspond to a material line (normal or
        contour)
      - `contourCache` contains the list of contour lines
    */
    private static readonly CLIP_PLANES: readonly (readonly number[])[] = [
        [1.0, 0.0, 0.0, 1.0],
        [-1.0, 0.0, 0.0, 1.0],
        [0.0, 1.0, 0.0, 1.0],
        [0.0, -1.0, 0.0, 1.0],
        [0.0, 0.0, 1.0, 1.0],
        [0.0, 0.0, -1.0, 1.0],
    ];

    private static evaluateClipPlane(plane: readonly number[], point: Vector4Dd): number {
        return plane[0]! * point.x() + plane[1]! * point.y() + plane[2]! * point.z() + plane[3]! * point.w();
    }

    private static interpolate(start: Vector4Dd, end: Vector4Dd, t: number): Vector4Dd {
        return start.multiply(1.0 - t).add(end.multiply(t));
    }

    private static clipLineToClipVolume(start: Vector4Dd, end: Vector4Dd): Vector4Dd[] | null {
        let clippedStart: Vector4Dd = start;
        let clippedEnd: Vector4Dd = end;

        for (let i = 0; i < HiddenLineRenderer.CLIP_PLANES.length; i++) {
            const plane: readonly number[] = HiddenLineRenderer.CLIP_PLANES[i]!;
            const d0: number = HiddenLineRenderer.evaluateClipPlane(plane, clippedStart);
            const d1: number = HiddenLineRenderer.evaluateClipPlane(plane, clippedEnd);

            if (d0 < 0.0 && d1 < 0.0) {
                return null;
            }
            if (d0 < 0.0 || d1 < 0.0) {
                const denominator: number = d0 - d1;
                if (Math.abs(denominator) < VSDK.EPSILON) {
                    return null;
                }
                const t: number = d0 / denominator;
                const intersection: Vector4Dd = HiddenLineRenderer.interpolate(clippedStart, clippedEnd, t);
                if (d0 < 0.0) {
                    clippedStart = intersection;
                } else {
                    clippedEnd = intersection;
                }
            }
        }
        return [clippedStart, clippedEnd];
    }

    private static addProjectedLine(
        lineSet: Calligraphic2DBuffer,
        point0: Vector3Dd,
        point1: Vector3Dd,
        camera: Camera,
    ): void {
        const clip0: Vector4Dd = camera.calculateProjectionMatrix().multiply(new Vector4Dd(point0));
        const clip1: Vector4Dd = camera.calculateProjectionMatrix().multiply(new Vector4Dd(point1));
        const clipped: Vector4Dd[] | null = HiddenLineRenderer.clipLineToClipVolume(clip0, clip1);
        if (clipped === null) {
            return;
        }

        const ndc0: Vector4Dd = clipped[0]!.dividedByW();
        const ndc1: Vector4Dd = clipped[1]!.dividedByW();
        lineSet.add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
    }

    private static processLineToBeDrawn(
        solids: ArrayList<SimpleBody>,
        inEdge: _AppelEdgeCache,
        inCamera: Camera,
        outVisibleContourLineSet: Calligraphic2DBuffer,
        outVisibleNonContourLineSet: Calligraphic2DBuffer,
        outHiddenLineSet: Calligraphic2DBuffer,
        contourCache: ArrayList<_AppelEdgeCache>,
        edgeDump: HiddenLineRenderer.AppelEdgeDump | null,
    ): void {
        //- 1. Compute the sweep plane triangle ---------------------------
        // Defines plane "SP1" on figure [APPE1967].5.
        const sp1a: Vector3Dd = inEdge.start;
        const sp1b: Vector3Dd = inEdge.end;
        const sp1c: Vector3Dd = inCamera.getPosition();

        //- 2. Break current edge into segments ---------------------------
        // Defines plane "SP2" on figure [APPE1967].5.
        let sp2a: Vector3Dd;
        let sp2b: Vector3Dd;
        let ray: Ray = new Ray(new Vector3Dd(), new Vector3Dd());
        let t0: number;
        let i: number;
        let cl: _AppelEdgeCache; // Line "CL" on figure 5 of [APPE1967]
        let segment: _AppelEdgeSegment;
        let plane: InfinitePlane;

        const segments: _AppelEdgeSegment[] = [];
        segment = new _AppelEdgeSegment();
        segment.lineParameter = 0;
        segment.deltaQI = 0;
        segments.push(segment);
        const sp2c: Vector3Dd = inCamera.getPosition();

        for (i = 0; i < contourCache.size(); i++) {
            cl = contourCache.get(i);
            if (cl === inEdge) {
                // Do not break an edge with itself.
                continue;
            }
            ray = ray.withOrigin(cl.start.add(cl.direction.multiply(3 * VSDK.EPSILON)));
            ray = ray.withDirection(cl.direction);
            t0 = ray.getDirection().length() - 6 * VSDK.EPSILON;
            ray = ray.withDirection(ray.getDirection().normalized());
            const hit: Intersection | null = Triangle.doIntersectionWithTriangle(ray, sp1a, sp1b, sp1c);
            if (inEdge.edgeIndex === HiddenLineRenderer.DEBUG_EDGE_INDEX && hit !== null) {
                HiddenLineRenderer.debugSplit(
                    inEdge.edgeIndex,
                    "contour cl.edgeIndex=" +
                        cl.edgeIndex +
                        " sweepHit t=" +
                        hit.getT().toFixed(5) +
                        " t0=" +
                        t0.toFixed(5) +
                        " accepted=" +
                        (hit.getT() < t0),
                );
            }
            if (hit !== null && hit.getT() < t0) {
                // The breaking point in the current testing edge corresponding
                // to the passing contour is the piercing point where the
                // edge intersects with the contour's sweeping plane.
                sp2a = cl.start;
                sp2b = cl.end;
                plane = new InfinitePlane(sp2a, sp2b, sp2c);
                ray = ray.withOrigin(inEdge.start);
                ray = ray.withDirection(inEdge.direction.normalized());
                const planeHit: Ray | null = plane.doIntersectionFirstHit(ray);
                if (planeHit === null) {
                    HiddenLineRenderer.debugSplit(
                        inEdge.edgeIndex,
                        "  cl=" + cl.edgeIndex + " DISCARDED: edge/SP2 plane intersection null",
                    );
                    continue;
                }
                segment = new _AppelEdgeSegment();
                // Point PP2 lies on the current edge, so its parameter must
                // come from the edge/plane intersection rather than from the
                // contour line piercing the sweep triangle.
                segment.lineParameter = planeHit.getT() / inEdge.direction.length();
                if (!HiddenLineRenderer.isUnitInterval(segment.lineParameter)) {
                    HiddenLineRenderer.debugSplit(
                        inEdge.edgeIndex,
                        "  cl=" +
                            cl.edgeIndex +
                            " DISCARDED: split t=" +
                            segment.lineParameter.toFixed(5) +
                            " outside unit interval",
                    );
                    continue;
                }

                // A contour line crosses this edge in the image at parameter
                // segment.t, so the edge is split there: the quantitative
                // invisibility can only change where the edge crosses a contour.
                // Each resulting sub-segment is classified independently in
                // step 4 by its midpoint Q.I.
                HiddenLineRenderer.debugSplit(
                    inEdge.edgeIndex,
                    "  cl=" + cl.edgeIndex + " ADDED split t=" + segment.lineParameter.toFixed(5),
                );
                segments.push(segment);
            }
        }
        segment = new _AppelEdgeSegment();
        segment.lineParameter = 1;
        segments.push(segment);

        //- 3. Sort segment set -------------------------------------------
        Collections.sort(segments);

        // Merge coincident boundaries: when two crossings fall within tolerance
        // they delimit a zero-length (undrawable) sub-segment, but each still
        // carries a real change in quantitative invisibility. Rather than drop
        // one (which would lose its increment), SUM their deltaQI into the kept
        // boundary so step 4 accounts for every crossing.
        for (i = 0; i < segments.length - 1; i++) {
            if (segments[i]!.compareTo(segments[i + 1]!) === 0) {
                segments[i + 1]!.deltaQI += segments[i]!.deltaQI;
                segments.splice(i, 1);
                i--;
            }
        }

        //- 4. Determine visibility of each sub-segment by its midpoint Q.I. -
        // The contour crossings (step 2) partition the edge into sub-segments of
        // UNIFORM visibility (the quantitative invisibility can change only where
        // the edge crosses a contour). Each sub-segment is classified by the
        // robust kernel Q.I. sampled at its midpoint
        // (PolyhedralBoundedSolid.computeQuantitativeInvisibility, delegated to
        // PolyhedralBoundedSolidPredicates). Incremental +/-1 deltaQI propagation
        // (Appel's original scheme) was investigated and dropped: the cheap
        // per-crossing deltaQI sign could not be made to reproduce the robust Q.I.
        // even at non-grazing orientations, so per-sub-segment sampling is the
        // correct classifier. See doc plan-stage09-appel-hidden-line-plan.md (P4).
        let pos1: Vector3Dd;
        let pos2: Vector3Dd;

        for (i = 0; i < segments.length - 1; i++) {
            const val1: number = segments[i]!.lineParameter;
            const val2: number = segments[i + 1]!.lineParameter;
            pos1 = inEdge.start.add(inEdge.direction.multiply(val1));
            pos2 = inEdge.start.add(inEdge.direction.multiply(val2));
            const posx: Vector3Dd = inEdge.start.add(inEdge.direction.multiply((val1 + val2) / 2));

            const midpointQi: number = HiddenLineRenderer.computeMidpointQuantitativeInvisibility(
                solids,
                inCamera,
                posx,
            );

            if (midpointQi === 0) {
                if (inEdge.edgeType === _AppelEdgeCache.CONTOUR_LINE) {
                    HiddenLineRenderer.addProjectedLine(outVisibleContourLineSet, pos1, pos2, inCamera);
                } else {
                    HiddenLineRenderer.addProjectedLine(outVisibleNonContourLineSet, pos1, pos2, inCamera);
                }
            } else {
                HiddenLineRenderer.addProjectedLine(outHiddenLineSet, pos1, pos2, inCamera);
            }

            if (edgeDump !== null) {
                const segmentDump = new HiddenLineRenderer.AppelSegmentDump();
                segmentDump.tStart = val1;
                segmentDump.tEnd = val2;
                segmentDump.start = new Vector3Dd(pos1);
                segmentDump.end = new Vector3Dd(pos2);
                segmentDump.midpoint = new Vector3Dd(posx);
                segmentDump.midpointQuantitativeInvisibility = midpointQi;
                segmentDump.classification = midpointQi === 0 ? "visible" : "hidden";
                edgeDump.segments.add(segmentDump);
            }
        }

        //segments = null;
    }

    /**
    Given a viewing camera and a set of bodies, this method generates three
    sets of lines for visible/hidden line rendering, as described in
    paper [APPE1967] and section [FOLE1992].15.3.2. The resulting line sets
    are projected to 2D calligraphic buffers and separated into visible
    contour, visible non-contour and hidden segments.
    */
    public static executeAppelAlgorithm(
        inSimpleBodyArray: ArrayList<SimpleBody> | readonly SimpleBody[],
        inCamera: Camera,
        outVisibleContourLineSet: Calligraphic2DBuffer,
        outVisibleNonContourLineSet: Calligraphic2DBuffer,
        outHiddenLineSet: Calligraphic2DBuffer,
    ): void {
        // Rendering path: do not collect the per-edge diagnostic dump, which
        // also lets each edge skip the diagnostic-only seed Q.I. sample and the
        // image-space deltaQI projection.
        HiddenLineRenderer.runAppelAlgorithm(
            inSimpleBodyArray,
            inCamera,
            outVisibleContourLineSet,
            outVisibleNonContourLineSet,
            outHiddenLineSet,
            false,
        );
    }

    public static executeAppelAlgorithmWithDiagnostics(
        inSimpleBodyArray: ArrayList<SimpleBody> | readonly SimpleBody[],
        inCamera: Camera,
        outVisibleContourLineSet: Calligraphic2DBuffer,
        outVisibleNonContourLineSet: Calligraphic2DBuffer,
        outHiddenLineSet: Calligraphic2DBuffer,
    ): HiddenLineRenderer.AppelAlgorithmDump {
        return HiddenLineRenderer.runAppelAlgorithm(
            inSimpleBodyArray,
            inCamera,
            outVisibleContourLineSet,
            outVisibleNonContourLineSet,
            outHiddenLineSet,
            true,
        );
    }

    private static runAppelAlgorithm(
        inSimpleBodyArray: ArrayList<SimpleBody> | readonly SimpleBody[],
        inCamera: Camera,
        outVisibleContourLineSet: Calligraphic2DBuffer,
        outVisibleNonContourLineSet: Calligraphic2DBuffer,
        outHiddenLineSet: Calligraphic2DBuffer,
        collectDiagnostics: boolean,
    ): HiddenLineRenderer.AppelAlgorithmDump {
        //-----------------------------------------------------------------
        const cache: ArrayList<_AppelEdgeCache> = new ArrayList<_AppelEdgeCache>();
        const contourCache: ArrayList<_AppelEdgeCache> = new ArrayList<_AppelEdgeCache>();

        //-----------------------------------------------------------------
        const solids: ArrayList<SimpleBody> = new ArrayList<SimpleBody>();
        let i: number;

        const bodies: ArrayList<SimpleBody> =
            inSimpleBodyArray instanceof ArrayList ? inSimpleBodyArray : new ArrayList<SimpleBody>(inSimpleBodyArray);
        for (i = 0; i < bodies.size(); i++) {
            HiddenLineRenderer.buildCache(solids, bodies.get(i), cache, contourCache, inCamera);
        }

        // Snapshot each solid's face planes and tolerance once: every edge below
        // issues many quantitative-invisibility samples against these unchanged
        // solids, so caching avoids recomputing each face plane per ray per
        // sample (the dominant cost on dense models such as the kurlanderBowl).
        for (i = 0; i < solids.size(); i++) {
            const geometry: Geometry | null = solids.get(i).getGeometry();
            if (geometry instanceof PolyhedralBoundedSolid) {
                geometry.beginVisibilityQueries();
            }
        }

        //-----------------------------------------------------------------
        let edge: _AppelEdgeCache;
        const dump = new HiddenLineRenderer.AppelAlgorithmDump();

        try {
            for (i = 0; i < cache.size(); i++) {
                edge = cache.get(i);
                const edgeDump: HiddenLineRenderer.AppelEdgeDump | null = collectDiagnostics
                    ? HiddenLineRenderer.createEdgeDump(edge)
                    : null;
                // Every cached edge is resolved through the quantitative
                // invisibility test, including those whose two adjacent faces are
                // both back-facing (edgeType HIDDEN_LINE). Such an edge must NOT be
                // assumed hidden: the "both faces back-facing => invisible"
                // shortcut is only sound for convex bodies. On a concave solid
                // (e.g. the kurlanderBowl) a both-back-facing edge can be genuinely
                // visible, so its visibility is determined by QI like any other
                // line, consistent with [APPE1967] which makes no convexity
                // assumption.
                switch (edge.edgeType) {
                    case _AppelEdgeCache.HIDDEN_LINE:
                    case _AppelEdgeCache.CONTOUR_LINE:
                    case _AppelEdgeCache.VISIBLE_LINE:
                        HiddenLineRenderer.processLineToBeDrawn(
                            solids,
                            edge,
                            inCamera,
                            outVisibleContourLineSet,
                            outVisibleNonContourLineSet,
                            outHiddenLineSet,
                            contourCache,
                            edgeDump,
                        );
                        break;
                    default:
                        break;
                }
                if (edgeDump !== null) {
                    dump.edges.add(edgeDump);
                }
            }
        } finally {
            for (i = 0; i < solids.size(); i++) {
                const geometry: Geometry | null = solids.get(i).getGeometry();
                if (geometry instanceof PolyhedralBoundedSolid) {
                    geometry.endVisibilityQueries();
                }
            }
        }
        //-----------------------------------------------------------------
        //cache = null;
        //contourCache = null;
        return dump;
    }
}

export namespace HiddenLineRenderer {
    export class AppelAlgorithmDump extends RenderingElement {
        public readonly edges: ArrayList<AppelEdgeDump> = new ArrayList<AppelEdgeDump>();
    }

    export class AppelEdgeDump extends RenderingElement {
        public edgeIndex = 0;
        public edgeType = 0;
        public edgeTypeName: string | null = null;
        public face1Id = 0;
        public face2Id = 0;
        public start: Vector3Dd | null = null;
        public end: Vector3Dd | null = null;
        public initialQuantitativeInvisibility = 0;
        public readonly segments: ArrayList<AppelSegmentDump> = new ArrayList<AppelSegmentDump>();
        public readonly events: ArrayList<AppelEventDump> = new ArrayList<AppelEventDump>();
    }

    export class AppelSegmentDump extends RenderingElement {
        public tStart = 0.0;
        public tEnd = 0.0;
        public start: Vector3Dd | null = null;
        public end: Vector3Dd | null = null;
        public midpoint: Vector3Dd | null = null;
        public midpointQuantitativeInvisibility = 0;
        public readonly midpointContributorFaceIds: ArrayList<number> = new ArrayList<number>();
        public classification: string | null = null;
    }

    export class AppelEventDump extends RenderingElement {
        public lineParameter = 0.0;
        public deltaQI = 0;
        public contourEdgeIndex = 0;
        public visibleFaceId = 0;
    }
}
