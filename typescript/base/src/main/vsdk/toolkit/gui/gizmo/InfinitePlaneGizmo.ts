import { Date as JavaDate } from "../../../../java/util/Date.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import { Gizmo } from "./Gizmo.js";

/**
Java's `InfinitePlaneGizmo.PlaneSnapshot` is a record whose compact constructor
validates the three components and then replaces each with a defensive copy —
the plane and the point cloned, the normal normalized. Those copies happen in
the constructor below for the same reason: the snapshot is posted from one
place and read from another, and neither may see the other's later edits.
*/
export class PlaneSnapshot {
    public readonly plane: InfinitePlane;
    public readonly point: Vector3Dd;
    public readonly normal: Vector3Dd;

    public constructor(plane: InfinitePlane | null, point: Vector3Dd | null, normal: Vector3Dd | null) {
        if (plane === null) {
            throw new Error("plane cannot be null");
        }
        if (point === null) {
            throw new Error("point cannot be null");
        }
        if (normal === null || normal.length() < VSDK.EPSILON) {
            throw new Error("normal cannot be null or zero");
        }
        this.plane = new InfinitePlane(plane);
        this.point = new Vector3Dd(point);
        this.normal = normal.normalized();
    }
}

/**
Port of `vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo`.

A gizmo that carries a cutting plane posted by a tangible-interface marker: the
plane itself, the point the marker sits at, and the normal it points along.
Like {@link RayGizmo}, it hides itself once no data has arrived for
{@link DEFAULT_DISABLE_TIME} seconds, which is what `update` checks.

Java holds the pending snapshot in an `AtomicReference` because its producer
and its consumer are two OS threads. A browser page runs both on one event
loop, where a plain field already has the get-and-set semantics the contract
asks for: nothing can interleave between the read and the write in
{@link acquireSnapshot}. The contract itself — latest update wins, consumed
exactly once, the current plane untouched for the rest of the frame — is
unchanged. This is the same reading recorded for `RayGizmo`.
*/
export class InfinitePlaneGizmo extends Gizmo {
    public static readonly DEFAULT_DISABLE_TIME = 2.0;
    public static readonly DEFAULT_FRAME_COLOR = new ColorRgb(1, 1, 1);

    private pendingSnapshot: PlaneSnapshot | null = null;

    private currentPlane: InfinitePlane;
    private currentPoint: Vector3Dd;
    private currentNormal: Vector3Dd;

    private lastDataTime: JavaDate;
    private previousDataTime: JavaDate;
    private visible: boolean;
    private disableAfterElapsedSeconds: number;
    private frameColor: ColorRgb;

    public constructor() {
        super();
        this.currentPoint = new Vector3Dd(0, 0, 0);
        this.currentNormal = new Vector3Dd(0, 0, 1);
        this.currentPlane = new InfinitePlane(this.currentNormal, this.currentPoint);

        this.lastDataTime = new JavaDate();
        this.previousDataTime = new JavaDate();
        this.visible = true;
        this.disableAfterElapsedSeconds = InfinitePlaneGizmo.DEFAULT_DISABLE_TIME;
        this.frameColor = InfinitePlaneGizmo.DEFAULT_FRAME_COLOR;
    }

    /**
    Java overloads `setPlane` on three arguments and on two, the two-argument
    one building the plane from the point and the normal. TypeScript spells
    that as one optional first argument.
    */
    public setPlane(plane: InfinitePlane | null, point: Vector3Dd | null, normal: Vector3Dd | null): void;
    public setPlane(point: Vector3Dd | null, normal: Vector3Dd | null): void;
    public setPlane(a: InfinitePlane | Vector3Dd | null, b: Vector3Dd | null, c?: Vector3Dd | null): void {
        if (c === undefined) {
            const point: Vector3Dd | null = a as Vector3Dd | null;
            const normal: Vector3Dd | null = b;
            if (point === null || normal === null || normal.length() < VSDK.EPSILON) {
                return;
            }
            this.setPlane(new InfinitePlane(normal, point), point, normal);
            return;
        }

        const plane: InfinitePlane | null = a as InfinitePlane | null;
        if (plane === null || b === null || c === null || c.length() < VSDK.EPSILON) {
            return;
        }

        this.pendingSnapshot = new PlaneSnapshot(plane, b, c);
        this.visible = true;
        this.recordDataArrival();
    }

    public acquireSnapshot(): PlaneSnapshot | null {
        const snap: PlaneSnapshot | null = this.pendingSnapshot;
        this.pendingSnapshot = null;
        if (snap === null) {
            return null;
        }

        this.currentPlane = new InfinitePlane(snap.plane);
        this.currentPoint = new Vector3Dd(snap.point);
        this.currentNormal = snap.normal.normalized();
        return snap;
    }

    public update(): void {
        if (this.inactivityThresholdExceeded()) {
            this.visible = false;
        }
        this.previousDataTime = this.lastDataTime;
    }

    public getPlane(): InfinitePlane {
        return this.currentPlane;
    }

    public getPoint(): Vector3Dd {
        return this.currentPoint;
    }

    public getNormal(): Vector3Dd {
        return this.currentNormal;
    }

    public isVisible(): boolean {
        return this.visible;
    }

    public setVisible(visible: boolean): void {
        this.visible = visible;
    }

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

    public getFrameColor(): ColorRgb {
        return this.frameColor;
    }

    public setFrameColor(frameColor: ColorRgb | null): void {
        if (frameColor !== null) {
            this.frameColor = frameColor;
        }
    }

    private recordDataArrival(): void {
        this.previousDataTime = this.lastDataTime;
        this.lastDataTime = new JavaDate();
    }

    private inactivityThresholdExceeded(): boolean {
        const elapsed: number = (new JavaDate().getTime() - this.lastDataTime.getTime()) / 1000.0;
        return this.disableAfterElapsedSeconds > 0.0 && elapsed > this.disableAfterElapsedSeconds;
    }
}
