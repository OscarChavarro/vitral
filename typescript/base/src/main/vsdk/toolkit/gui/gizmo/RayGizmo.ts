import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Date as JavaDate } from "../../../../java/util/Date.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { Intersection } from "../../environment/geometry/element/Intersection.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { Arrow } from "../../environment/geometry/volume/Arrow.js";
import { Sphere } from "../../environment/geometry/volume/Sphere.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { Gizmo } from "./Gizmo.js";
import { InputGizmo } from "./InputGizmo.js";

/**
Java's `RayGizmo.RaySnapshot` is a record whose canonical constructor checks
that the two lists match in length and then copies them into unmodifiable
lists.
*/
export class RaySnapshot {
    public readonly rays: readonly Ray[];
    public readonly intersections: readonly (Intersection | null)[];

    public constructor(
        public readonly rotationAngleInRadians: number,
        rays: readonly Ray[],
        intersections: readonly (Intersection | null)[],
    ) {
        if (rays.length !== intersections.length) {
            throw new Error(
                "rays and intersections must be the same size: " + rays.length + " vs " + intersections.length,
            );
        }
        this.rays = Object.freeze([...rays]);
        this.intersections = Object.freeze([...intersections]);
    }
}

/**
Java record `RayGizmo.IndicatorMesh`: geometry of a single triangle, used to
mark the current roll angle around the ray direction: a fin pointing in local
+X at the base of the arrow (z=0), with its tip lifted towards +Z. It is
generated once around the local +Z axis; the renderer rotates it by the roll
angle of the gizmo (see `getRotationAngleInRadians`) and transforms it with the
body of the arrow.
*/
export class RollIndicatorMesh {
    /**
    @param positions 3 vertexes (tip, base-left, base-right), 3 floats each
    @param normals one repeated flat normal, 3 floats each
    @param uvs 3 texture coordinates, 2 floats each
    */
    public constructor(
        public readonly positions: Float32Array,
        public readonly normals: Float32Array,
        public readonly uvs: Float32Array,
    ) {}
}

/**
Gizmo that represents a 3D ray (origin + direction) as a volumetric arrow.

The Arrow geometry is defined in object space along the +Z axis.  The
internal {@link SimpleBody} is positioned and rotated so that the arrow
points from the ray origin toward its direction.

An optional callback can be provided at construction time to query scene
intersections.  When present, RayGizmo calls it for each ray bounce (primary +
reflections up to `maxNumOfReflections`) and stores the results in the snapshot
so the renderer can visualize hits and missed rays differently.

Thread-safety contract:
  - The network (or any non-render) thread calls {@link setRay} to post a
    new ray.  The call is non-blocking and always stores the *latest*
    update; intermediate updates are discarded if the render thread has not
    consumed the previous one yet.
  - The render thread calls {@link acquireSnapshot} once per frame.  If a
    new snapshot is pending it is applied to the {@link SimpleBody} and
    returned; otherwise `null` is returned and the body is unchanged.
  - During a single frame the body is never modified: it is only updated at
    the moment {@link acquireSnapshot} is called, before any drawing begins.

Java holds the pending snapshot in an `AtomicReference` because its producer
and its consumer are two OS threads. A browser page runs both on one event
loop, where a plain field already has the getAndSet semantics the contract
asks for: nothing can interleave between the read and the write below. The
contract itself — latest update wins, consumed exactly once, body untouched
for the rest of the frame — is unchanged.
*/
export class RayGizmo extends Gizmo {
    public static readonly DEFAULT_DISABLE_TIME = 2.0;

    private static readonly ARROW_BASE_LENGTH = 3.0;
    private static readonly ARROW_HEAD_LENGTH = 1.0;
    private static readonly RAY_THICKNESS_FACTOR = 0.5;
    private static readonly ARROW_BASE_RADIUS = 0.15 * RayGizmo.RAY_THICKNESS_FACTOR;
    private static readonly ARROW_HEAD_RADIUS = 0.4 * RayGizmo.RAY_THICKNESS_FACTOR;
    private static readonly REFERENCE_VERTICAL_FOV_DEGREES = 60.0;
    private static readonly NORMAL_VIEWPORT_HEIGHT_FRACTION = 0.2;

