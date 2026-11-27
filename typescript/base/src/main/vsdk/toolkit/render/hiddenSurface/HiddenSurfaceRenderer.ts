//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

// Java classes
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";
import { Double } from "../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";

// VitralSDK classes
import { VSDK } from "../../common/VSDK.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { Geometry } from "../../environment/geometry/Geometry.js";
import type { Vertex2D } from "../../environment/geometry/element/Vertex2D.js";
import { WeilerAthertonPolygonClipper } from "../../environment/geometry/geometricProcessing/genericPolygonClipper/WeilerAthertonPolygonClipper.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import { Polygon2D } from "../../environment/geometry/surface/polygon/Polygon2D.js";
import { _Polygon2DContour } from "../../environment/geometry/surface/polygon/_Polygon2DContour.js";
import { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { DirectionalLight } from "../../environment/light/DirectionalLight.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { RenderingElement } from "../RenderingElement.js";

/**
An area of the x-y plane of the viewing space (a polygon with holes, maybe
with several main contours), with the measures used to sort and to clip it.

Package-private in Java (`class _WeilerAthertonArea` at the top of
`HiddenSurfaceRenderer.java`); exported from this module only so that the
symbol stays traceable, and not re-exported by the package barrel.
*/
export class _WeilerAthertonArea extends RenderingElement {
    public readonly polygon: Polygon2D;
    public readonly minX: number;
    public readonly minY: number;
    public readonly maxX: number;
    public readonly maxY: number;
    /// Area enclosed (holes discounted)
    public readonly area: number;
    /// Length of all the contours
    public readonly perimeter: number;

    public constructor(polygon: Polygon2D) {
        super();
        const box: number[] = HiddenSurfaceRenderer.boundingBox(polygon);
        this.polygon = polygon;
        this.minX = box[0]!;
        this.minY = box[1]!;
        this.maxX = box[2]!;
        this.maxY = box[3]!;
        this.area = HiddenSurfaceRenderer.enclosedArea(polygon);
        this.perimeter = HiddenSurfaceRenderer.perimeter(polygon);
    }

    public overlapsBox(other: _WeilerAthertonArea): boolean {
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxY >= other.minY && this.minY <= other.maxY;
    }
}

/**
Original copy of an input polygon of [WEIL1977]: a front face of a solid,
transformed and clipped to the viewing space, with its plane in that space.
As noted in [WEIL1977].216, the clip template is always this copy of the
original polygon, not the pieces left of it after previous clips.

Package-private in Java; exported only for traceability.
*/
export class _WeilerAthertonSourcePolygon extends RenderingElement {
    /// Area of the polygon on the x-y plane of the viewing space
    public template: _WeilerAthertonArea = null as unknown as _WeilerAthertonArea;
    /// Plane of the polygon in the viewing space, as depth =
    /// depthSlopeX * x + depthSlopeY * y + depthOffset,
    /// where a smaller depth is closer to the observer
    public depthSlopeX = 0.0;
    public depthSlopeY = 0.0;
    public depthOffset = 0.0;
    /// Key of the preliminary depth sort: the nearest depth of the polygon
    public nearestDepth = 0.0;
    public body: SimpleBody | null = null;
    public face: _PolyhedralBoundedSolidFace | null = null;
    public worldPlane: InfinitePlane | null = null;
    /// False if the near, far or window clips of the view removed part of
    /// the face
    public complete = false;
    /// Polygon of the whole view this one was clipped from by the screen
    /// subdivision (itself if it was not clipped)
    public original: _WeilerAthertonSourcePolygon = this;

    public depthAt(x: number, y: number): number {
        return this.depthSlopeX * x + this.depthSlopeY * y + this.depthOffset;
    }
}

/**
Part of a `_WeilerAthertonSourcePolygon` left by the x-y clips: the
elements of the inside and outside lists of [WEIL1977].216, figure 2.

Package-private in Java; exported only for traceability.
*/
export class _WeilerAthertonPiece extends RenderingElement {
    public readonly area: _WeilerAthertonArea;
    public readonly source: _WeilerAthertonSourcePolygon;

    public constructor(area: _WeilerAthertonArea, source: _WeilerAthertonSourcePolygon) {
        super();
        this.area = area;
        this.source = source;
    }
}

/**
Areas of a face of a solid reached by a light (the "surface detail of a
lighter shade" of [WEIL1977].218), in world coordinates: the visible
polygons of a view from the light, transformed back to the plane of their
source face.

Package-private in Java; exported only for traceability.
*/
export class _WeilerAthertonLitFace extends RenderingElement {
    /// The whole face is visible from the light
    public whole = false;
    /// Otherwise, the lit regions: each one a list of contours (main
    /// contours and holes), each contour a list of points
    public readonly regions: ArrayList<ArrayList<ArrayList<Vector3Dd>>> = new ArrayList<
        ArrayList<ArrayList<Vector3Dd>>
    >();
}

/**
Uniform grid over the edges of a polygon, telling quickly whether a point is
near its border: the verification of the clips tests it at a number of
points that grows with the number of vertices.

Package-private in Java; exported only for traceability.
*/
export class _WeilerAthertonEdgeGrid extends RenderingElement {
    private readonly tolerance: number;
    private readonly minX: number;
    private readonly minY: number;
    private readonly cellWidth: number;
    private readonly cellHeight: number;
    private readonly columns: number;
    private readonly rows: number;
    /// For each cell, the edges {ax, ay, bx, by} near it
    private readonly cells: ArrayList<ArrayList<number[]> | null>;

    public constructor(polygon: Polygon2D, tolerance: number) {
        super();
        const box: number[] = HiddenSurfaceRenderer.boundingBox(polygon);
        let edges = 0;

        for (const loop of polygon.loops) {
            edges += loop.vertices.length;
        }
        this.tolerance = tolerance;
        this.minX = box[0]! - tolerance;
        this.minY = box[1]! - tolerance;
        const side: number = Math.max(1, Math.trunc(Math.sqrt(edges)));
        this.columns = side;
        this.rows = side;
        this.cellWidth = Math.max((box[2]! + tolerance - this.minX) / this.columns, Double.MIN_NORMAL);
        this.cellHeight = Math.max((box[3]! + tolerance - this.minY) / this.rows, Double.MIN_NORMAL);
        this.cells = new ArrayList<ArrayList<number[]> | null>(this.columns * this.rows);
        for (let i = 0; i < this.columns * this.rows; i++) {
            this.cells.add(null);
        }
        for (const loop of polygon.loops) {
            const n: number = loop.vertices.length;
            for (let i = 0; i < n; i++) {
                const a: Vertex2D = loop.vertices[i]!;
                const b: Vertex2D = loop.vertices[(i + 1) % n]!;
                const edge: number[] = [a.x, a.y, b.x, b.y];
                const c0: number = this.column(Math.min(a.x, b.x) - tolerance);
                const c1: number = this.column(Math.max(a.x, b.x) + tolerance);
                const r0: number = this.row(Math.min(a.y, b.y) - tolerance);
                const r1: number = this.row(Math.max(a.y, b.y) + tolerance);
                for (let r = r0; r <= r1; r++) {
                    for (let c = c0; c <= c1; c++) {
                        let cell: ArrayList<number[]> | null = this.cells.get(r * this.columns + c);
                        if (cell === null) {
                            cell = new ArrayList<number[]>();
                            this.cells.set(r * this.columns + c, cell);
                        }
                        cell.add(edge);
                    }
                }
            }
        }
    }

    private column(x: number): number {
        return Math.max(0, Math.min(this.columns - 1, Math.trunc((x - this.minX) / this.cellWidth)));
    }

    private row(y: number): number {
        return Math.max(0, Math.min(this.rows - 1, Math.trunc((y - this.minY) / this.cellHeight)));
    }

    /**
    @return true if the point is at a distance from the border of the
    polygon not greater than the tolerance
    */
    public isNearBorder(x: number, y: number): boolean {
        if (
            x < this.minX ||
            y < this.minY ||
            x > this.minX + this.columns * this.cellWidth ||
            y > this.minY + this.rows * this.cellHeight
        ) {
            return false;
        }
        const cell: ArrayList<number[]> | null = this.cells.get(this.row(y) * this.columns + this.column(x));
        if (cell === null) {
            return false;
        }
        const tolerance2: number = this.tolerance * this.tolerance;
        for (const e of cell) {
            const dx: number = e[2]! - e[0]!;
            const dy: number = e[3]! - e[1]!;
            const length2: number = dx * dx + dy * dy;
            const t: number =
                length2 > 0.0 ? Math.max(0.0, Math.min(1.0, ((x - e[0]!) * dx + (y - e[1]!) * dy) / length2)) : 0.0;
            const ex: number = e[0]! + t * dx - x;
            const ey: number = e[1]! + t * dy - y;
            if (ex * ex + ey * ey <= tolerance2) {
                return true;
            }
        }
        return false;
    }
}

/**
Part of a visible area of a face, with the lights that reach it.

Package-private in Java; exported only for traceability.
*/
export class _WeilerAthertonShadedPart extends RenderingElement {
    public readonly area: _WeilerAthertonArea;
    public readonly lights: ArrayList<Light>;

    public constructor(area: _WeilerAthertonArea, lights: ArrayList<Light>) {
        super();
        this.area = area;
        this.lights = lights;
    }
}

/**
This class implements the hidden surface removal algorithm by polygon area
sorting of Weiler and Atherton [WEIL1977]: the hidden surface problem is
solved in polygon shaped windows of the image, taken from the input
polygons themselves, using the general 2D polygon clipper of the same paper
(`WeilerAthertonPolygonClipper`), which clips a concave polygon with holes
to the area of another concave polygon with holes.

The input are the faces of a set of `PolyhedralBoundedSolid`s. As required
by [WEIL1977].214, they are first transformed to the viewing space, where
the X and Y axes are parallel to the display surface and Z is parallel to
the line of sight: here, the normalized device coordinates of the camera
(with x and y scaled by `SCALE`), after clipping in homogeneous coordinates
[FOLE1992].6 against the near and far planes. The projective
transformation keeps planes as planes, so each face keeps a planar depth
function over its x-y area, and the depth of any point given by the 2D
clipper (that only computes x and y) is recovered from the plane of its
source face ("depth information can be preserved in all output",
[WEIL1977].217). The faces facing away from the observer are removed
beforehand (the "backplane removal" of [WEIL1977].221), and the faces are
clipped to the window of the view (the "viewbox clipping") with the same 2D
clipper.

The four steps of [WEIL1977].215 follow:
  a) A preliminary rough depth sort, by the nearest depth of each polygon.
  b) An x-y polygon area sort to the area of the currently most forward
     polygon: the remaining polygons are clipped to it, giving an inside
     list and an outside list (figure 2).
  c) A depth sort by removal of the polygons of the inside list located
     behind the current clip polygon.
  d) A conclusive depth sort by recursive subdivision when a polygon of the
     inside list is in front of the clip polygon, using it (its original
     copy) as the clip polygon and the inside list as input (figure 3). A
     stack of the polygons used as clip polygons detects the cyclic overlap
     (figure 4a), where no additional recursion is needed. Then the inside
     list is output and the process repeats with the outside list.

The output never overlaps on the x-y plane, as each visible area had all
the polygons behind it removed, so it solves the hidden lines as well as the
hidden surfaces [WEIL1977].215.

As the number of sorting steps can grow exponentially with the number of
polygons, the view is first divided by the screen subdivision of
[WEIL1977].219 in windows with a bounded number of polygons, each one solved
separately (see `solveWindow`). Visible areas of a face crossing several
windows are output in several pieces.

Two additions to [WEIL1977] make it usable with the solids of a scene:
  - Polygons that penetrate each other are not considered by [WEIL1977]
    (between two polygons in a window, one is entirely in front of the
    other). As solids of a scene may intersect, a piece of the inside list
    that lies in front of the clip polygon in part, and behind it in the
    other part, is divided by the projection of the intersection line of
    both planes (where their depth functions are equal), and only its front
    part is kept, so the steps c) and d) work as in [WEIL1977] with it.
  - The pieces have most of their vertices on the borders of previous clip
    polygons, the degenerate case of the clipper where "care is taken in
    placement of intersections where the subject and clip polygon contours
    are identical" [WEIL1977].218. Each clip is verified (the inside and
    outside areas must add up to the area of the subject, and sample points
    of the polygons must fall in the right part) and, if it fails, it is
    repeated with the clip polygon moved a distance far under the size of a
    pixel, which turns the degenerate contacts into regular ones. The strips
    thinner than `SLIVER_WIDTH` that such moves can leave are discarded. If
    every attempt fails, a convex clip polygon (the faces of boxes and the
    facets of spheres, the half planes of the penetrations) is solved by the
    half planes of its edges, as the Sutherland-Hodgman clipper that
    [WEIL1977].217 cites, and a concave one by the half planes of the convex
    trapezoids it is decomposed in.

Shadows are cast as in [WEIL1977].218 ("Shadowing"), taking advantage of the
output having the same form as the input: a hidden surface removed view of
the scene is produced from the position of each light, and its visible
polygons, transformed back to the original space, are the lit areas of their
source faces (surface detail of a lighter shade, [WEIL1977].219). Then the
hidden surface removed view from the observer is produced, and each visible
area is clipped with the same polygon clipper to the lit areas of its face,
for each light, so its parts know which lights reach them. A point light is
seen with one perspective view enclosing the scene or, if it is among the
solids, with the six views of the faces of a cube around it (slightly
overlapped); a directional light with an orthogonal view along its
direction. Ambient lights cast no
shadows.

Port of `vsdk.toolkit.render.hiddenSurface.HiddenSurfaceRenderer`, method by
method. Java's `IdentityHashMap`s are JavaScript `Map`s (keyed by object
identity), Java's `double[]` are `number[]`, the overloads of `signedArea`
and `perimeter` are one method each dispatching on the argument type, and
the package-private static helpers are public. Java's public nested class
`VisiblePolygon` is a member of the `HiddenSurfaceRenderer` namespace below.
*/
export class HiddenSurfaceRenderer extends RenderingElement {
    /// Units of the viewing space for each unit of the normalized device
    /// coordinates: the tolerances of the 2D clipper are absolute (1e-4), so
    /// with this scale they are around 1e-5 pixels of a usual view
    private static readonly SCALE = 1.0e4;
    /// Tolerance to compare depths in the viewing space (normalized device
    /// coordinates, from -1 to 1)
    private static readonly DEPTH_EPSILON = 1.0e-10;
    /// Areas (in square units of the viewing space) under this are degenerate
    private static readonly AREA_EPSILON = 1.0e-6;
    /// Contours thinner than this (area over half perimeter, in units of the
    /// viewing space) are degenerate strips
    private static readonly SLIVER_WIDTH = 1.0e-3;
    /// Distance the clip polygon is moved in the first retry of a failed
    /// clip, in units of the viewing space; it grows in each retry
    private static readonly RETRY_SHIFT = 1.0e-2;
    private static readonly MAXIMUM_RETRIES = 6;
    /// Most polygons solved together in a window of the screen subdivision
    private static readonly MAXIMUM_POLYGONS_PER_WINDOW = 32;
    /// Deepest level of the screen subdivision (4^8 windows at most)
    private static readonly MAXIMUM_SUBDIVISION_DEPTH = 8;
    /// Edges shorter than this (in units of the viewing space) are ignored by
    /// the clips by half planes
    private static readonly MINIMUM_EDGE_LENGTH = 1.0e-6;

    private static readonly BEHIND = 0;
    private static readonly IN_FRONT = 1;
    private static readonly PENETRATING = 2;

    /// Relative margin of the views from the lights around the scene
    private static readonly LIGHT_VIEW_MARGIN = 1.05;
    /// Largest half angle (radians) of the scene seen from a point light
    /// covered with one view; beyond it, six views of a cube are used
    private static readonly MAXIMUM_LIGHT_HALF_ANGLE = JavaMath.toRadians(55.0);
    /// Degrees added to the field of view of the six views of a cube
    private static readonly CUBE_VIEW_OVERLAP = 2.0;
    /// Size (in pixels) of the views from the lights: only their aspect
    /// ratio matters
    private static readonly LIGHT_VIEW_SIZE = 256;

    private readonly clipper: WeilerAthertonPolygonClipper;
    private readonly clipStack: ArrayList<_WeilerAthertonSourcePolygon>;
    /// Visible polygons, in the viewing space
    private readonly output: ArrayList<_WeilerAthertonPiece>;

    private constructor() {
        super();
        this.clipper = new WeilerAthertonPolygonClipper();
        this.clipStack = new ArrayList<_WeilerAthertonSourcePolygon>();
        this.output = new ArrayList<_WeilerAthertonPiece>();
    }

    /**
    Solves the visible surfaces of a set of solids seen from a camera.
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view
    */
    public static executeWeilerAthertonAlgorithm(
        solids: ArrayList<SimpleBody> | readonly SimpleBody[],
        camera: Camera,
    ): ArrayList<HiddenSurfaceRenderer.VisiblePolygon>;
    /**
    Solves the visible surfaces of a set of solids seen from a camera, with
    the shadows cast by a set of lights, as in [WEIL1977].218 (see the class
    documentation).
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored, and cast no shadows)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @param lights lights of the scene
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view, each one with the lights that reach it
    */
    public static executeWeilerAthertonAlgorithm(
        solids: ArrayList<SimpleBody> | readonly SimpleBody[],
        camera: Camera,
        lights: ArrayList<Light> | readonly Light[],
    ): ArrayList<HiddenSurfaceRenderer.VisiblePolygon>;
    public static executeWeilerAthertonAlgorithm(
        solids: ArrayList<SimpleBody> | readonly SimpleBody[],
        camera: Camera,
        lights?: ArrayList<Light> | readonly Light[],
    ): ArrayList<HiddenSurfaceRenderer.VisiblePolygon> {
        if (lights === undefined) {
            const renderer: HiddenSurfaceRenderer = HiddenSurfaceRenderer.solve(solids, camera);
            const visible = new ArrayList<HiddenSurfaceRenderer.VisiblePolygon>();

            for (const piece of renderer.output) {
                visible.add(
                    new HiddenSurfaceRenderer.VisiblePolygon(
                        HiddenSurfaceRenderer.toNormalizedDeviceCoordinates(piece.area.polygon),
                        piece.source,
                        null,
                    ),
                );
            }
            return visible;
        }

        const renderer: HiddenSurfaceRenderer = HiddenSurfaceRenderer.solve(solids, camera);
        const ambientLights = new ArrayList<Light>();
        const castingLights = new ArrayList<Light>();
        const litFaces = new ArrayList<Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>>();
        const bounds: number[] | null = HiddenSurfaceRenderer.boundingSphere(solids);
        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        const visible = new ArrayList<HiddenSurfaceRenderer.VisiblePolygon>();

        // First pass: hidden surface removed views from the lights
        for (const light of lights) {
            if (light.isAmbient()) {
                ambientLights.add(light);
            } else if (bounds !== null) {
                castingLights.add(light);
                litFaces.add(HiddenSurfaceRenderer.litFacesFromLight(solids, light, bounds));
            }
        }

        // Second pass: the visible areas from the observer, divided by the
        // lit areas of their faces
        for (const piece of renderer.output) {
            let parts = new ArrayList<_WeilerAthertonShadedPart>();
            parts.add(new _WeilerAthertonShadedPart(piece.area, new ArrayList<Light>(ambientLights)));
            for (let k = 0; k < castingLights.size(); k++) {
                const faces: Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace> | undefined = litFaces
                    .get(k)
                    .get(piece.source.body!);
                const lit: _WeilerAthertonLitFace | null =
                    faces !== undefined ? (faces.get(piece.source.face!) ?? null) : null;
                if (lit !== null) {
                    parts = renderer.divideByLitArea(parts, lit, castingLights.get(k), projection);
                }
            }
            for (const part of parts) {
                visible.add(
                    new HiddenSurfaceRenderer.VisiblePolygon(
                        HiddenSurfaceRenderer.toNormalizedDeviceCoordinates(part.area.polygon),
                        piece.source,
                        part.lights,
                    ),
                );
            }
        }
        return visible;
    }

    /**
    Runs the hidden surface removal of [WEIL1977] for a view.
    @return the renderer, with the visible polygons in its output
    */
    private static solve(solids: ArrayList<SimpleBody> | readonly SimpleBody[], camera: Camera): HiddenSurfaceRenderer {
        const renderer = new HiddenSurfaceRenderer();

        renderer.solveWindow(
            -HiddenSurfaceRenderer.SCALE,
            -HiddenSurfaceRenderer.SCALE,
            HiddenSurfaceRenderer.SCALE,
            HiddenSurfaceRenderer.SCALE,
            renderer.buildInputPolygons(solids, camera),
            0,
        );
        return renderer;
    }

    //= Screen subdivision ================================================

    /**
    Screen subdivision of [WEIL1977].219: "by taking the approach of a
    Warnock style screen area subdivision, the image can be divided into
    areas each containing a specified maximum number of polygons", each one
    processed separately by the hidden surface removal. This bounds the
    exponential growth of the number of sorting steps with the number of
    polygons, so the whole process becomes almost linear (the subdivision
    itself grows as n log n).

    A window with more than `MAXIMUM_POLYGONS_PER_WINDOW` polygons is divided
    in four quadrants, and the polygons are clipped to each of them (to a
    convex window, by its half planes). The division stops at
    `MAXIMUM_SUBDIVISION_DEPTH` levels, and also in a quadrant where it did
    not reduce the number of polygons (all of them cover it, as a floor seen
    from above).
    @param sources the polygons of the window, clipped to it
    @param depth level of the window in the subdivision
    */
    private solveWindow(
        minX: number,
        minY: number,
        maxX: number,
        maxY: number,
        sources: ArrayList<_WeilerAthertonSourcePolygon>,
        depth: number,
    ): void {
        if (
            sources.size() <= HiddenSurfaceRenderer.MAXIMUM_POLYGONS_PER_WINDOW ||
            depth >= HiddenSurfaceRenderer.MAXIMUM_SUBDIVISION_DEPTH
        ) {
            this.solveArea(sources);
            return;
        }
        const middleX: number = (minX + maxX) / 2.0;
        const middleY: number = (minY + maxY) / 2.0;
        const quadrants: number[][] = [
            [minX, minY, middleX, middleY],
            [middleX, minY, maxX, middleY],
            [minX, middleY, middleX, maxY],
            [middleX, middleY, maxX, maxY],
        ];
        for (const q of quadrants) {
            const inside = new ArrayList<_WeilerAthertonSourcePolygon>();
            for (const source of sources) {
                const clipped: _WeilerAthertonSourcePolygon | null = HiddenSurfaceRenderer.clippedToWindow(
                    source,
                    q[0]!,
                    q[1]!,
                    q[2]!,
                    q[3]!,
                );
                if (clipped !== null) {
                    inside.add(clipped);
                }
            }
            if (inside.size() < sources.size()) {
                this.solveWindow(q[0]!, q[1]!, q[2]!, q[3]!, inside, depth + 1);
            } else {
                this.solveArea(inside);
            }
        }
    }

    /**
    @return the part of a polygon inside a window (the same polygon if it is
    entirely inside), or null if no part is inside
    */
    private static clippedToWindow(
        source: _WeilerAthertonSourcePolygon,
        minX: number,
        minY: number,
        maxX: number,
        maxY: number,
    ): _WeilerAthertonSourcePolygon | null {
        const template: _WeilerAthertonArea = source.template;

        if (template.maxX <= minX || template.minX >= maxX || template.maxY <= minY || template.minY >= maxY) {
            return null;
        }
        if (template.minX >= minX && template.maxX <= maxX && template.minY >= minY && template.maxY <= maxY) {
            return source;
        }
        let polygon: Polygon2D | null = HiddenSurfaceRenderer.clipByHalfPlane(template.polygon, 1.0, 0.0, -minX);
        if (polygon !== null) {
            polygon = HiddenSurfaceRenderer.clipByHalfPlane(polygon, -1.0, 0.0, maxX);
        }
        if (polygon !== null) {
            polygon = HiddenSurfaceRenderer.clipByHalfPlane(polygon, 0.0, 1.0, -minY);
        }
        if (polygon !== null) {
            polygon = HiddenSurfaceRenderer.clipByHalfPlane(polygon, 0.0, -1.0, maxY);
        }
        const parts = new ArrayList<_WeilerAthertonArea>();
        HiddenSurfaceRenderer.addWithoutSlivers(parts, polygon);
        if (parts.isEmpty()) {
            return null;
        }

        const clipped = new _WeilerAthertonSourcePolygon();
        clipped.template = HiddenSurfaceRenderer.merged(parts);
        clipped.depthSlopeX = source.depthSlopeX;
        clipped.depthSlopeY = source.depthSlopeY;
        clipped.depthOffset = source.depthOffset;
        clipped.body = source.body;
        clipped.face = source.face;
        clipped.worldPlane = source.worldPlane;
        clipped.complete = false;
        clipped.original = source.original;
        clipped.nearestDepth = Double.MAX_VALUE;
        for (const loop of clipped.template.polygon.loops) {
            for (const v of loop.vertices) {
                clipped.nearestDepth = Math.min(clipped.nearestDepth, clipped.depthAt(v.x, v.y));
            }
        }
        return clipped;
    }

    /**
    Hidden surface removal of the polygons of a window, as in
    [WEIL1977].215-216.
    */
    private solveArea(sources: ArrayList<_WeilerAthertonSourcePolygon>): void {
        let polygonList: ArrayList<_WeilerAthertonPiece>;
        let i: number;

        sources = new ArrayList<_WeilerAthertonSourcePolygon>(sources);

        // Step a) of [WEIL1977].215: preliminary rough depth sort, from the
        // polygons closest to the observer to those furthest away
        Collections.sort(sources, {
            compare: (p: _WeilerAthertonSourcePolygon, q: _WeilerAthertonSourcePolygon): number =>
                Double.compare(p.nearestDepth, q.nearestDepth),
        });

        polygonList = new ArrayList<_WeilerAthertonPiece>();
        for (i = 0; i < sources.size(); i++) {
            polygonList.add(new _WeilerAthertonPiece(sources.get(i).template, sources.get(i)));
        }

        // [WEIL1977].216: the process is continued until the outside list
        // is exhausted
        while (!polygonList.isEmpty()) {
            polygonList = this.subdivide(polygonList.get(0).source, polygonList);
        }
    }

    //= Shadows ===========================================================

    /**
    Shadowing pass of [WEIL1977].218 for one light: "a hidden surface removed
    view of a scene from the position of the light source", whose visible
    polygons are "transformed back to the original space" as the lit areas
    of their source faces.
    @param bounds bounding sphere of the solids, as {x, y, z, radius}
    @return the lit areas of the faces reached by the light, by body and face
    */
    private static litFacesFromLight(
        solids: ArrayList<SimpleBody> | readonly SimpleBody[],
        light: Light,
        bounds: number[],
    ): Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>> {
        const lit = new Map<SimpleBody, Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>>();

        // A light inside a solid sees only its inner side, the back of its
        // faces: it reaches nothing outside of it
        if (!(light instanceof DirectionalLight) && HiddenSurfaceRenderer.isInsideASolid(solids, light.getPosition())) {
            return lit;
        }
        for (const camera of HiddenSurfaceRenderer.lightCameras(light, bounds)) {
            const renderer: HiddenSurfaceRenderer = HiddenSurfaceRenderer.solve(solids, camera);
            const inverse: Matrix4x4d = camera.calculateProjectionMatrix().inverse();
            const visibleAreas = new Map<_WeilerAthertonSourcePolygon, number[]>();

            // The visible areas of a face, added over the windows of the
            // screen subdivision
            for (const piece of renderer.output) {
                let sum: number[] | undefined = visibleAreas.get(piece.source.original);
                if (sum === undefined) {
                    sum = [0.0];
                    visibleAreas.set(piece.source.original, sum);
                }
                sum[0] = sum[0]! + piece.area.area;
            }
            for (const piece of renderer.output) {
                const source: _WeilerAthertonSourcePolygon = piece.source;
                let faces: Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace> | undefined = lit.get(source.body!);
                if (faces === undefined) {
                    faces = new Map<_PolyhedralBoundedSolidFace, _WeilerAthertonLitFace>();
                    lit.set(source.body!, faces);
                }
                let litFace: _WeilerAthertonLitFace | undefined = faces.get(source.face!);
                if (litFace === undefined) {
                    litFace = new _WeilerAthertonLitFace();
                    faces.set(source.face!, litFace);
                }
                if (litFace.whole) {
                    continue;
                }
                // A face entirely in the view and not hidden at all is lit
                // whole: its visible pieces add up to its area (but for the
                // slivers the screen subdivision may discard)
                const original: _WeilerAthertonSourcePolygon = source.original;
                if (
                    original.complete &&
                    visibleAreas.get(original)![0]! >=
                        original.template.area * (1.0 - 1.0e-9) -
                            HiddenSurfaceRenderer.AREA_EPSILON -
                            HiddenSurfaceRenderer.SLIVER_WIDTH * original.template.perimeter
                ) {
                    litFace.whole = true;
                    litFace.regions.clear();
                    continue;
                }
                const region = new ArrayList<ArrayList<Vector3Dd>>();
                for (const loop of piece.area.polygon.loops) {
                    const contour = new ArrayList<Vector3Dd>();
                    for (const v of loop.vertices) {
                        const p: Vector3Dd | null = HiddenSurfaceRenderer.unprojectToPlane(
                            inverse,
                            v.x / HiddenSurfaceRenderer.SCALE,
                            v.y / HiddenSurfaceRenderer.SCALE,
                            source.worldPlane!,
                        );
                        if (p !== null) {
                            contour.add(p);
                        }
                    }
                    if (contour.size() >= 3) {
                        region.add(contour);
                    }
                }
                if (!region.isEmpty()) {
                    litFace.regions.add(region);
                }
            }
        }
        return lit;
    }

    private static isInsideASolid(solids: ArrayList<SimpleBody> | readonly SimpleBody[], p: Vector3Dd): boolean {
        for (const body of solids) {
            const brep = body.getGeometry();
            if (
                brep instanceof PolyhedralBoundedSolid &&
                brep.doContainmentTest(body.getTransformationMatrix().inverse().multiply(p), VSDK.EPSILON) ===
                    Geometry.INSIDE
            ) {
                return true;
            }
        }
        return false;
    }

    /**
    @return the point of a plane seen at a point of the x-y plane of the
    normalized device coordinates of a view, or null if the line of sight
    is parallel to the plane
    */
    private static unprojectToPlane(
        inverseProjection: Matrix4x4d,
        x: number,
        y: number,
        plane: InfinitePlane,
    ): Vector3Dd | null {
        const a: Vector4Dd = inverseProjection.multiply(new Vector4Dd(x, y, -0.5, 1.0)).dividedByW();
        const b: Vector4Dd = inverseProjection.multiply(new Vector4Dd(x, y, 0.5, 1.0)).dividedByW();
        const p = new Vector3Dd(a.x(), a.y(), a.z());
        const direction = new Vector3Dd(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
        const n: Vector3Dd = plane.getNormal();
        const denominator: number = n.dotProduct(direction);

        if (Math.abs(denominator) <= 1.0e-300) {
            return null;
        }
        const t: number = -(n.dotProduct(p) + plane.getD()) / denominator;
        return p.add(direction.multiply(t));
    }

    /**
    Views from a light enclosing the scene: one perspective view, or the six
    views of the faces of a cube when the light is among the solids, for a
    point (or spot) light, and one orthogonal view for a directional light.
    @param bounds bounding sphere of the solids, as {x, y, z, radius}
    */
    private static lightCameras(light: Light, bounds: number[]): ArrayList<Camera> {
        const cameras = new ArrayList<Camera>();
        const center = new Vector3Dd(bounds[0]!, bounds[1]!, bounds[2]!);
        const radius: number = bounds[3]! * HiddenSurfaceRenderer.LIGHT_VIEW_MARGIN + VSDK.EPSILON;

        if (light instanceof DirectionalLight) {
            // The position of a directional light is its direction
            const direction: Vector3Dd = light.getPosition().normalized();
            const camera: Camera = HiddenSurfaceRenderer.lightCamera(
                center.subtract(direction.multiply(2.0 * radius)),
                direction,
            );
            camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
            camera.setOrthogonalZoom(1.0 / radius);
            camera.setNearPlaneDistance(0.5 * radius);
            camera.setFarPlaneDistance(3.5 * radius);
            camera.updateViewportResize(HiddenSurfaceRenderer.LIGHT_VIEW_SIZE, HiddenSurfaceRenderer.LIGHT_VIEW_SIZE);
            cameras.add(camera);
            return cameras;
        }

        const position: Vector3Dd = light.getPosition();
        const toCenter: Vector3Dd = center.subtract(position);
        const distance: number = toCenter.length();
        if (distance > radius && Math.asin(radius / distance) <= HiddenSurfaceRenderer.MAXIMUM_LIGHT_HALF_ANGLE) {
            const camera: Camera = HiddenSurfaceRenderer.lightCamera(position, toCenter.normalized());
            camera.setFov(
                2.0 * JavaMath.toDegrees(Math.asin(radius / distance)) * HiddenSurfaceRenderer.LIGHT_VIEW_MARGIN,
            );
            camera.setNearPlaneDistance(0.5 * (distance - radius));
            camera.setFarPlaneDistance(2.0 * (distance + radius));
            cameras.add(camera);
        } else {
            const directions: Vector3Dd[] = [
                new Vector3Dd(1.0, 0.0, 0.0),
                new Vector3Dd(-1.0, 0.0, 0.0),
                new Vector3Dd(0.0, 1.0, 0.0),
                new Vector3Dd(0.0, -1.0, 0.0),
                new Vector3Dd(0.0, 0.0, 1.0),
                new Vector3Dd(0.0, 0.0, -1.0),
            ];
            // The views overlap a little, so no lit area is lost on their
            // common borders (the lit regions of a face may overlap: see
            // `divideByLitArea`)
            for (const direction of directions) {
                const camera: Camera = HiddenSurfaceRenderer.lightCamera(position, direction);
                camera.setFov(90.0 + HiddenSurfaceRenderer.CUBE_VIEW_OVERLAP);
                camera.setNearPlaneDistance(1.0e-3 * (distance + radius));
                camera.setFarPlaneDistance(2.0 * (distance + radius));
                cameras.add(camera);
            }
        }
        for (const camera of cameras) {
            camera.updateViewportResize(HiddenSurfaceRenderer.LIGHT_VIEW_SIZE, HiddenSurfaceRenderer.LIGHT_VIEW_SIZE);
        }
        return cameras;
    }

    /**
    @return a camera at a position looking along a direction
    */
    private static lightCamera(position: Vector3Dd, front: Vector3Dd): Camera {
        const camera = new Camera();
        const upHint: Vector3Dd =
            Math.abs(front.z()) < 0.9 ? new Vector3Dd(0.0, 0.0, 1.0) : new Vector3Dd(1.0, 0.0, 0.0);
        const left: Vector3Dd = upHint.crossProduct(front).normalized();
        const up: Vector3Dd = front.crossProduct(left).normalized();

        camera.setPosition(position);
        camera.setRotation(
            new Matrix4x4d()
                .withVal(0, 0, front.x())
                .withVal(0, 1, left.x())
                .withVal(0, 2, up.x())
                .withVal(1, 0, front.y())
                .withVal(1, 1, left.y())
                .withVal(1, 2, up.y())
                .withVal(2, 0, front.z())
                .withVal(2, 1, left.z())
                .withVal(2, 2, up.z()),
        );
        camera.updateViewportResize(HiddenSurfaceRenderer.LIGHT_VIEW_SIZE, HiddenSurfaceRenderer.LIGHT_VIEW_SIZE);
        return camera;
    }

    /**
    @return the bounding sphere of the vertices of the solids, as {x, y, z,
    radius}, or null if they have no vertices
    */
    private static boundingSphere(solids: ArrayList<SimpleBody> | readonly SimpleBody[]): number[] | null {
        const box: number[] = [
            Double.MAX_VALUE,
            Double.MAX_VALUE,
            Double.MAX_VALUE,
            -Double.MAX_VALUE,
            -Double.MAX_VALUE,
            -Double.MAX_VALUE,
        ];
        const points = new ArrayList<Vector3Dd>();

        for (const body of solids) {
            const brep = body.getGeometry();
            if (!(brep instanceof PolyhedralBoundedSolid)) {
                continue;
            }
            const transform: Matrix4x4d = body.getTransformationMatrix();
            for (let i = 0; i < brep.getPolygonsList().size(); i++) {
                const face: _PolyhedralBoundedSolidFace = brep.getPolygonsList().get(i)!;
                for (let j = 0; j < face.boundariesList.size(); j++) {
                    const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(j) ?? null;
                    let he: _PolyhedralBoundedSolidHalfEdge | null = loop !== null ? loop.boundaryStartHalfEdge : null;
                    const heStart: _PolyhedralBoundedSolidHalfEdge | null = he;
                    if (he === null) {
                        continue;
                    }
                    do {
                        if (he === null || he.startingVertex === null) {
                            break;
                        }
                        const p: Vector3Dd = transform.multiply(he.startingVertex.position);
                        points.add(p);
                        box[0] = Math.min(box[0]!, p.x());
                        box[1] = Math.min(box[1]!, p.y());
                        box[2] = Math.min(box[2]!, p.z());
                        box[3] = Math.max(box[3]!, p.x());
                        box[4] = Math.max(box[4]!, p.y());
                        box[5] = Math.max(box[5]!, p.z());
                        he = he.next();
                    } while (he !== heStart);
                }
            }
        }
        if (points.isEmpty()) {
            return null;
        }
        const center = new Vector3Dd((box[0]! + box[3]!) / 2.0, (box[1]! + box[4]!) / 2.0, (box[2]! + box[5]!) / 2.0);
        let radius = 0.0;
        for (const p of points) {
            radius = Math.max(radius, p.subtract(center).length());
        }
        return [center.x(), center.y(), center.z(), radius];
    }

    /**
    Divides the parts of a visible area by the lit areas of its face for a
    light: the parts inside them get the light. The lit areas are projected
    to the viewing space of the observer and used as clip polygons.
    */
    private divideByLitArea(
        parts: ArrayList<_WeilerAthertonShadedPart>,
        lit: _WeilerAthertonLitFace,
        light: Light,
        projection: Matrix4x4d,
    ): ArrayList<_WeilerAthertonShadedPart> {
        const result = new ArrayList<_WeilerAthertonShadedPart>();

        if (lit.whole) {
            for (const part of parts) {
                result.add(
                    new _WeilerAthertonShadedPart(part.area, HiddenSurfaceRenderer.withLight(part.lights, light)),
                );
            }
            return result;
        }

        const regions = new ArrayList<_WeilerAthertonArea>();
        for (const region of lit.regions) {
            const area: _WeilerAthertonArea | null = HiddenSurfaceRenderer.projectRegion(region, projection);
            if (area !== null) {
                regions.add(area);
            }
        }
        for (const part of parts) {
            // Each lit region takes its part of what the previous ones left,
            // so regions that overlap (from the views of a cube) are counted
            // once
            let remaining = new ArrayList<_WeilerAthertonArea>();
            const litAreas = new ArrayList<_WeilerAthertonArea>();
            remaining.add(part.area);
            for (const region of regions) {
                const next = new ArrayList<_WeilerAthertonArea>();
                for (const area of remaining) {
                    if (!area.overlapsBox(region)) {
                        next.add(area);
                        continue;
                    }
                    this.verifiedClip(region, area, litAreas, next);
                }
                remaining = next;
            }
            if (!litAreas.isEmpty()) {
                const lights: ArrayList<Light> = HiddenSurfaceRenderer.withLight(part.lights, light);
                for (const area of litAreas) {
                    result.add(new _WeilerAthertonShadedPart(area, lights));
                }
            }
            for (const area of remaining) {
                result.add(new _WeilerAthertonShadedPart(area, part.lights));
            }
        }
        return result;
    }

    private static withLight(lights: ArrayList<Light>, light: Light): ArrayList<Light> {
        const result = new ArrayList<Light>(lights);
        result.add(light);
        return result;
    }

    /**
    Projects a lit region (in world coordinates) to the viewing space of the
    observer, clipped against its near and far planes.
    @return the region, with its main contours counterclockwise and its holes
    clockwise, or null if nothing of it is left
    */
    private static projectRegion(
        region: ArrayList<ArrayList<Vector3Dd>>,
        projection: Matrix4x4d,
    ): _WeilerAthertonArea | null {
        const polygon = new Polygon2D();
        polygon.loops.length = 0;

        for (const contour of region) {
            let clipLoop = new ArrayList<Vector4Dd>();
            for (const p of contour) {
                clipLoop.add(projection.multiply(new Vector4Dd(p)));
            }
            clipLoop = HiddenSurfaceRenderer.clipAgainstPlane(clipLoop, 1.0);
            clipLoop = HiddenSurfaceRenderer.clipAgainstPlane(clipLoop, -1.0);
            if (clipLoop.size() < 3) {
                continue;
            }
            const loop = new ArrayList<number[]>();
            for (const p of clipLoop) {
                const ndc: Vector4Dd = p.dividedByW();
                loop.add([HiddenSurfaceRenderer.SCALE * ndc.x(), HiddenSurfaceRenderer.SCALE * ndc.y()]);
            }
            if (Math.abs(HiddenSurfaceRenderer.signedArea(loop)) > HiddenSurfaceRenderer.AREA_EPSILON) {
                HiddenSurfaceRenderer.addLoop(polygon, loop);
            }
        }
        if (polygon.loops.length === 0) {
            return null;
        }
        // The light and the observer see the same side of the face, so the
        // orientation is kept; it is checked anyway
        if (HiddenSurfaceRenderer.enclosedArea(polygon) < 0.0) {
            for (const loop of polygon.loops) {
                Collections.reverse(loop.vertices);
            }
        }
        const area = new _WeilerAthertonArea(polygon);
        return area.area > HiddenSurfaceRenderer.AREA_EPSILON ? area : null;
    }

    private static toNormalizedDeviceCoordinates(polygon: Polygon2D): Polygon2D {
        const ndc = new Polygon2D();
        ndc.loops.length = 0;
        for (const loop of polygon.loops) {
            ndc.nextLoop();
            for (const v of loop.vertices) {
                ndc.addVertex(v.x / HiddenSurfaceRenderer.SCALE, v.y / HiddenSurfaceRenderer.SCALE);
            }
        }
        return ndc;
    }

    //= Hidden surface algorithm ==========================================

    /**
    Solves the hidden surface problem in the area of a clip polygon, as in
    [WEIL1977].215-216 (steps b, c and d), outputting the visible polygons of
    that area.
    @param clipPolygon source polygon whose original copy is the clip
    template
    @param polygonList polygons to sort
    @return the outside list: the parts of the polygons outside of the clip
    polygon, still to be solved
    */
    private subdivide(
        clipPolygon: _WeilerAthertonSourcePolygon,
        polygonList: ArrayList<_WeilerAthertonPiece>,
    ): ArrayList<_WeilerAthertonPiece> {
        let insideList = new ArrayList<_WeilerAthertonPiece>();
        const outsideList = new ArrayList<_WeilerAthertonPiece>();
        let i: number;

        // Step b): x-y polygon area sort to the area of the clip polygon
        for (i = 0; i < polygonList.size(); i++) {
            this.clipPiece(clipPolygon, polygonList.get(i), insideList, outsideList);
        }

        // Step c): removal of the polygons behind the clip polygon
        insideList = this.removePolygonsBehind(clipPolygon, insideList);

        // Step d): a polygon in front of the clip polygon reveals an error in
        // the initial depth sort: the region is subdivided recursively with
        // the offending polygon as clip polygon and the inside list as input
        // (figure 3). If it is on the stack, it is a cyclic overlap
        // (figure 4a), and all the material behind it was already removed.
        while (true) {
            let offending: _WeilerAthertonPiece | null = null;
            for (i = 0; i < insideList.size(); i++) {
                const piece: _WeilerAthertonPiece = insideList.get(i);
                if (piece.source !== clipPolygon && !this.clipStack.contains(piece.source)) {
                    offending = piece;
                    break;
                }
            }
            if (offending === null) {
                break;
            }
            this.clipStack.add(clipPolygon);
            insideList = this.subdivide(offending.source, insideList);
            this.clipStack.remove(this.clipStack.size() - 1);
        }

        // The inside list is displayed: first the parts of the clip polygon,
        // then the parts of the cyclic overlaps, which are in front of it
        for (i = 0; i < insideList.size(); i++) {
            if (insideList.get(i).source === clipPolygon) {
                this.display(insideList.get(i));
            }
        }
        for (i = 0; i < insideList.size(); i++) {
            if (insideList.get(i).source !== clipPolygon) {
                this.display(insideList.get(i));
            }
        }
        return outsideList;
    }

    /**
    Clips a polygon to the area of the original copy of the clip polygon,
    placing its parts on the inside and outside lists.
    */
    private clipPiece(
        clipPolygon: _WeilerAthertonSourcePolygon,
        piece: _WeilerAthertonPiece,
        insideList: ArrayList<_WeilerAthertonPiece>,
        outsideList: ArrayList<_WeilerAthertonPiece>,
    ): void {
        // The parts of a polygon are inside its own original copy
        if (piece.source === clipPolygon) {
            insideList.add(piece);
            return;
        }
        // Areas that do not overlap need no clip
        if (!piece.area.overlapsBox(clipPolygon.template)) {
            outsideList.add(piece);
            return;
        }

        const inside = new ArrayList<_WeilerAthertonArea>();
        const outside = new ArrayList<_WeilerAthertonArea>();
        this.verifiedClip(clipPolygon.template, piece.area, inside, outside);
        for (const area of inside) {
            insideList.add(new _WeilerAthertonPiece(area, piece.source));
        }
        for (const area of outside) {
            outsideList.add(new _WeilerAthertonPiece(area, piece.source));
        }
    }

    /**
    Step c) of [WEIL1977].216: the polygons of the inside list located behind
    the clip polygon are hidden from view. A polygon partially in front of
    it (penetrating it) is divided, and only its front part is kept.
    @return the new inside list
    */
    private removePolygonsBehind(
        clipPolygon: _WeilerAthertonSourcePolygon,
        insideList: ArrayList<_WeilerAthertonPiece>,
    ): ArrayList<_WeilerAthertonPiece> {
        const remaining = new ArrayList<_WeilerAthertonPiece>();
        let i: number;

        for (i = 0; i < insideList.size(); i++) {
            const piece: _WeilerAthertonPiece = insideList.get(i);
            if (piece.source === clipPolygon) {
                remaining.add(piece);
                continue;
            }
            switch (HiddenSurfaceRenderer.classifyDepth(clipPolygon, piece)) {
                case HiddenSurfaceRenderer.IN_FRONT:
                    remaining.add(piece);
                    break;
                case HiddenSurfaceRenderer.PENETRATING:
                    this.addFrontPart(clipPolygon, piece, remaining);
                    break;
                case HiddenSurfaceRenderer.BEHIND:
                default:
                    break;
            }
        }
        return remaining;
    }

    /**
    The difference between the depth of a piece and the depth of the clip
    polygon is an affine function over the x-y plane (both are planes in the
    viewing space), so its extremes over the piece are at its vertices.
    @return BEHIND if no part of the piece is in front of the clip polygon
    (including the coplanar case), IN_FRONT if no part is behind it, or
    PENETRATING otherwise
    */
    private static classifyDepth(clipPolygon: _WeilerAthertonSourcePolygon, piece: _WeilerAthertonPiece): number {
        let front = false;
        let behind = false;

        for (const loop of piece.area.polygon.loops) {
            for (const v of loop.vertices) {
                const difference: number = piece.source.depthAt(v.x, v.y) - clipPolygon.depthAt(v.x, v.y);
                if (difference < -HiddenSurfaceRenderer.DEPTH_EPSILON) {
                    front = true;
                } else if (difference > HiddenSurfaceRenderer.DEPTH_EPSILON) {
                    behind = true;
                }
            }
        }
        if (front && behind) {
            return HiddenSurfaceRenderer.PENETRATING;
        }
        return front ? HiddenSurfaceRenderer.IN_FRONT : HiddenSurfaceRenderer.BEHIND;
    }

    /**
    Divides a piece penetrating the clip polygon by the projection of the
    intersection of their planes, where their depths are equal, keeping
    only the part in front of the clip polygon. The half plane in front is
    taken as a convex polygon covering the piece, to which the piece is
    clipped.
    */
    private addFrontPart(
        clipPolygon: _WeilerAthertonSourcePolygon,
        piece: _WeilerAthertonPiece,
        remaining: ArrayList<_WeilerAthertonPiece>,
    ): void {
        // Points with da*x + db*y + dc < 0 are in front of the clip polygon
        const da: number = piece.source.depthSlopeX - clipPolygon.depthSlopeX;
        const db: number = piece.source.depthSlopeY - clipPolygon.depthSlopeY;
        const dc: number = piece.source.depthOffset - clipPolygon.depthOffset;
        const area: _WeilerAthertonArea = piece.area;
        const margin: number = 1.0 + 0.01 * (area.maxX - area.minX + (area.maxY - area.minY));
        const box: number[][] = [
            [area.minX - margin, area.minY - margin],
            [area.maxX + margin, area.minY - margin],
            [area.maxX + margin, area.maxY + margin],
            [area.minX - margin, area.maxY + margin],
        ];
        const halfPlane = new ArrayList<number[]>();
        let i: number;

        // Sutherland-Hodgman clip of the (convex) box by the line
        for (i = 0; i < box.length; i++) {
            const p: number[] = box[i]!;
            const q: number[] = box[(i + 1) % box.length]!;
            const fp: number = da * p[0]! + db * p[1]! + dc;
            const fq: number = da * q[0]! + db * q[1]! + dc;
            if (fp <= 0.0) {
                halfPlane.add(p);
            }
            if ((fp < 0.0 && fq > 0.0) || (fp > 0.0 && fq < 0.0)) {
                const t: number = fp / (fp - fq);
                halfPlane.add([p[0]! + t * (q[0]! - p[0]!), p[1]! + t * (q[1]! - p[1]!)]);
            }
        }
        if (halfPlane.size() < 3) {
            return;
        }

        const frontRegion = new Polygon2D();
        for (const p of halfPlane) {
            frontRegion.addVertex(p[0]!, p[1]!);
        }
        const front = new ArrayList<_WeilerAthertonArea>();
        const back = new ArrayList<_WeilerAthertonArea>();
        this.verifiedClip(new _WeilerAthertonArea(frontRegion), area, front, back);
        for (const part of front) {
            remaining.add(new _WeilerAthertonPiece(part, piece.source));
        }
    }

    private display(piece: _WeilerAthertonPiece): void {
        this.output.add(piece);
    }

    //= Verified clips ====================================================

    /**
    Clips a subject polygon to the area of a clip polygon with
    `WeilerAthertonPolygonClipper`, verifying the result (see the class
    documentation): if it is not consistent, the clip is repeated with the
    clip polygon slightly moved. If no attempt succeeds, the clip is solved
    by the half planes of the clip polygon (`clipByConvexPolygon`), or of
    the convex trapezoids of a concave one (`trapezoids`).
    @param clip clip polygon
    @param subject polygon to clip
    @param inside receives the parts of the subject inside the clip polygon
    @param outside receives the parts of the subject outside of it
    */
    private verifiedClip(
        clip: _WeilerAthertonArea,
        subject: _WeilerAthertonArea,
        inside: ArrayList<_WeilerAthertonArea>,
        outside: ArrayList<_WeilerAthertonArea>,
    ): void {
        let attempt: number;

        for (attempt = 0; attempt <= HiddenSurfaceRenderer.MAXIMUM_RETRIES; attempt++) {
            const shift: number = attempt * HiddenSurfaceRenderer.RETRY_SHIFT;
            // Golden angle: the directions of the moves do not repeat
            const angle: number = attempt * 2.399963229728653;
            const clipPolygon: Polygon2D =
                attempt === 0
                    ? clip.polygon
                    : HiddenSurfaceRenderer.translated(clip.polygon, shift * Math.cos(angle), shift * Math.sin(angle));
            const insidePolygon = new Polygon2D();
            const outsidePolygon = new Polygon2D();

            this.clipper.clipPolygons(clipPolygon, subject.polygon, insidePolygon, outsidePolygon);
            if (this.clipper.isLastClipAbandoned()) {
                continue;
            }
            const insideClean: Polygon2D | null = HiddenSurfaceRenderer.withoutDegenerateContours(insidePolygon);
            const outsideClean: Polygon2D | null = HiddenSurfaceRenderer.withoutDegenerateContours(outsidePolygon);
            if (HiddenSurfaceRenderer.isConsistentClip(clip, clipPolygon, subject, insideClean, outsideClean, shift)) {
                HiddenSurfaceRenderer.addWithoutSlivers(inside, insideClean);
                HiddenSurfaceRenderer.addWithoutSlivers(outside, outsideClean);
                return;
            }
        }

        // The clipper could not solve the contacts: a convex clip polygon
        // (the usual case) is solved exactly by half planes instead, and a
        // concave one by the half planes of its trapezoids
        if (HiddenSurfaceRenderer.isConvex(clip.polygon)) {
            HiddenSurfaceRenderer.clipByConvexPolygon(clip.polygon, subject, inside, outside);
            return;
        }
        let remaining = new ArrayList<_WeilerAthertonArea>();
        remaining.add(subject);
        for (const trapezoid of HiddenSurfaceRenderer.trapezoids(clip.polygon)) {
            const next = new ArrayList<_WeilerAthertonArea>();
            const trapezoidArea = new _WeilerAthertonArea(trapezoid);
            for (const area of remaining) {
                if (area.overlapsBox(trapezoidArea)) {
                    HiddenSurfaceRenderer.clipByConvexPolygon(trapezoid, area, inside, next);
                } else {
                    next.add(area);
                }
            }
            remaining = next;
        }
        outside.addAll(remaining);
    }

    /**
    Decomposes a polygon (with holes, filled by the even-odd rule) in convex
    trapezoids that do not overlap: between each pair of consecutive x
    coordinates of its vertices, the edges crossing that vertical slab,
    sorted from bottom to top, bound the polygon by pairs.
    @return the trapezoids, counterclockwise and without repeated vertices
    */
    private static trapezoids(polygon: Polygon2D): ArrayList<Polygon2D> {
        const result = new ArrayList<Polygon2D>();
        const edges = new ArrayList<number[]>();
        const xs = new ArrayList<number>();

        for (const loop of polygon.loops) {
            const n: number = loop.vertices.length;
            for (let i = 0; i < n; i++) {
                const p: Vertex2D = loop.vertices[i]!;
                const q: Vertex2D = loop.vertices[(i + 1) % n]!;
                xs.add(p.x);
                if (p.x !== q.x) {
                    edges.add(p.x < q.x ? [p.x, p.y, q.x, q.y] : [q.x, q.y, p.x, p.y]);
                }
            }
        }
        Collections.sort(xs);
        for (let i = 0; i + 1 < xs.size(); i++) {
            const x0: number = xs.get(i);
            const x1: number = xs.get(i + 1);
            if (x1 <= x0) {
                continue;
            }
            const xm: number = (x0 + x1) / 2.0;
            const crossing = new ArrayList<number[]>();
            for (const e of edges) {
                if (e[0]! <= x0 && e[2]! >= x1) {
                    const slope: number = (e[3]! - e[1]!) / (e[2]! - e[0]!);
                    crossing.add([
                        e[1]! + slope * (x0 - e[0]!),
                        e[1]! + slope * (x1 - e[0]!),
                        e[1]! + slope * (xm - e[0]!),
                    ]);
                }
            }
            Collections.sort(crossing, { compare: (a: number[], b: number[]): number => Double.compare(a[2]!, b[2]!) });
            for (let j = 0; j + 1 < crossing.size(); j += 2) {
                const low: number[] = crossing.get(j);
                const high: number[] = crossing.get(j + 1);
                const corners = new ArrayList<number[]>();
                HiddenSurfaceRenderer.addDistinct(corners, x0, low[0]!);
                HiddenSurfaceRenderer.addDistinct(corners, x1, low[1]!);
                HiddenSurfaceRenderer.addDistinct(corners, x1, high[1]!);
                HiddenSurfaceRenderer.addDistinct(corners, x0, high[0]!);
                if (
                    corners.size() >= 3 &&
                    Math.abs(HiddenSurfaceRenderer.signedArea(corners)) > HiddenSurfaceRenderer.AREA_EPSILON
                ) {
                    const trapezoid = new Polygon2D();
                    trapezoid.loops.length = 0;
                    HiddenSurfaceRenderer.addLoop(trapezoid, corners);
                    result.add(trapezoid);
                }
            }
        }
        return result;
    }

    private static addDistinct(points: ArrayList<number[]>, x: number, y: number): void {
        if (!points.isEmpty()) {
            const last: number[] = points.get(points.size() - 1);
            const first: number[] = points.get(0);
            if (
                Math.hypot(last[0]! - x, last[1]! - y) <= HiddenSurfaceRenderer.MINIMUM_EDGE_LENGTH ||
                Math.hypot(first[0]! - x, first[1]! - y) <= HiddenSurfaceRenderer.MINIMUM_EDGE_LENGTH
            ) {
                return;
            }
        }
        points.add([x, y]);
    }

    /**
    Clips a polygon to a convex one, by the half planes of its edges
    (Sutherland-Hodgman, [FOLE1992].3.14.1, for each contour): the inside part
    is the subject within all the half planes, and the outside part is the
    union of the disjoint parts outside of each edge and inside of the
    previous ones. The parts may have degenerate edges along the clip edges,
    harmless for their areas.
    @param clip a convex counterclockwise polygon (see `isConvex`)
    */
    private static clipByConvexPolygon(
        clip: Polygon2D,
        subject: _WeilerAthertonArea,
        inside: ArrayList<_WeilerAthertonArea>,
        outside: ArrayList<_WeilerAthertonArea>,
    ): void {
        const vertices: Vertex2D[] = clip.loops[0]!.vertices;
        let remaining: Polygon2D | null = subject.polygon;
        const n: number = vertices.length;

        for (let i = 0; i < n && remaining !== null; i++) {
            const p: Vertex2D = vertices[i]!;
            const q: Vertex2D = vertices[(i + 1) % n]!;
            // An edge of (almost) no length gives no direction: its half
            // plane would cut at random
            if (Math.hypot(q.x - p.x, q.y - p.y) <= HiddenSurfaceRenderer.MINIMUM_EDGE_LENGTH) {
                continue;
            }
            // Inside is at the left of the edge: a*x + b*y + c >= 0
            const a: number = -(q.y - p.y);
            const b: number = q.x - p.x;
            const c: number = -(a * p.x + b * p.y);
            const out: Polygon2D | null = HiddenSurfaceRenderer.clipByHalfPlane(remaining, -a, -b, -c);
            if (out !== null) {
                HiddenSurfaceRenderer.addWithoutSlivers(outside, out);
            }
            remaining = HiddenSurfaceRenderer.clipByHalfPlane(remaining, a, b, c);
        }
        if (remaining !== null) {
            HiddenSurfaceRenderer.addWithoutSlivers(inside, remaining);
        }
    }

    /**
    @return the part of a polygon where a*x + b*y + c >= 0 (each contour
    clipped by Sutherland-Hodgman), or null if nothing is left
    */
    private static clipByHalfPlane(polygon: Polygon2D, a: number, b: number, c: number): Polygon2D | null {
        const result = new Polygon2D();
        result.loops.length = 0;
        for (const loop of polygon.loops) {
            const clipped = new ArrayList<number[]>();
            const n: number = loop.vertices.length;
            for (let i = 0; i < n; i++) {
                const p: Vertex2D = loop.vertices[i]!;
                const q: Vertex2D = loop.vertices[(i + 1) % n]!;
                const fp: number = a * p.x + b * p.y + c;
                const fq: number = a * q.x + b * q.y + c;
                if (fp >= 0.0) {
                    clipped.add([p.x, p.y]);
                }
                if (fp >= 0.0 !== fq >= 0.0) {
                    const t: number = fp / (fp - fq);
                    clipped.add([p.x + t * (q.x - p.x), p.y + t * (q.y - p.y)]);
                }
            }
            if (clipped.size() >= 3) {
                HiddenSurfaceRenderer.addLoop(result, clipped);
            }
        }
        return HiddenSurfaceRenderer.withoutDegenerateContours(result);
    }

    /**
    @return true if the polygon has one contour, counterclockwise and convex
    */
    private static isConvex(polygon: Polygon2D): boolean {
        if (polygon.loops.length !== 1) {
            return false;
        }
        const vertices: Vertex2D[] = polygon.loops[0]!.vertices;
        const n: number = vertices.length;
        if (n < 3 || HiddenSurfaceRenderer.signedArea(polygon.loops[0]!) <= 0.0) {
            return false;
        }
        for (let i = 0; i < n; i++) {
            const p: Vertex2D = vertices[i]!;
            const q: Vertex2D = vertices[(i + 1) % n]!;
            const r: Vertex2D = vertices[(i + 2) % n]!;
            const cross: number = (q.x - p.x) * (r.y - q.y) - (q.y - p.y) * (r.x - q.x);
            const scale: number = Math.hypot(q.x - p.x, q.y - p.y) * Math.hypot(r.x - q.x, r.y - q.y);
            if (cross < -1.0e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    /**
    @return true if the inside and outside parts of a clip add up to the
    subject and are within the expected bounds
    */
    private static isConsistentClip(
        clip: _WeilerAthertonArea,
        clipPolygon: Polygon2D,
        subject: _WeilerAthertonArea,
        inside: Polygon2D | null,
        outside: Polygon2D | null,
        shift: number,
    ): boolean {
        const insideArea: number = inside !== null ? HiddenSurfaceRenderer.enclosedArea(inside) : 0.0;
        const outsideArea: number = outside !== null ? HiddenSurfaceRenderer.enclosedArea(outside) : 0.0;
        const length: number = shift + HiddenSurfaceRenderer.SLIVER_WIDTH;
        const tolerance: number =
            1.0e-7 * subject.area +
            HiddenSurfaceRenderer.AREA_EPSILON +
            4.0 * length * (subject.perimeter + 2.0 * (subject.maxX - subject.minX + (subject.maxY - subject.minY)));

        if (insideArea < -tolerance || outsideArea < -tolerance) {
            return false;
        }
        if (Math.abs(insideArea + outsideArea - subject.area) > tolerance) {
            return false;
        }
        if (insideArea > Math.min(subject.area, clip.area) + tolerance) {
            return false;
        }
        if (inside !== null) {
            const box: number[] = HiddenSurfaceRenderer.boundingBox(inside);
            const slack: number = 2.0 * length;
            if (
                box[0]! < Math.max(subject.minX, clip.minX) - slack ||
                box[1]! < Math.max(subject.minY, clip.minY) - slack ||
                box[2]! > Math.min(subject.maxX, clip.maxX) + slack ||
                box[3]! > Math.min(subject.maxY, clip.maxY) + slack
            ) {
                return false;
            }
        }
        if (outside !== null) {
            const box: number[] = HiddenSurfaceRenderer.boundingBox(outside);
            const slack: number = 2.0 * length;
            if (
                box[0]! < subject.minX - slack ||
                box[1]! < subject.minY - slack ||
                box[2]! > subject.maxX + slack ||
                box[3]! > subject.maxY + slack
            ) {
                return false;
            }
        }
        // At sample points of the four polygons, far from all their
        // borders, a point is in the inside part if and only if it is in the
        // subject and in the clip polygon, and it is in the outside part if
        // and only if it is in the subject and not in the clip polygon. It
        // fails when the clipper misses contacts of the contours: i.e. a
        // piece inscribed in the clip polygon (all its vertices on its
        // border), or a contour of the subject identical to the clip polygon
        const slack: number = 2.0 * length;
        const samples: ArrayList<number[]> = HiddenSurfaceRenderer.samplePoints(clipPolygon);
        samples.addAll(HiddenSurfaceRenderer.samplePoints(subject.polygon));
        if (inside !== null) {
            samples.addAll(HiddenSurfaceRenderer.samplePoints(inside));
        }
        if (outside !== null) {
            samples.addAll(HiddenSurfaceRenderer.samplePoints(outside));
        }
        const clipBorder = new _WeilerAthertonEdgeGrid(clipPolygon, slack);
        const subjectBorder = new _WeilerAthertonEdgeGrid(subject.polygon, slack);
        const insideBorder: _WeilerAthertonEdgeGrid | null =
            inside !== null ? new _WeilerAthertonEdgeGrid(inside, slack) : null;
        const outsideBorder: _WeilerAthertonEdgeGrid | null =
            outside !== null ? new _WeilerAthertonEdgeGrid(outside, slack) : null;
        for (const point of samples) {
            const x: number = point[0]!;
            const y: number = point[1]!;
            if (clipBorder.isNearBorder(x, y) || subjectBorder.isNearBorder(x, y)) {
                continue;
            }
            const inSubject: boolean = HiddenSurfaceRenderer.contains(subject.polygon, x, y);
            const inClip: boolean = HiddenSurfaceRenderer.contains(clipPolygon, x, y);
            if (
                !HiddenSurfaceRenderer.isOnExpectedSide(inside, insideBorder, x, y, inSubject && inClip) ||
                !HiddenSurfaceRenderer.isOnExpectedSide(outside, outsideBorder, x, y, inSubject && !inClip)
            ) {
                return false;
            }
        }
        return true;
    }

    /**
    @param part a part of the clip result (null if empty)
    @param border index of the edges of the part
    @param expected true if the point must be in the part
    @return true if the point is in the part as expected, or it is too close
    to its border to tell
    */
    private static isOnExpectedSide(
        part: Polygon2D | null,
        border: _WeilerAthertonEdgeGrid | null,
        x: number,
        y: number,
        expected: boolean,
    ): boolean {
        if (part === null) {
            return !expected;
        }
        return HiddenSurfaceRenderer.contains(part, x, y) === expected || border!.isNearBorder(x, y);
    }

    /**
    @return sample points of a polygon, as {x, y} pairs: its vertices, the
    middle points of its edges, and the centroids of the triangles at its
    vertices (inside the polygon at its ears)
    */
    private static samplePoints(polygon: Polygon2D): ArrayList<number[]> {
        const points = new ArrayList<number[]>();
        for (const loop of polygon.loops) {
            const n: number = loop.vertices.length;
            for (let i = 0; i < n; i++) {
                const p: Vertex2D = loop.vertices[(i + n - 1) % n]!;
                const q: Vertex2D = loop.vertices[i]!;
                const r: Vertex2D = loop.vertices[(i + 1) % n]!;
                points.add([q.x, q.y]);
                points.add([(q.x + r.x) / 2.0, (q.y + r.y) / 2.0]);
                points.add([(p.x + q.x + r.x) / 3.0, (p.y + q.y + r.y) / 3.0]);
            }
        }
        return points;
    }

    /**
    Adds the parts of a clip result to a list, discarding the contours that
    are degenerate strips (see `SLIVER_WIDTH`).
    */
    private static addWithoutSlivers(list: ArrayList<_WeilerAthertonArea>, polygon: Polygon2D | null): void {
        if (polygon === null) {
            return;
        }
        const clean = new Polygon2D();
        let empty = true;
        clean.loops.length = 0;
        for (const loop of polygon.loops) {
            const area: number = Math.abs(HiddenSurfaceRenderer.signedArea(loop));
            const halfPerimeter: number = HiddenSurfaceRenderer.perimeter(loop) / 2.0;
            if (halfPerimeter <= 0.0 || area / halfPerimeter < HiddenSurfaceRenderer.SLIVER_WIDTH) {
                continue;
            }
            HiddenSurfaceRenderer.copyLoop(clean, loop);
            empty = false;
        }
        if (!empty) {
            const area = new _WeilerAthertonArea(clean);
            if (area.area > HiddenSurfaceRenderer.AREA_EPSILON) {
                list.add(area);
            }
        }
    }

    //= Input polygons ====================================================

    /**
    Transforms the faces of the solids to the viewing space, removing the
    faces facing away from the observer and clipping them to the view.
    */
    private buildInputPolygons(
        solids: ArrayList<SimpleBody> | readonly SimpleBody[],
        camera: Camera,
    ): ArrayList<_WeilerAthertonSourcePolygon> {
        const sources = new ArrayList<_WeilerAthertonSourcePolygon>();
        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        const depthSign: number = HiddenSurfaceRenderer.depthSign(camera, projection);
        const windowPolygon = new Polygon2D();
        let i: number;

        windowPolygon.addVertex(-HiddenSurfaceRenderer.SCALE, -HiddenSurfaceRenderer.SCALE);
        windowPolygon.addVertex(HiddenSurfaceRenderer.SCALE, -HiddenSurfaceRenderer.SCALE);
        windowPolygon.addVertex(HiddenSurfaceRenderer.SCALE, HiddenSurfaceRenderer.SCALE);
        windowPolygon.addVertex(-HiddenSurfaceRenderer.SCALE, HiddenSurfaceRenderer.SCALE);
        const window = new _WeilerAthertonArea(windowPolygon);

        for (const body of solids) {
            const geometry: Geometry | null = body.getGeometry();
            if (!(geometry instanceof PolyhedralBoundedSolid)) {
                continue;
            }
            const brep: PolyhedralBoundedSolid = geometry;
            const transform: Matrix4x4d = body.getTransformationMatrix();
            for (i = 0; i < brep.getPolygonsList().size(); i++) {
                const face: _PolyhedralBoundedSolidFace = brep.getPolygonsList().get(i)!;
                const source: _WeilerAthertonSourcePolygon | null = this.buildInputPolygon(
                    face,
                    body,
                    transform,
                    camera,
                    projection,
                    depthSign,
                    window,
                );
                if (source !== null) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    private buildInputPolygon(
        face: _PolyhedralBoundedSolidFace,
        body: SimpleBody,
        transform: Matrix4x4d,
        camera: Camera,
        projection: Matrix4x4d,
        depthSign: number,
        window: _WeilerAthertonArea,
    ): _WeilerAthertonSourcePolygon | null {
        const worldPlane: InfinitePlane | null = HiddenSurfaceRenderer.worldContainingPlane(face, body, transform);
        if (worldPlane === null || !HiddenSurfaceRenderer.isFrontFace(worldPlane, camera)) {
            return null;
        }

        // Projection of each loop, clipped in homogeneous coordinates against
        // the near and far planes
        const loops = new ArrayList<ArrayList<number[]>>();
        let complete = true;
        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i) ?? null;
            let clipLoop = new ArrayList<Vector4Dd>();
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
            const heStart: _PolyhedralBoundedSolidHalfEdge | null = he;
            do {
                if (he === null || he.startingVertex === null) {
                    break;
                }
                clipLoop.add(projection.multiply(new Vector4Dd(transform.multiply(he.startingVertex.position))));
                he = he.next();
            } while (he !== heStart);

            for (const p of clipLoop) {
                if (p.z() + p.w() < 0.0 || p.w() - p.z() < 0.0) {
                    complete = false;
                }
            }
            clipLoop = HiddenSurfaceRenderer.clipAgainstPlane(clipLoop, 1.0);
            clipLoop = HiddenSurfaceRenderer.clipAgainstPlane(clipLoop, -1.0);
            if (clipLoop.size() < 3) {
                complete = false;
                continue;
            }
            const projectedLoop = new ArrayList<number[]>();
            for (const p of clipLoop) {
                const ndc: Vector4Dd = p.dividedByW();
                projectedLoop.add([
                    HiddenSurfaceRenderer.SCALE * ndc.x(),
                    HiddenSurfaceRenderer.SCALE * ndc.y(),
                    depthSign * ndc.z(),
                ]);
            }
            if (Math.abs(HiddenSurfaceRenderer.signedArea(projectedLoop)) > HiddenSurfaceRenderer.AREA_EPSILON) {
                loops.add(projectedLoop);
            }
        }
        if (loops.isEmpty()) {
            return null;
        }

        // The polygon clipper follows main contours counterclockwise and
        // holes clockwise: the main contour is the one of largest area
        let outer = 0;
        for (let i = 1; i < loops.size(); i++) {
            if (
                Math.abs(HiddenSurfaceRenderer.signedArea(loops.get(i))) >
                Math.abs(HiddenSurfaceRenderer.signedArea(loops.get(outer)))
            ) {
                outer = i;
            }
        }
        for (let i = 0; i < loops.size(); i++) {
            const area: number = HiddenSurfaceRenderer.signedArea(loops.get(i));
            if ((i === outer && area < 0.0) || (i !== outer && area > 0.0)) {
                Collections.reverse(loops.get(i));
            }
        }

        const source = new _WeilerAthertonSourcePolygon();
        if (!HiddenSurfaceRenderer.fitDepthPlane(loops.get(outer), source)) {
            // Seen edge-on
            return null;
        }

        const projected = new Polygon2D();
        projected.loops.length = 0;
        HiddenSurfaceRenderer.addLoop(projected, loops.get(outer));
        for (let i = 0; i < loops.size(); i++) {
            if (i !== outer) {
                HiddenSurfaceRenderer.addLoop(projected, loops.get(i));
            }
        }

        // Clip to the window of the view
        let template = new _WeilerAthertonArea(projected);
        if (!template.overlapsBox(window)) {
            return null;
        }
        if (
            template.minX < window.minX ||
            template.maxX > window.maxX ||
            template.minY < window.minY ||
            template.maxY > window.maxY
        ) {
            const inside = new ArrayList<_WeilerAthertonArea>();
            const outside = new ArrayList<_WeilerAthertonArea>();
            this.verifiedClip(window, template, inside, outside);
            if (inside.isEmpty()) {
                return null;
            }
            template = HiddenSurfaceRenderer.merged(inside);
            complete = false;
        }

        source.template = template;
        source.nearestDepth = Double.MAX_VALUE;
        for (const loop of template.polygon.loops) {
            for (const v of loop.vertices) {
                source.nearestDepth = Math.min(source.nearestDepth, source.depthAt(v.x, v.y));
            }
        }
        source.body = body;
        source.face = face;
        source.worldPlane = worldPlane;
        source.complete = complete;
        return source;
    }

    /**
    Sutherland-Hodgman clip of a loop in homogeneous coordinates against the
    near (side = 1: z >= -w) or far (side = -1: z <= w) plane.
    */
    private static clipAgainstPlane(loop: ArrayList<Vector4Dd>, side: number): ArrayList<Vector4Dd> {
        const result = new ArrayList<Vector4Dd>();
        const n: number = loop.size();
        let i: number;

        for (i = 0; i < n; i++) {
            const p: Vector4Dd = loop.get(i);
            const q: Vector4Dd = loop.get((i + 1) % n);
            const dp: number = side * p.z() + p.w();
            const dq: number = side * q.z() + q.w();
            if (dp >= 0.0) {
                result.add(p);
            }
            if (dp >= 0.0 !== dq >= 0.0) {
                const t: number = dp / (dp - dq);
                result.add(p.multiply(1.0 - t).add(q.multiply(t)));
            }
        }
        return result;
    }

    /**
    Fits the depth plane of a projected loop with Newell's method (robust for
    concave loops), as depth = a*x + b*y + c.
    @return false if the plane is parallel to the line of sight
    */
    private static fitDepthPlane(loop: ArrayList<number[]>, source: _WeilerAthertonSourcePolygon): boolean {
        let nx = 0.0;
        let ny = 0.0;
        let nz = 0.0;
        let cx = 0.0;
        let cy = 0.0;
        let cz = 0.0;
        const n: number = loop.size();
        let i: number;

        for (i = 0; i < n; i++) {
            const p: number[] = loop.get(i);
            const q: number[] = loop.get((i + 1) % n);
            nx += (p[1]! - q[1]!) * (p[2]! + q[2]!);
            ny += (p[2]! - q[2]!) * (p[0]! + q[0]!);
            nz += (p[0]! - q[0]!) * (p[1]! + q[1]!);
            cx += p[0]!;
            cy += p[1]!;
            cz += p[2]!;
        }
        const length: number = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length <= VSDK.EPSILON * VSDK.EPSILON || Math.abs(nz) <= 1.0e-12 * length) {
            return false;
        }
        cx /= n;
        cy /= n;
        cz /= n;
        source.depthSlopeX = -nx / nz;
        source.depthSlopeY = -ny / nz;
        source.depthOffset = cz - source.depthSlopeX * cx - source.depthSlopeY * cy;
        return true;
    }

    /**
    @return 1 if the depth of the normalized device coordinates grows away
    from the observer, -1 otherwise, so the depths used by the algorithm
    are always smaller closer to the observer
    */
    private static depthSign(camera: Camera, projection: Matrix4x4d): number {
        const front: Vector3Dd = camera.getFront().normalized();
        const near: number = camera.getNearPlaneDistance();
        const far: number = camera.getFarPlaneDistance();
        const p0: Vector3Dd = camera.getPosition().add(front.multiply(near + 0.25 * (far - near)));
        const p1: Vector3Dd = camera.getPosition().add(front.multiply(near + 0.75 * (far - near)));
        const z0: number = projection.multiply(new Vector4Dd(p0)).dividedByW().z();
        const z1: number = projection.multiply(new Vector4Dd(p1)).dividedByW().z();
        return z1 >= z0 ? 1.0 : -1.0;
    }

    /**
    Backplane removal: a face of a closed solid is seen only from the side
    its outward normal points to.
    */
    private static isFrontFace(worldPlane: InfinitePlane, camera: Camera): boolean {
        const n: Vector3Dd = worldPlane.getNormal();
        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            return n.dotProduct(camera.getFront()) < 0.0;
        }
        // Signed distance from the plane to the eye
        const distance: number =
            n.x() * camera.getPosition().x() +
            n.y() * camera.getPosition().y() +
            n.z() * camera.getPosition().z() +
            worldPlane.getD();
        return distance > 0.0;
    }

    /**
    @return the plane of a face in world coordinates, with its outward
    normal (from the maintained plane of the face, a Newell fit, not from
    three vertices that could be at a reflex corner), or null if undefined
    */
    private static worldContainingPlane(
        face: _PolyhedralBoundedSolidFace,
        body: SimpleBody,
        transform: Matrix4x4d,
    ): InfinitePlane | null {
        const localPlane: InfinitePlane | null = face.getContainingPlane();
        let pointOnFace: Vector3Dd | null = null;

        if (localPlane === null) {
            return null;
        }
        for (let i = 0; i < face.boundariesList.size() && pointOnFace === null; i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i) ?? null;
            const he: _PolyhedralBoundedSolidHalfEdge | null = loop !== null ? loop.boundaryStartHalfEdge : null;
            if (he !== null && he.startingVertex !== null) {
                pointOnFace = he.startingVertex.position;
            }
        }
        if (pointOnFace === null) {
            return null;
        }
        const worldNormal: Vector3Dd = body.transformNormalToWorld(localPlane.getNormal());
        if (worldNormal.length() <= VSDK.EPSILON) {
            return null;
        }
        return new InfinitePlane(worldNormal.normalized(), transform.multiply(pointOnFace));
    }

    //= 2D polygon utilities ==============================================

    private static addLoop(polygon: Polygon2D, loop: ArrayList<number[]>): void {
        polygon.nextLoop();
        for (const p of loop) {
            polygon.addVertex(p[0]!, p[1]!);
        }
    }

    private static copyLoop(polygon: Polygon2D, loop: _Polygon2DContour): void {
        polygon.nextLoop();
        for (const v of loop.vertices) {
            polygon.addVertex(v.x, v.y);
        }
    }

    private static translated(polygon: Polygon2D, dx: number, dy: number): Polygon2D {
        const result = new Polygon2D();
        result.loops.length = 0;
        for (const loop of polygon.loops) {
            result.nextLoop();
            for (const v of loop.vertices) {
                result.addVertex(v.x + dx, v.y + dy);
            }
        }
        return result;
    }

    /**
    @return one area with all the contours of several areas (that do not
    overlap)
    */
    private static merged(areas: ArrayList<_WeilerAthertonArea>): _WeilerAthertonArea {
        if (areas.size() === 1) {
            return areas.get(0);
        }
        const result = new Polygon2D();
        result.loops.length = 0;
        for (const area of areas) {
            for (const loop of area.polygon.loops) {
                HiddenSurfaceRenderer.copyLoop(result, loop);
            }
        }
        return new _WeilerAthertonArea(result);
    }

    /**
    Java's two `signedArea` overloads: the private one over a list of
    {x, y, ...} points, and the package-private one over a contour.
    */
    public static signedArea(loop: ArrayList<number[]> | _Polygon2DContour): number {
        let area = 0.0;
        if (loop instanceof _Polygon2DContour) {
            const n: number = loop.vertices.length;
            for (let i = 0; i < n; i++) {
                const p: Vertex2D = loop.vertices[i]!;
                const q: Vertex2D = loop.vertices[(i + 1) % n]!;
                area += p.x * q.y - q.x * p.y;
            }
            return area / 2.0;
        }
        const n: number = loop.size();
        for (let i = 0; i < n; i++) {
            const p: number[] = loop.get(i);
            const q: number[] = loop.get((i + 1) % n);
            area += p[0]! * q[1]! - q[0]! * p[1]!;
        }
        return area / 2.0;
    }

    /**
    Java's two `perimeter` overloads: over a contour, and over all the
    contours of a polygon.
    */
    public static perimeter(loopOrPolygon: _Polygon2DContour | Polygon2D): number {
        let length = 0.0;
        if (loopOrPolygon instanceof Polygon2D) {
            for (const loop of loopOrPolygon.loops) {
                length += HiddenSurfaceRenderer.perimeter(loop);
            }
            return length;
        }
        const loop: _Polygon2DContour = loopOrPolygon;
        const n: number = loop.vertices.length;
        for (let i = 0; i < n; i++) {
            const p: Vertex2D = loop.vertices[i]!;
            const q: Vertex2D = loop.vertices[(i + 1) % n]!;
            length += Math.hypot(q.x - p.x, q.y - p.y);
        }
        return length;
    }

    /**
    @return the area enclosed by the contours of a polygon: its main contours
    are counterclockwise (positive area) and its holes clockwise (negative
    area), as the clipper keeps them
    */
    public static enclosedArea(polygon: Polygon2D): number {
        let area = 0.0;
        for (const loop of polygon.loops) {
            area += HiddenSurfaceRenderer.signedArea(loop);
        }
        return area;
    }

    /**
    @return a copy of a clip result without its degenerate contours (with
    less than three vertices or with no area), or null if no contour is left
    */
    public static withoutDegenerateContours(polygon: Polygon2D): Polygon2D | null {
        const clean = new Polygon2D();
        let empty = true;

        clean.loops.length = 0;
        for (const loop of polygon.loops) {
            if (
                loop.vertices.length < 3 ||
                Math.abs(HiddenSurfaceRenderer.signedArea(loop)) <= HiddenSurfaceRenderer.AREA_EPSILON
            ) {
                continue;
            }
            HiddenSurfaceRenderer.copyLoop(clean, loop);
            empty = false;
        }
        return empty ? null : clean;
    }

    /**
    @return true if the point is inside the polygon, for the even-odd rule
    */
    public static contains(polygon: Polygon2D, x: number, y: number): boolean {
        let inside = false;
        for (const loop of polygon.loops) {
            if (loop.vertices.length >= 3 && HiddenSurfaceRenderer.containsInLoop(loop, x, y)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static containsInLoop(loop: _Polygon2DContour, x: number, y: number): boolean {
        let inside = false;
        const n: number = loop.vertices.length;
        for (let i = 0, j = n - 1; i < n; j = i++) {
            const a: Vertex2D = loop.vertices[i]!;
            const b: Vertex2D = loop.vertices[j]!;
            if (a.y > y !== b.y > y && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
    @return {minX, minY, maxX, maxY} of the vertices of a polygon
    */
    public static boundingBox(polygon: Polygon2D): number[] {
        const box: number[] = [Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE];
        for (const loop of polygon.loops) {
            for (const v of loop.vertices) {
                box[0] = Math.min(box[0]!, v.x);
                box[1] = Math.min(box[1]!, v.y);
                box[2] = Math.max(box[2]!, v.x);
                box[3] = Math.max(box[3]!, v.y);
            }
        }
        return box;
    }
}

export namespace HiddenSurfaceRenderer {
    /**
    A visible area of a face of a solid, output of the algorithm.
    */
    export class VisiblePolygon extends RenderingElement {
        /// Visible area, on the x-y plane of the normalized device coordinates
        /// of the camera (from <-1, -1> at the lower left corner of the view
        /// to <1, 1> at its upper right corner). Main contours are
        /// counterclockwise and holes clockwise.
        public readonly area: Polygon2D;
        /// Body the face belongs to
        public readonly body: SimpleBody;
        /// Face whose visible area is given
        public readonly face: _PolyhedralBoundedSolidFace;
        /// Plane of the face in world coordinates, with an outward normal
        public readonly worldPlane: InfinitePlane;
        /// Lights reaching the area (ambient lights included), or null if
        /// shadows were not computed
        public readonly lights: ArrayList<Light> | null;

        /** Package-private in Java. */
        public constructor(area: Polygon2D, source: _WeilerAthertonSourcePolygon, lights: ArrayList<Light> | null) {
            super();
            this.area = area;
            this.body = source.body!;
            this.face = source.face!;
            this.worldPlane = source.worldPlane!;
            this.lights = lights;
        }
    }
}
