import { Double } from "../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { RayHit } from "../../environment/geometry/element/RayHit.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Tube } from "../../environment/geometry/volume/Tube.js";
import type { Volume } from "../../environment/geometry/volume/Volume.js";
import type { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { Viewport } from "../viewport/Viewport.js";
import type { ViewportElementScaler } from "../viewport/ViewportElementScaler.js";
import { Gizmo } from "./Gizmo.js";

/**
A camera control gizmo, modeled after the ViewCube of Autodesk products: a
small cube, presented over a corner of a viewport (as a HUD), that always
shows the orientation of the camera of the viewport, and lets the user turn
that camera to a predefined view with a click, or orbit it by dragging.

Model
-----
The cube [-1, 1]^3 is divided, along each axis, in three bands: a corner band
at each side (of width `CORNER_FRACTION`) and a central band. Each of the 26
combinations (i, j, k) in {-1, 0, 1}^3, except (0, 0, 0), is one region of
the cube, modeled as a `Box`:
  - 6 face regions (one non zero component): the center of each face of the
    cube; clicking one gives an orthogonal view of that side.
  - 12 edge regions (two non zero components): long boxes along the edges;
    clicking one gives a three-fourths view between two sides.
  - 8 corner regions (three non zero components): small cubes at the
    vertices; clicking one gives a three-fourths view between three sides.
Regions are separated by small gaps (`GAP`), that mark them (and avoid
coincident faces between neighbors). Under the cube there is a compass, a
`Tube` around the vertical axis; dragging it turns the camera around Z.

The direction of a region is (i, j, k): the camera of its view looks from
that direction towards the center. Following the cameras of `Viewport` (Z
up), the faces are TOP (+Z), BOTTOM (-Z), FRONT (-Y), BACK (+Y), LEFT (-X)
and RIGHT (+X).

Presentation
------------
The gizmo is seen through its own orthogonal camera (`getHudCamera`), that
has the same orientation of the camera of the view (`setCamera`), and
occupies a square area of `getSizeInPixels()` pixels at the upper right
corner of the viewport (see `placeInViewport`). Pixel coordinates (`getHudX`,
`getHudY`, `pickRegion`...) have the origin at the upper left corner of the
viewport and y growing downwards. The parts (`getParts`) are bodies with materials that
reflect the state of the gizmo (`updatePartMaterials`): the region under the
cursor (`getVolatileSelection`) is highlighted, and the gizmo is translucent
when the cursor is not over it (`getInactiveOpacity`). Renderers of each
technology draw the parts with the HUD camera.

Interaction (see `CameraControlGizmoInteractionTechnique`) and the smooth
turn of the camera (see `CameraControlGizmoAnimation`) are platform
independent too.
*/
export class CameraControlGizmo extends Gizmo {
    /// Not a region of the gizmo
    public static readonly NO_REGION: number = -1;
    /// Number of regions of the cube
    public static readonly NUMBER_OF_CUBE_REGIONS: number = 26;
    /// The compass ring under the cube
    public static readonly COMPASS_REGION: number = CameraControlGizmo.NUMBER_OF_CUBE_REGIONS;
    /// Number of parts (bodies) of the gizmo: the regions and the compass
    public static readonly NUMBER_OF_PARTS: number = CameraControlGizmo.NUMBER_OF_CUBE_REGIONS + 1;

    /// Kinds of regions
    public static readonly KIND_NONE: number = 0;
    public static readonly KIND_FACE: number = 1;
    public static readonly KIND_EDGE: number = 2;
    public static readonly KIND_CORNER: number = 3;
    public static readonly KIND_COMPASS: number = 4;

    /// Width of the corner bands, as a fraction of the side of the cube
    public static readonly CORNER_FRACTION: number = 0.15;
    /// Separation between neighbor regions, in units of the model
    public static readonly GAP: number = 0.04;
    /// Radii and height of the compass, and the height of its base
    public static readonly COMPASS_INTERNAL_RADIUS: number = 1.5;
    public static readonly COMPASS_EXTERNAL_RADIUS: number = 1.8;
    public static readonly COMPASS_HEIGHT: number = 0.08;
    public static readonly COMPASS_BASE_Z: number = -1.0;

    /// Half of the side of the square of the model seen by the HUD camera
    public static readonly HUD_HALF_EXTENT: number = 2.05;
    /// Distance from the center of the model to the HUD camera
    public static readonly HUD_CAMERA_DISTANCE: number = 10.0;

    public static readonly DEFAULT_BASE_SIZE_IN_PIXELS: number = 110;
    public static readonly DEFAULT_BASE_MARGIN_IN_PIXELS: number = 6;
    public static readonly DEFAULT_INACTIVE_OPACITY: number = 0.55;

    private static readonly FACE_LABELS: readonly string[] = ["LEFT", "RIGHT", "FRONT", "BACK", "BOTTOM", "TOP"];

    /// Sides of the polygons approximating the circles of the compass
    public static readonly COMPASS_SLICES: number = 24;

    //= Model =============================================================
    /// Direction (i, j, k) of each region of the cube
    private readonly regionDirections: number[][];
    /// Bodies of the regions, then the compass
    private readonly parts: ArrayList<SimpleBody>;
    /// Edges of the boundary representations of the parts, by part
    private edgeSegments: Float32Array[] | null;

    //= Presentation ======================================================
    private camera: Camera | null;
    private readonly hudCamera: Camera;
    private baseSizeInPixels: number;
    private baseMarginInPixels: number;
    private sizeInPixels: number;
    private marginInPixels: number;
    /// Upper left corner of the area of the gizmo, in pixels of the viewport
    private hudX: number;
    private hudY: number;
    private visible: boolean;
    private inactiveOpacity: number;
    private active: boolean;
    private volatileSelection: number;

    private faceColor: ColorRgb;
    private edgeColor: ColorRgb;
    private cornerColor: ColorRgb;
    private compassColor: ColorRgb;
    private highlightColor: ColorRgb;
    private lineColor: ColorRgb;
    private labelColor: ColorRgb;

    public constructor() {
        super();
        this.regionDirections = new Array<number[]>(CameraControlGizmo.NUMBER_OF_CUBE_REGIONS);
        this.parts = new ArrayList<SimpleBody>();
        this.edgeSegments = null;

        this.faceColor = new ColorRgb(0.86, 0.88, 0.92);
        this.edgeColor = new ColorRgb(0.74, 0.77, 0.82);
        this.cornerColor = new ColorRgb(0.66, 0.69, 0.75);
        this.compassColor = new ColorRgb(0.55, 0.57, 0.62);
        this.highlightColor = new ColorRgb(0.35, 0.62, 1.0);
        this.lineColor = new ColorRgb(0.15, 0.15, 0.2);
        this.labelColor = new ColorRgb(0.1, 0.1, 0.15);

        this.buildModel();

        this.camera = null;
        this.hudCamera = new Camera();
        this.hudCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        this.hudCamera.setOrthogonalZoom(1.0 / CameraControlGizmo.HUD_HALF_EXTENT);
        this.hudCamera.setNearPlaneDistance(0.1);
        this.hudCamera.setFarPlaneDistance(2 * CameraControlGizmo.HUD_CAMERA_DISTANCE);
        this.hudCamera.setName("Camera control gizmo");

        this.baseSizeInPixels = CameraControlGizmo.DEFAULT_BASE_SIZE_IN_PIXELS;
        this.baseMarginInPixels = CameraControlGizmo.DEFAULT_BASE_MARGIN_IN_PIXELS;
        this.sizeInPixels = this.baseSizeInPixels;
        this.marginInPixels = this.baseMarginInPixels;
        this.hudX = this.marginInPixels;
        this.hudY = this.marginInPixels;
        this.visible = true;
        this.inactiveOpacity = CameraControlGizmo.DEFAULT_INACTIVE_OPACITY;
        this.active = false;
        this.volatileSelection = CameraControlGizmo.NO_REGION;
        this.updateHudCamera();
        this.updatePartMaterials();
    }

    //= Model construction ================================================

    /**
    @return the interval [min, max] of the band `v` (-1, 0 or 1) of an axis,
    without the gaps towards its neighbor bands
    */
    private static band(v: number): number[] {
        const c = 2 * CameraControlGizmo.CORNER_FRACTION;
        const g = CameraControlGizmo.GAP / 2;

        switch (v) {
            case -1:
                return [-1, -1 + c - g];
            case 1:
                return [1 - c + g, 1];
            default:
                return [-1 + c + g, 1 - c - g];
        }
    }

    private buildModel(): void {
        let region = 0;

        for (let i = -1; i <= 1; i++) {
            for (let j = -1; j <= 1; j++) {
                for (let k = -1; k <= 1; k++) {
                    if (i === 0 && j === 0 && k === 0) {
                        continue;
                    }
                    this.regionDirections[region] = [i, j, k];
                    const bx = CameraControlGizmo.band(i);
                    const by = CameraControlGizmo.band(j);
                    const bz = CameraControlGizmo.band(k);
                    // Centered boxes, placed by the position of the part
                    // (renderers tessellate boxes centered at the origin)
                    const half = new Vector3Dd(
                        (bx[1]! - bx[0]!) / 2,
                        (by[1]! - by[0]!) / 2,
                        (bz[1]! - bz[0]!) / 2,
                    );
                    const box = new Box(half.multiply(-1), half);
                    this.parts.add(
                        CameraControlGizmo.createPart(
                            box,
                            new Vector3Dd((bx[0]! + bx[1]!) / 2, (by[0]! + by[1]!) / 2, (bz[0]! + bz[1]!) / 2),
                            this.regionName(region),
                        ),
                    );
                    region++;
                }
            }
        }

        const compass = new Tube(
            CameraControlGizmo.COMPASS_INTERNAL_RADIUS,
            CameraControlGizmo.COMPASS_EXTERNAL_RADIUS,
            CameraControlGizmo.COMPASS_HEIGHT,
        );
        this.parts.add(
            CameraControlGizmo.createPart(compass, new Vector3Dd(0, 0, CameraControlGizmo.COMPASS_BASE_Z), "Compass"),
        );
    }

    private static createPart(geometry: Geometry, position: Vector3Dd, name: string): SimpleBody {
        const body = new SimpleBody();

        body.setGeometry(geometry);
        body.setPosition(position);
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setScale(new Vector3Dd(1, 1, 1));
        body.setMaterial(new SimpleMaterial());
        body.setName(name);
        return body;
    }

    //= Model queries =====================================================

    /**
    @return the bodies of the gizmo: index `r` is the body of region `r`, and
    the last one (`COMPASS_REGION`) is the compass
    */
    public getParts(): ArrayList<SimpleBody> {
        return Collections.unmodifiableList(this.parts);
    }

    /**
    @param part index of a part (a region or `COMPASS_REGION`)
    @return the boundary representation of the geometry of the part, in the
    space of the part (its body places it with its position)
    */
    public exportPartToPolyhedralBoundedSolid(part: number): PolyhedralBoundedSolid {
        const body = this.parts.get(part);
        let brep: PolyhedralBoundedSolid;

        const geometry = body.getGeometry();
        if (geometry instanceof Tube) {
            brep = geometry.exportToPolyhedralBoundedSolid(CameraControlGizmo.COMPASS_SLICES);
        } else {
            brep = (geometry as Volume).exportToPolyhedralBoundedSolid() as PolyhedralBoundedSolid;
        }
        return brep;
    }

    /**
    The edges of the boundary representations of the parts, for renderers
    that draw the gizmo with lines over its surfaces (the edges of their
    triangle meshes would also show the diagonals of the faces).
    @param part index of a part (a region or `COMPASS_REGION`)
    @return x, y, z of the two ends of each edge, in the space of the gizmo
    */
    public getEdgeSegments(part: number): Float32Array {
        if (this.edgeSegments === null) {
            this.edgeSegments = new Array<Float32Array>(CameraControlGizmo.NUMBER_OF_PARTS);
            for (let p = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++) {
                const brep = this.exportPartToPolyhedralBoundedSolid(p);
                const offset = this.parts.get(p).getPosition();
                const n = brep.getEdgesList().size();
                const positions = new Float32Array(6 * n);
                for (let e = 0; e < n; e++) {
                    const edge: _PolyhedralBoundedSolidEdge = brep.getEdgesList().get(e)!;
                    const a = edge.rightHalf!.startingVertex.position.add(offset);
                    const b = edge.leftHalf!.startingVertex.position.add(offset);
                    positions[6 * e] = a.x();
                    positions[6 * e + 1] = a.y();
                    positions[6 * e + 2] = a.z();
                    positions[6 * e + 3] = b.x();
                    positions[6 * e + 4] = b.y();
                    positions[6 * e + 5] = b.z();
                }
                this.edgeSegments[p] = positions;
            }
        }
        return this.edgeSegments[part]!.slice();
    }

    /**
    @param region a region of the cube
    @return its direction (i, j, k), each component -1, 0 or 1
    */
    public getRegionDirectionComponents(region: number): number[] {
        return this.regionDirections[region]!.slice();
    }

    /**
    @param region a region of the cube
    @return the unit vector from the center of the cube towards the region
    */
    public getRegionDirection(region: number): Vector3Dd {
        const d = this.regionDirections[region]!;
        return new Vector3Dd(d[0]!, d[1]!, d[2]!).normalized();
    }

    /**
    @param i component along X (-1, 0 or 1)
    @param j component along Y (-1, 0 or 1)
    @param k component along Z (-1, 0 or 1)
    @return the region with that direction, or `NO_REGION` for (0, 0, 0)
    */
    public findRegion(i: number, j: number, k: number): number {
        for (let r = 0; r < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS; r++) {
            const d = this.regionDirections[r]!;
            if (d[0] === i && d[1] === j && d[2] === k) {
                return r;
            }
        }
        return CameraControlGizmo.NO_REGION;
    }

    /**
    @param region a region, `COMPASS_REGION` or `NO_REGION`
    @return one of the `KIND_*` constants
    */
    public getRegionKind(region: number): number {
        if (region === CameraControlGizmo.COMPASS_REGION) {
            return CameraControlGizmo.KIND_COMPASS;
        }
        if (region < 0 || region >= CameraControlGizmo.NUMBER_OF_CUBE_REGIONS) {
            return CameraControlGizmo.KIND_NONE;
        }
        const d = this.regionDirections[region]!;
        const nonZero = JavaMath.abs(d[0]!) + JavaMath.abs(d[1]!) + JavaMath.abs(d[2]!);
        switch (nonZero) {
            case 1:
                return CameraControlGizmo.KIND_FACE;
            case 2:
                return CameraControlGizmo.KIND_EDGE;
            default:
                return CameraControlGizmo.KIND_CORNER;
        }
    }

    /**
    @param region a face region
    @return its label (i.e. "TOP"), or null if it is not a face
    */
    public getFaceLabel(region: number): string | null {
        if (this.getRegionKind(region) !== CameraControlGizmo.KIND_FACE) {
            return null;
        }
        const d = this.regionDirections[region]!;
        for (let axis = 0; axis < 3; axis++) {
            if (d[axis] !== 0) {
                return CameraControlGizmo.FACE_LABELS[2 * axis + (d[axis]! > 0 ? 1 : 0)]!;
            }
        }
        return null;
    }

    /**
    @return a readable name of a region (i.e. "TOP FRONT RIGHT")
    */
    public regionName(region: number): string {
        if (region === CameraControlGizmo.COMPASS_REGION) {
            return "Compass";
        }
        if (region < 0 || region >= CameraControlGizmo.NUMBER_OF_CUBE_REGIONS) {
            return "None";
        }
        const d = this.regionDirections[region]!;
        let sb = "";
        // Vertical first, as in "TOP FRONT RIGHT"
        const order = [2, 1, 0];
        for (const axis of order) {
            if (d[axis] !== 0) {
                if (sb.length > 0) {
                    sb += " ";
                }
                sb += CameraControlGizmo.FACE_LABELS[2 * axis + (d[axis]! > 0 ? 1 : 0)]!;
            }
        }
        return sb;
    }

    /**
    @return the regions that are faces of the cube
    */
    public getFaceRegions(): number[] {
        const faces: number[] = [0, 0, 0, 0, 0, 0];
        let n = 0;
        for (let r = 0; r < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS; r++) {
            if (this.getRegionKind(r) === CameraControlGizmo.KIND_FACE) {
                faces[n++] = r;
            }
        }
        return faces;
    }

    /**
    @param region a face region
    @return the point of the model where its label goes (the center of the
    face, on the surface of the cube)
    */
    public getFaceLabelPosition(region: number): Vector3Dd {
        const d = this.regionDirections[region]!;
        return new Vector3Dd(d[0]!, d[1]!, d[2]!);
    }

    //= Views =============================================================

    /**
    Builds a camera rotation (as given by `Camera.getRotation()`: columns
    front, left and up) looking along `front`, with the up direction as near
    as possible to `upHint`.
    @return the rotation, or null if `upHint` is parallel to `front`
    */
    public static createCameraRotation(front: Vector3Dd, upHint: Vector3Dd): Matrix4x4d | null {
        const f = front.normalized();
        let left = upHint.crossProduct(f);

        if (left.length() < VSDK.EPSILON) {
            return null;
        }
        left = left.normalized();
        const up = f.crossProduct(left).normalized();

        return new Matrix4x4d()
            .withVal(0, 0, f.x())
            .withVal(0, 1, left.x())
            .withVal(0, 2, up.x())
            .withVal(1, 0, f.y())
            .withVal(1, 1, left.y())
            .withVal(1, 2, up.y())
            .withVal(2, 0, f.z())
            .withVal(2, 1, left.z())
            .withVal(2, 2, up.z());
    }

    /**
    Calculates the orientation of the camera for the view of a region: it
    looks at the center from the direction of the region, keeping the scene
    upright (Z up). For the TOP and BOTTOM faces, where Z can not be up, up is
    the horizontal axis (+-X or +-Y) nearest to where the current camera was
    looking (or to its up direction), so the turn is the smallest one.
    @param region a region of the cube
    @param currentRotation rotation of the camera now (columns front, left,
    up)
    @return the rotation of the camera for the view of the region
    */
    public calculateViewRotation(region: number, currentRotation: Matrix4x4d): Matrix4x4d | null {
        const front = this.getRegionDirection(region).multiply(-1);
        const d = this.regionDirections[region]!;

        if (d[0] !== 0 || d[1] !== 0) {
            return CameraControlGizmo.createCameraRotation(front, new Vector3Dd(0, 0, 1));
        }

        const currentFront = new Vector3Dd(
            currentRotation.get(0, 0),
            currentRotation.get(1, 0),
            currentRotation.get(2, 0),
        );
        const currentUp = new Vector3Dd(currentRotation.get(0, 2), currentRotation.get(1, 2), currentRotation.get(2, 2));
        // Tilting down to look at the TOP, what was ahead goes up on the
        // screen; tilting up to look at the BOTTOM, it goes down
        const hint = currentUp.add(currentFront.multiply(d[2]!));
        const candidates: Vector3Dd[] = [
            new Vector3Dd(1, 0, 0),
            new Vector3Dd(-1, 0, 0),
            new Vector3Dd(0, 1, 0),
            new Vector3Dd(0, -1, 0),
        ];
        let best = candidates[2]!;
        let bestDot = Double.NEGATIVE_INFINITY;

        for (const c of candidates) {
            const dot = c.dotProduct(hint);
            if (dot > bestDot + VSDK.EPSILON) {
                bestDot = dot;
                best = c;
            }
        }
        return CameraControlGizmo.createCameraRotation(front, best);
    }

    //= Presentation ======================================================

    /**
    @param camera camera of the view the gizmo shows and controls
    */
    public setCamera(camera: Camera | null): void {
        this.camera = camera;
        this.updateHudCamera();
    }

    public getCamera(): Camera | null {
        return this.camera;
    }

    /**
    @return the orthogonal camera the gizmo is seen through, oriented as the
    camera of the view, with a viewport of the size of the area of the gizmo
    */
    public getHudCamera(): Camera {
        return this.hudCamera;
    }

    /**
    Orients the HUD camera as the camera of the view, looking at the center
    of the model.
    */
    public updateHudCamera(): void {
        const rotation: Matrix4x4d =
            this.camera !== null
                ? this.camera.getRotation()
                : CameraControlGizmo.createCameraRotation(new Vector3Dd(1, 1, -1), new Vector3Dd(0, 0, 1))!;
        const front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0), rotation.get(2, 0)).normalized();

        this.hudCamera.setRotation(rotation);
        this.hudCamera.setPosition(front.multiply(-CameraControlGizmo.HUD_CAMERA_DISTANCE));
        this.hudCamera.updateViewportResize(this.sizeInPixels, this.sizeInPixels);
        this.hudCamera.updateVectors();
    }

    /**
    Java overloads `placeInViewport(int viewportSizeX, int titleAreaEndX,
    int titleAreaEndY, ViewportElementScaler scaler)` and
    `placeInViewport(Viewport viewport, ViewportElementScaler scaler)`.

    The first one places the gizmo at the upper right corner of a viewport,
    with the size following the resolution of the screen. The title of the
    viewport is at its upper left corner: if the viewport is so narrow that
    the gizmo would cover the title, it is moved down, under the title.
    `viewportSizeX` is the width of the viewport, in pixels; `titleAreaEndX`
    the first pixel to the right of the title area (0 if there is no title);
    `titleAreaEndY` the first pixel under the title area (0 if there is no
    title); `scaler` the scaler of the viewport set, or null to use base
    sizes.

    The second one makes the gizmo show and control the camera of a
    viewport, placed at its upper right corner, avoiding its title (see
    `Viewport.getTitleAreaSizeX`). Renderers and interaction techniques must
    place the gizmo the same way.
    */
    public placeInViewport(
        viewportSizeX: number,
        titleAreaEndX: number,
        titleAreaEndY: number,
        scaler: ViewportElementScaler | null,
    ): void;
    public placeInViewport(viewport: Viewport, scaler: ViewportElementScaler | null): void;
    public placeInViewport(
        first: number | Viewport,
        second: number | ViewportElementScaler | null,
        titleAreaEndY?: number,
        scaler?: ViewportElementScaler | null,
    ): void {
        if (typeof first !== "number") {
            const viewport = first;
            this.camera = viewport.getActiveCamera();
            this.placeInViewport(
                viewport.getPixelSizeX(),
                viewport.getTitleAreaStartX() + viewport.getTitleAreaSizeX(),
                viewport.getTitleAreaStartY() + viewport.getTitleAreaSizeY(),
                second as ViewportElementScaler | null,
            );
            return;
        }
        const viewportSizeX = first;
        const titleAreaEndX = second as number;
        const theScaler = scaler ?? null;

        if (theScaler !== null) {
            this.sizeInPixels = JavaMath.max(16, theScaler.scaleSize(this.baseSizeInPixels));
            this.marginInPixels = theScaler.scaleSize(this.baseMarginInPixels);
        } else {
            this.sizeInPixels = this.baseSizeInPixels;
            this.marginInPixels = this.baseMarginInPixels;
        }
        this.hudX = viewportSizeX - this.marginInPixels - this.sizeInPixels;
        this.hudY = this.marginInPixels;
        if (this.hudX < titleAreaEndX + this.marginInPixels) {
            this.hudY = titleAreaEndY! + this.marginInPixels;
        }
        this.updateHudCamera();
    }

    public getBaseSizeInPixels(): number {
        return this.baseSizeInPixels;
    }

    public setBaseSizeInPixels(baseSizeInPixels: number): void {
        this.baseSizeInPixels = baseSizeInPixels;
    }

    /** @return side of the square area of the gizmo, in pixels */
    public getSizeInPixels(): number {
        return this.sizeInPixels;
    }

    /** @return left of the area of the gizmo, in pixels of the viewport */
    public getHudX(): number {
        return this.hudX;
    }

    /** @return top of the area of the gizmo, in pixels of the viewport (y
    grows downwards) */
    public getHudY(): number {
        return this.hudY;
    }

    /**
    @param x horizontal pixel of the viewport
    @param y vertical pixel of the viewport (from the top)
    @return true if the pixel is in the area of the gizmo
    */
    public containsPixel(x: number, y: number): boolean {
        return (
            this.visible &&
            x >= this.hudX &&
            x < this.hudX + this.sizeInPixels &&
            y >= this.hudY &&
            y < this.hudY + this.sizeInPixels
        );
    }

    public isVisible(): boolean {
        return this.visible;
    }

    public setVisible(visible: boolean): void {
        this.visible = visible;
    }

    /**
    Projects a point of the model to the viewport.
    @return {x, y} pixel of the viewport (y from the top)
    */
    public projectToViewport(p: Vector3Dd): number[] {
        const clip = this.hudCamera.calculateProjectionMatrix().multiply(new Vector4Dd(p));
        const ndc = clip.dividedByW();

        return [
            this.hudX + ((ndc.x() + 1.0) / 2.0) * this.sizeInPixels,
            this.hudY + ((1.0 - ndc.y()) / 2.0) * this.sizeInPixels,
        ];
    }

    /**
    @param region a face region
    @return the cosine of the angle between the outward normal of the face
    and the direction towards the viewer (positive when the face is seen)
    */
    public faceVisibility(region: number): number {
        const towardsViewer = this.hudCamera.getFront().multiply(-1).normalized();
        return this.getRegionDirection(region).dotProduct(towardsViewer);
    }

    //= Picking ===========================================================

    /**
    Java overloads `pickRegion(int x, int y)` and `pickRegion(Ray ray)`.

    With a pixel (`x` horizontal, `y` vertical from the top, of the
    viewport), finds the part of the gizmo under it: the region (or
    `COMPASS_REGION`) nearest to the viewer under the pixel, or `NO_REGION`.

    With a ray in the space of the model of the gizmo, returns the region
    nearest to the origin of the ray hit by it, or `NO_REGION`.
    */
    public pickRegion(x: number, y: number): number;
    public pickRegion(ray: Ray): number;
    public pickRegion(first: number | Ray, y?: number): number {
        if (typeof first === "number") {
            const x = first;
            if (!this.containsPixel(x, y!)) {
                return CameraControlGizmo.NO_REGION;
            }
            const ray = this.hudCamera.generateRay(x - this.hudX, y! - this.hudY);
            return this.pickRegion(ray);
        }
        const ray = first;
        let nearest = CameraControlGizmo.NO_REGION;
        let nearestT = Double.POSITIVE_INFINITY;

        for (let r = 0; r < this.parts.size(); r++) {
            const hit = new RayHit(RayHit.DETAIL_NONE, true);
            if (this.parts.get(r).doIntersectionFirstHit(ray, hit)) {
                const t = hit.getRay() !== null ? hit.getRay()!.getT() : hit.getHitDistance();
                if (t < nearestT) {
                    nearestT = t;
                    nearest = r;
                }
            }
        }
        return nearest;
    }

    /**
    Intersects the line of sight of a pixel with the plane of the compass.
    @param x horizontal pixel of the area of the gizmo (0 at its left)
    @param y vertical pixel of the area of the gizmo (0 at its top)
    @param viewCamera camera that sees the gizmo (i.e. a copy of the HUD
    camera taken when a gesture started)
    @return the angle, around Z, of the point of the plane of the compass
    under the pixel, or NaN if the plane is seen almost edge on
    */
    public static compassAngleAt(x: number, y: number, viewCamera: Camera): number {
        const ray: Ray = viewCamera.generateRay(x, y);
        const d = ray.getDirection();
        if (JavaMath.abs(d.normalized().z()) < 0.1) {
            return Double.NaN;
        }
        const zc = CameraControlGizmo.COMPASS_BASE_Z + CameraControlGizmo.COMPASS_HEIGHT / 2;
        const t = (zc - ray.getOrigin().z()) / d.z();
        const p = ray.getOrigin().add(d.multiply(t));
        return Math.atan2(p.y(), p.x());
    }

    //= State and its materials ===========================================

    /**
    @return true if the cursor is over the gizmo (it is opaque then)
    */
    public isActive(): boolean {
        return this.active;
    }

    public setActive(active: boolean): void {
        if (this.active !== active) {
            this.active = active;
            this.updatePartMaterials();
        }
    }

    /**
    @return the region under the cursor, `COMPASS_REGION`, or `NO_REGION`
    */
    public getVolatileSelection(): number {
        return this.volatileSelection;
    }

    public setVolatileSelection(region: number): void {
        if (this.volatileSelection !== region) {
            this.volatileSelection = region;
            this.updatePartMaterials();
        }
    }

    public getInactiveOpacity(): number {
        return this.inactiveOpacity;
    }

    public setInactiveOpacity(inactiveOpacity: number): void {
        this.inactiveOpacity = inactiveOpacity;
        this.updatePartMaterials();
    }

    /** @return opacity of the gizmo in its current state */
    public getOpacity(): number {
        return this.active ? 1.0 : this.inactiveOpacity;
    }

    public getLineColor(): ColorRgb {
        return this.lineColor;
    }

    public getLabelColor(): ColorRgb {
        return this.labelColor;
    }

    public getHighlightColor(): ColorRgb {
        return this.highlightColor;
    }

    /**
    @param region a region or `COMPASS_REGION`
    @return the color of the part of the region, for the current state
    */
    public getPartColor(region: number): ColorRgb {
        if (region === this.volatileSelection) {
            return this.highlightColor;
        }
        switch (this.getRegionKind(region)) {
            case CameraControlGizmo.KIND_FACE:
                return this.faceColor;
            case CameraControlGizmo.KIND_EDGE:
                return this.edgeColor;
            case CameraControlGizmo.KIND_CORNER:
                return this.cornerColor;
            default:
                return this.compassColor;
        }
    }

    /**
    Gives each part a material with the color and opacity it has in the
    current state of the gizmo.
    */
    public updatePartMaterials(): void {
        const opacity = this.getOpacity();

        for (let r = 0; r < this.parts.size(); r++) {
            const c = this.getPartColor(r);
            const m = new SimpleMaterial()
                .withAmbient(c.multiply(0.45))
                .withDiffuse(c)
                .withSpecular(new ColorRgb(0.15, 0.15, 0.15))
                .withPhongExponent(20)
                .withOpacity(opacity);
            this.parts.get(r).setMaterial(m);
        }
    }
}