    public static readonly DEFAULT_RAY_COLOR = new ColorRgb(1, 0.8, 0.8);
    public static readonly DEFAULT_NORMAL_COLOR = new ColorRgb(1, 1, 0.8);

    // Colors of the boxes of the input gizmo: x, y, z, yaw and pitch
    private static readonly INPUT_FIELD_COLORS: readonly ColorRgb[] = [
        new ColorRgb(1, 0, 0),
        new ColorRgb(0, 1, 0),
        new ColorRgb(0, 0, 1),
        new ColorRgb(1, 1, 0),
        new ColorRgb(0, 1, 1),
    ];

    private readonly arrow: Arrow;
    private readonly dotSphere: Sphere;
    private readonly body: SimpleBody;
    private readonly intersectionCallback: ((ray: Ray) => Intersection | null) | null;
    private readonly inputGizmo: InputGizmo;
    private maxNumOfReflections: number;
    private pendingSnapshot: RaySnapshot | null = null;

    private currentPosition: Vector3Dd;
    private currentDirection: Vector3Dd;
    private currentRotationAngleInRadians: number;
    private currentSnapshot: RaySnapshot | null = null;

    private lastDataTime: JavaDate;
    private previousDataTime: JavaDate;
    private visible: boolean;
    private rollIndicatorEnabled: boolean;
    private disableAfterElapsedSeconds: number;

    private sourceRayColor: ColorRgb;
    private normalRayColors: ColorRgb[];
    private reflectedRayColors: ColorRgb[];
    private refractedRayColors: ColorRgb[];

    /**
    @param intersectionCallback called per-bounce to query scene hits; may be null
    @param maxNumOfReflections  number of reflection bounces to trace (0 = primary only)
    */
    public constructor(intersectionCallback: ((ray: Ray) => Intersection | null) | null, maxNumOfReflections: number) {
        super();
        this.arrow = new Arrow(
            RayGizmo.ARROW_BASE_LENGTH,
            RayGizmo.ARROW_HEAD_LENGTH,
            RayGizmo.ARROW_BASE_RADIUS,
            RayGizmo.ARROW_HEAD_RADIUS,
        );
        this.dotSphere = new Sphere(RayGizmo.ARROW_BASE_RADIUS);

        this.body = new SimpleBody();
        this.body.setGeometry(this.arrow);

        let mat = new SimpleMaterial();
        mat = mat.withAmbient(new ColorRgb(0.1, 0.0, 0.0));
        mat = mat.withDiffuse(new ColorRgb(0.9, 0.2, 0.1));
        mat = mat.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        mat = mat.withPhongExponent(32.0);
        this.body.setMaterial(mat);

        this.intersectionCallback = intersectionCallback;
        this.maxNumOfReflections = maxNumOfReflections;
        this.inputGizmo = new InputGizmo(5, 3, 3);
        for (let i = 0; i < RayGizmo.INPUT_FIELD_COLORS.length; i++) {
            this.inputGizmo.setFieldColor(i, RayGizmo.INPUT_FIELD_COLORS[i]!);
        }

        this.currentPosition = new Vector3Dd(0, 0, 0);
        this.currentDirection = new Vector3Dd(0, 0, 1);
        this.currentRotationAngleInRadians = 0.0;
        this.applyTransform(this.currentPosition, this.currentDirection);
        this.syncInputGizmo(this.currentPosition, this.currentDirection);

        this.lastDataTime = new JavaDate();
        this.previousDataTime = new JavaDate();
        this.visible = true;
        this.rollIndicatorEnabled = false;
        this.disableAfterElapsedSeconds = RayGizmo.DEFAULT_DISABLE_TIME;

        this.sourceRayColor = RayGizmo.DEFAULT_RAY_COLOR;
        this.normalRayColors = [RayGizmo.DEFAULT_NORMAL_COLOR];
        this.reflectedRayColors = [RayGizmo.DEFAULT_RAY_COLOR];
        this.refractedRayColors = [RayGizmo.DEFAULT_RAY_COLOR];
    }

    public getPosition(): Vector3Dd {
        return this.currentPosition;
    }

    public getDirection(): Vector3Dd {
        return this.currentDirection;
    }

    public setRay(ray: Ray | null, rotationAngleInRadians: number): void {
        if (ray === null) {
            return;
        }

        const rays: Ray[] = [];
        const intersections: (Intersection | null)[] = [];

        const primaryIntersection: Intersection | null =
            this.intersectionCallback !== null ? this.intersectionCallback(ray) : null;
        rays.push(ray);
        intersections.push(primaryIntersection);

        if (this.intersectionCallback !== null && primaryIntersection !== null) {
            let currentRay: Ray = ray;
            let currentIntersection: Intersection = primaryIntersection;
            for (let i = 0; i < this.maxNumOfReflections; i++) {
                const reflectedRay: Ray | null = RayGizmo.computeReflectedRay(currentRay, currentIntersection);
                if (reflectedRay === null) {
                    break;
                }
                const reflectedIntersection: Intersection | null = this.intersectionCallback(reflectedRay);
                rays.push(reflectedRay);
                intersections.push(reflectedIntersection);
                if (reflectedIntersection === null) {
                    break;
                }
                currentRay = reflectedRay;
                currentIntersection = reflectedIntersection;
            }
        }

        this.pendingSnapshot = new RaySnapshot(rotationAngleInRadians, rays, intersections);
        this.syncInputGizmo(ray.getOrigin(), ray.getDirection());
        this.visible = true;
        this.recordDataArrival();
    }

    public getMaxNumOfReflections(): number {
        return this.maxNumOfReflections;
    }

    public setMaxNumOfReflections(maxNumOfReflections: number): void {
        this.maxNumOfReflections = Math.max(0, maxNumOfReflections);
    }

    public getInputGizmo(): InputGizmo {
        return this.inputGizmo;
    }

    public update(): void {
        if (this.inactivityThresholdExceeded()) {
            this.visible = false;
        }
        this.previousDataTime = this.lastDataTime;
    }

    public isVisible(): boolean {
        return this.visible;
    }

    public setVisible(visible: boolean): void {
        this.visible = visible;
    }

    public isRollIndicatorEnabled(): boolean { return this.rollIndicatorEnabled; }
    public setRollIndicatorEnabled(rollIndicatorEnabled: boolean): void { this.rollIndicatorEnabled = rollIndicatorEnabled; }

    public getDisableAfterElapsedSeconds(): number {
        return this.disableAfterElapsedSeconds;
    }

    public setDisableAfterElapsedSeconds(disableAfterElapsedSeconds: number): void {
        this.disableAfterElapsedSeconds = disableAfterElapsedSeconds;
    }

    public getLastDataTime(): JavaDate {
        return this.lastDataTime;
    }

    public getPreviousDataTime(): JavaDate {
        return this.previousDataTime;
    }

    public acquireSnapshot(): RaySnapshot | null {
        const snap: RaySnapshot | null = this.pendingSnapshot;
        this.pendingSnapshot = null;
        if (snap === null) {
            return null;
        }
        if (snap.rays.length !== 0) {
            const primary: Ray = snap.rays[0]!;
            this.applyTransform(primary.getOrigin(), primary.getDirection());
            this.syncInputGizmo(primary.getOrigin(), primary.getDirection());
        }
        this.currentRotationAngleInRadians = snap.rotationAngleInRadians;
        this.currentSnapshot = snap;
        return snap;
    }

    public getCurrentSnapshot(): RaySnapshot | null {
        return this.currentSnapshot;
    }

    public getRotationAngleInRadians(): number {
        return this.currentRotationAngleInRadians;
    }

    public getBody(): SimpleBody {
        return this.body;
    }

    public getArrow(): Arrow {
        return this.arrow;
    }

    /**
    Builds a {@link SimpleScene} from the current snapshot, with all bodies
    (ray arrows, normal arrows, dot spheres) fully configured — materials,
    positions, rotations, and scales included.  The scene is ready to be
    consumed by any renderer without additional logical work.

    Body types present in the returned scene:
      - {@link Arrow} bodies: ray arrows (source + reflections) and
        surface-normal arrows (thinner).
      - {@link Sphere} bodies: three dot-spheres beyond a ray arrow that
        misses geometry.

    @return a new scene; empty only if the gizmo has no snapshot yet
    */
    public buildScene(camera: Camera | null = null): SimpleScene {
        const scene = new SimpleScene();
        const arrowTotalLength: number = RayGizmo.ARROW_BASE_LENGTH + RayGizmo.ARROW_HEAD_LENGTH;
        const thicknessScale = RayGizmo.calculateViewThicknessScale(camera);

        if (this.currentSnapshot === null || this.currentSnapshot.rays.length === 0) {
            scene.addBody(
                this.buildRayBody(
                    new Ray(this.currentPosition, this.currentDirection),
                    null,
                    this.sourceRayColor,
                    arrowTotalLength,
                    RayGizmo.calculateMissingRayLength(camera, this.currentPosition),
                    thicknessScale,
                ),
            );
            return scene;
        }

        const normalColors: ColorRgb[] = this.normalRayColors;

        // Source ray (index 0)
        const sourceRay: Ray = this.currentSnapshot.rays[0]!;
        const sourceIntersection: Intersection | null = this.currentSnapshot.intersections[0]!;
        const sourceLength = sourceIntersection === null ? RayGizmo.calculateMissingRayLength(camera, sourceRay.getOrigin()) : arrowTotalLength;
        scene.addBody(this.buildRayBody(sourceRay, sourceIntersection, this.sourceRayColor, arrowTotalLength, sourceLength, thicknessScale));

        if (sourceIntersection === null) {
            this.addDotBodies(scene, sourceRay, camera, sourceLength, RayGizmo.calculateVisibleNormalLength(camera, sourceRay.getOrigin()), this.sourceRayColor);
        } else {
            this.addNormalBody(scene, sourceIntersection, normalColors[0 % normalColors.length]!, camera, thicknessScale);
        }

        // Reflection bounces (index 1+)
        for (let i = 1; i < this.currentSnapshot.rays.length; i++) {
            const reflRay: Ray = this.currentSnapshot.rays[i]!;
            const reflIntersection: Intersection | null = this.currentSnapshot.intersections[i]!;
            const reflColor: ColorRgb = this.reflectedRayColors[(i - 1) % this.reflectedRayColors.length]!;
            const reflectedLength = reflIntersection === null ? RayGizmo.calculateMissingRayLength(camera, reflRay.getOrigin()) : arrowTotalLength;
            scene.addBody(this.buildRayBody(reflRay, reflIntersection, reflColor, arrowTotalLength, reflectedLength, thicknessScale));

            if (reflIntersection === null) {
                this.addDotBodies(scene, reflRay, camera, reflectedLength, RayGizmo.calculateVisibleNormalLength(camera, reflRay.getOrigin()), reflColor);
            } else {
                this.addNormalBody(scene, reflIntersection, normalColors[i % normalColors.length]!, camera, thicknessScale);
            }
        }

        return scene;
    }

    private buildRayBody(
        ray: Ray,
        intersection: Intersection | null,
        color: ColorRgb,
        arrowTotalLength: number,
        displayedLength: number,
        thicknessScale: number,
    ): SimpleBody {
        let rayLength = displayedLength;
        if (intersection !== null) {
            const hitT: number = intersection.getT();
            if (hitT > 1e-6 && arrowTotalLength > 1e-6) {
                rayLength = hitT;
            }
        }
        const b = new SimpleBody();
        b.setGeometry(RayGizmo.buildRayArrow(rayLength));
        b.setPosition(ray.getOrigin());
        b.setRotation(RayGizmo.rotationFromZToDirection(ray.getDirection()));
        b.setScale(new Vector3Dd(thicknessScale, thicknessScale, 1.0));
        b.setMaterial(RayGizmo.materialFromColor(color));
        return b;
    }

    private static buildRayArrow(requestedLength: number): Arrow {
        const totalLength = Math.max(requestedLength, RayGizmo.ARROW_HEAD_LENGTH);
        return new Arrow(Math.max(0, totalLength - RayGizmo.ARROW_HEAD_LENGTH), RayGizmo.ARROW_HEAD_LENGTH,
            RayGizmo.ARROW_BASE_RADIUS, RayGizmo.ARROW_HEAD_RADIUS);
    }

    private addNormalBody(scene: SimpleScene, intersection: Intersection, color: ColorRgb, camera: Camera | null, thicknessScale: number): void {
        // Java returns early when either accessor answers null. `Intersection`
        // declares both non-nullable in TypeScript, so that guard is
        // unreachable here and is left out rather than written as dead code.
        const hitPoint: Vector3Dd = intersection.getPoint();
        const normal: Vector3Dd = intersection.getNormal();
        const b = new SimpleBody();
        b.setGeometry(this.arrow);
        b.setPosition(hitPoint);
        b.setRotation(RayGizmo.rotationFromZToDirection(normal));
        b.setScale(new Vector3Dd(0.5 * thicknessScale, 0.5 * thicknessScale,
            RayGizmo.calculateVisibleNormalLength(camera, hitPoint) / (RayGizmo.ARROW_BASE_LENGTH + RayGizmo.ARROW_HEAD_LENGTH)));
        b.setMaterial(RayGizmo.materialFromColor(color));
        scene.addBody(b);
    }

    private addDotBodies(scene: SimpleScene, ray: Ray, camera: Camera | null, rayLength: number, normalLength: number, rayColor: ColorRgb): void {
        const len: number = ray.getDirection().length();
        if (len < VSDK.EPSILON) {
            return;
        }
        const dir: Vector3Dd = ray.getDirection().multiply(1.0 / len);
        const origin: Vector3Dd = ray.getOrigin();
        const dotMat: SimpleMaterial = RayGizmo.materialFromColor(rayColor);
        const radius = (camera === null ? RayGizmo.ARROW_BASE_RADIUS : normalLength * 0.05) / 3.0;
        const sphereScale = radius / RayGizmo.ARROW_BASE_RADIUS;
        const firstOffset = camera === null ? rayLength * 0.25 : 2.5 * radius;
        const spacing = camera === null ? rayLength * 0.25 : 3.0 * radius;
        for (let i = 0; i < 3; i++) {
            const dotPos: Vector3Dd = origin.add(dir.multiply(rayLength + firstOffset + i * spacing));
            const b = new SimpleBody();
            b.setGeometry(this.dotSphere);
            b.setPosition(dotPos);
            b.setScale(new Vector3Dd(sphereScale, sphereScale, sphereScale));
            b.setMaterial(dotMat);
            scene.addBody(b);
        }
    }

    public static calculateViewThicknessScale(camera: Camera | null): number {
        if (camera === null) return 1.0;
        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) return 1.0 / Math.max(camera.getOrthogonalZoom(), VSDK.EPSILON);
        return Math.max(Math.tan(JavaMath.toRadians(camera.getFov()) / 2.0), VSDK.EPSILON) /
            Math.tan(JavaMath.toRadians(RayGizmo.REFERENCE_VERTICAL_FOV_DEGREES) / 2.0);
    }

    public static calculateVisibleNormalLength(camera: Camera | null, position: Vector3Dd | null): number {
        const legacy = RayGizmo.ARROW_BASE_LENGTH + RayGizmo.ARROW_HEAD_LENGTH;
        if (camera === null || position === null) return legacy;
        const viewportHeight = Math.max(camera.getViewportYSize(), 1.0);
        let worldPerPixel: number;
        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            worldPerPixel = 2.0 / Math.max(Math.abs(camera.getOrthogonalZoom()), VSDK.EPSILON) / viewportHeight;
        } else {
            const depth = Math.max(Math.abs(position.subtract(camera.getPosition()).dotProduct(camera.getFront())), camera.getNearPlaneDistance());
            worldPerPixel = 2.0 * depth * Math.max(Math.tan(JavaMath.toRadians(camera.getFov()) / 2.0), VSDK.EPSILON) / viewportHeight;
        }
        return RayGizmo.NORMAL_VIEWPORT_HEIGHT_FRACTION * viewportHeight * worldPerPixel;
    }

    public static calculateMissingRayLength(camera: Camera | null, origin: Vector3Dd): number {
        return 2.0 * RayGizmo.calculateVisibleNormalLength(camera, origin);
    }

    public getRollIndicatorModelMatrix(camera: Camera | null): Matrix4x4d {
        const scale = RayGizmo.calculateViewThicknessScale(camera);
        return this.body.getTransformationMatrix().multiply(new Matrix4x4d().scale(scale, scale, 1.0));
    }

    private static materialFromColor(color: ColorRgb): SimpleMaterial {
        let m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(color.r() * 0.1, color.g() * 0.1, color.b() * 0.1));
        m = m.withDiffuse(color);
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withPhongExponent(32.0);
        return m;
    }

    public getSourceRayColor(): ColorRgb {
        return this.sourceRayColor;
    }

    public setSourceRayColor(sourceRayColor: ColorRgb): void {
        this.sourceRayColor = sourceRayColor;
    }

    public getNormalRayColors(): ColorRgb[] {
        return this.normalRayColors;
    }

    public setNormalRayColors(normalRayColors: readonly ColorRgb[]): void {
        this.normalRayColors = [...normalRayColors];
    }

    public getReflectedRayColors(): ColorRgb[] {
        return this.reflectedRayColors;
    }

    public setReflectedRayColors(reflectedRayColors: readonly ColorRgb[]): void {
        this.reflectedRayColors = [...reflectedRayColors];
    }

    public getRefractedRayColors(): ColorRgb[] {
        return this.refractedRayColors;
    }

    public setRefractedRayColors(refractedRayColors: readonly ColorRgb[]): void {
        this.refractedRayColors = [...refractedRayColors];
    }

    /// Outer radius of the roll indicator fin (see `buildIndicatorMesh`)
    private static readonly ROLL_INDICATOR_OUTER_RADIUS = Math.fround(0.65);
    /// Inner radius of the roll indicator fin
    private static readonly ROLL_INDICATOR_INNER_RADIUS = Math.fround(0.17);
    /// Half width of the roll indicator fin, at its base
    private static readonly ROLL_INDICATOR_HALF_WIDTH = Math.fround(0.12);
    /// Distance of the tip of the roll indicator fin, along +Z
    private static readonly ROLL_INDICATOR_TIP_Z = Math.fround(0.3);

    /**
    @return the geometry of the roll indicator fin, in the local space of the
    arrow (see {@link IndicatorMesh})
    */
    public static buildRollIndicatorMesh(): RollIndicatorMesh {
        const outer: number = RayGizmo.ROLL_INDICATOR_OUTER_RADIUS;
        const inner: number = RayGizmo.ROLL_INDICATOR_INNER_RADIUS;
        const halfWidth: number = RayGizmo.ROLL_INDICATOR_HALF_WIDTH;
        const tipZ: number = RayGizmo.ROLL_INDICATOR_TIP_Z;
        // P0=tip, P1=base-left, P2=base-right.
        const positions = new Float32Array([outer, 0.0, tipZ, inner, -halfWidth, 0.0, inner, halfWidth, 0.0]);
        const uvs = new Float32Array([0.5, 1.0, 0.0, 0.0, 1.0, 0.0]);
        // Java computes the normal in float arithmetic: every step is rounded
        const f = Math.fround;
        const ax: number = f(inner - outer);
        const ay: number = f(-halfWidth);
        const az: number = f(-tipZ);
        const bx: number = f(inner - outer);
        const by: number = halfWidth;
        const bz: number = f(-tipZ);
        let nx: number = f(f(ay * bz) - f(az * by));
        let ny: number = f(f(az * bx) - f(ax * bz));
        let nz: number = f(f(ax * by) - f(ay * bx));
        const normalLength: number = f(Math.sqrt(f(f(f(nx * nx) + f(ny * ny)) + f(nz * nz))));

        nx = f(nx / normalLength);
        ny = f(ny / normalLength);
        nz = f(nz / normalLength);

        const normals = new Float32Array([nx, ny, nz, nx, ny, nz, nx, ny, nz]);

        return new RollIndicatorMesh(positions, normals, uvs);
    }

    private recordDataArrival(): void {
        this.previousDataTime = this.lastDataTime;
        this.lastDataTime = new JavaDate();
    }

    private inactivityThresholdExceeded(): boolean {
        const elapsed: number = (new JavaDate().getTime() - this.lastDataTime.getTime()) / 1000.0;
        return this.disableAfterElapsedSeconds > 0.0 && elapsed > this.disableAfterElapsedSeconds;
    }

    private applyTransform(position: Vector3Dd, direction: Vector3Dd): void {
        this.currentPosition = position;
        this.currentDirection = direction;

        const rotation: Matrix4x4d = RayGizmo.rotationFromZToDirection(direction);
        const rotationInverse: Matrix4x4d = rotation.invert();

        this.body.setPosition(position);
        this.body.setRotation(rotation);
        this.body.setRotationInverse(rotationInverse);
    }

    private syncInputGizmo(position: Vector3Dd, direction: Vector3Dd): void {
        const d: Vector3Dd = direction.length() > VSDK.EPSILON ? direction.normalized() : new Vector3Dd(0, 0, 1);
        const yaw: number = Math.atan2(d.y(), d.x());
        const pitch: number = Math.asin(Math.max(-1.0, Math.min(1.0, d.z())));

        this.inputGizmo.setValue(0, position.x());
        this.inputGizmo.setValue(1, position.y());
        this.inputGizmo.setValue(2, position.z());
        this.inputGizmo.setValue(3, JavaMath.toDegrees(yaw));
        this.inputGizmo.setValue(4, JavaMath.toDegrees(pitch));
    }

    private static computeReflectedRay(incomingRay: Ray, intersection: Intersection | null): Ray | null {
        if (intersection === null) {
            return null;
        }
        // As in `addNormalBody`, Java's null guard on these two accessors is
        // unreachable under the TypeScript declarations.
        const hitPoint: Vector3Dd = intersection.getPoint();
        const normal: Vector3Dd = intersection.getNormal();
        const normalLen: number = normal.length();
        if (normalLen < VSDK.EPSILON) {
            return null;
        }
        const n: Vector3Dd = normal.multiply(1.0 / normalLen);
        const d: Vector3Dd = incomingRay.getDirection();
        const dot: number = d.dotProduct(n);
        const r: Vector3Dd = d.subtract(n.multiply(2.0 * dot));
        if (r.length() < VSDK.EPSILON) {
            return null;
        }
        const origin: Vector3Dd = hitPoint.add(n.multiply(1e-4));
        return new Ray(origin, r);
    }

    private static rotationFromZToDirection(direction: Vector3Dd): Matrix4x4d {
        const len: number = direction.length();
        if (len < VSDK.EPSILON) {
            return new Matrix4x4d();
        }

        const d: Vector3Dd = direction.multiply(1.0 / len);
        const z = new Vector3Dd(0, 0, 1);
        const dot: number = z.dotProduct(d);

        if (dot > 1.0 - VSDK.EPSILON) {
            return new Matrix4x4d();
        }
        if (dot < -1.0 + VSDK.EPSILON) {
            return new Matrix4x4d().axisRotation(Math.PI, 1, 0, 0);
        }

        const axis: Vector3Dd = z.crossProduct(d).normalized();
        const angle: number = Math.acos(dot);
        return new Matrix4x4d().axisRotation(angle, axis.x(), axis.y(), axis.z());
    }
}
