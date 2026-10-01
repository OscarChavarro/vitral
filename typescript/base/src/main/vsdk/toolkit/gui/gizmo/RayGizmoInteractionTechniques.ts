import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { KeyEvent } from "../KeyEvent.js";
import type { InputGizmo } from "./InputGizmo.js";
import { RayGizmo } from "./RayGizmo.js";

/**
Keyboard interaction for a {@link RayGizmo}. The ray can be toggled with one
of the configured activation keys and then controlled from the numeric keypad:
NUM4/NUM6 move X, NUM2/NUM8 move Y, NUM1/NUM7 move Z, NUM* and NUM/ change
yaw, and NUM+/NUM- change pitch. Regular numeric editing is delegated to the
gizmo's five-field {@link InputGizmo}: x, y, z, yaw and pitch.
*/
export class RayGizmoInteractionTechniques {
    private static readonly ORIGIN_STEP = 0.1;
    private static readonly ANGLE_STEP_DEGREES = 5.0;

    private readonly gizmo: RayGizmo;
    private readonly activationKeys: number[];
    private active: boolean;
    private origin: Vector3Dd;
    private yawInDegrees = 0;
    private pitchInDegrees = 0;

    public constructor(gizmo: RayGizmo, ...activationKeys: number[]) {
        this.gizmo = gizmo;
        this.activationKeys = activationKeys.slice();
        const ray: Ray = this.currentRay();
        this.origin = ray.getOrigin();
        this.updateAnglesFromDirection(ray.getDirection());
        this.active = gizmo.isVisible();
        this.updateInputGizmo();
    }

    public isActive(): boolean {
        return this.active;
    }

    public setActive(active: boolean): void {
        this.active = active;
        this.gizmo.setVisible(active);
        this.gizmo.setDisableAfterElapsedSeconds(active ? Number.POSITIVE_INFINITY : RayGizmo.DEFAULT_DISABLE_TIME);
        if (active) {
            this.applyRay();
        }
    }

    public getInputGizmo(): InputGizmo {
        return this.gizmo.getInputGizmo();
    }

    public processKeyPressedEvent(event: KeyEvent | null): boolean {
        if (event === null) {
            return false;
        }
        this.active = this.gizmo.isVisible();
        if (this.isActivationKey(event) || event.keycode === KeyEvent.KEY_NUM5) {
            this.setActive(!this.active);
            return true;
        }
        if (!this.active) {
            return false;
        }
        if (this.processNumericPadCommand(event)) {
            this.getInputGizmo().cancelEditing();
            this.applyRay();
            return true;
        }
        if (this.getInputGizmo().consumesKey(event)) {
            this.getInputGizmo().processKeyPressedEvent(event);
            if (this.getInputGizmo().consumeCommit()) {
                this.applyInputGizmoValues();
                this.getInputGizmo().cancelEditing();
                this.applyRay();
            }
            return true;
        }
        return false;
    }

    public setRay(ray: Ray | null): void {
        if (ray === null) {
            return;
        }
        this.origin = ray.getOrigin();
        this.updateAnglesFromDirection(ray.getDirection());
        this.updateInputGizmo();
        this.gizmo.setRay(ray, 0.0);
    }

    private processNumericPadCommand(event: KeyEvent): boolean {
        const step: number = RayGizmoInteractionTechniques.ORIGIN_STEP;
        const angleStep: number = RayGizmoInteractionTechniques.ANGLE_STEP_DEGREES;

        switch (event.keycode) {
            case KeyEvent.KEY_NUM4:
                this.origin = this.origin.add(new Vector3Dd(-step, 0, 0));
                return true;
            case KeyEvent.KEY_NUM6:
                this.origin = this.origin.add(new Vector3Dd(step, 0, 0));
                return true;
            case KeyEvent.KEY_NUM2:
                this.origin = this.origin.add(new Vector3Dd(0, -step, 0));
                return true;
            case KeyEvent.KEY_NUM8:
                this.origin = this.origin.add(new Vector3Dd(0, step, 0));
                return true;
            case KeyEvent.KEY_NUM1:
                this.origin = this.origin.add(new Vector3Dd(0, 0, -step));
                return true;
            case KeyEvent.KEY_NUM7:
                this.origin = this.origin.add(new Vector3Dd(0, 0, step));
                return true;
            case KeyEvent.KEY_NUMASTERISK:
                this.yawInDegrees -= angleStep;
                return true;
            case KeyEvent.KEY_NUMSLASH:
                this.yawInDegrees += angleStep;
                return true;
            case KeyEvent.KEY_NUMPLUS:
                this.pitchInDegrees += angleStep;
                this.clampPitch();
                return true;
            case KeyEvent.KEY_NUMMINUS:
                this.pitchInDegrees -= angleStep;
                this.clampPitch();
                return true;
            case KeyEvent.KEY_NUM9:
                this.gizmo.setMaxNumOfReflections(this.gizmo.getMaxNumOfReflections() + 1);
                return true;
            case KeyEvent.KEY_NUM3:
                this.gizmo.setMaxNumOfReflections(this.gizmo.getMaxNumOfReflections() - 1);
                return true;
            default:
                return false;
        }
    }

    private isActivationKey(event: KeyEvent): boolean {
        for (const key of this.activationKeys) {
            if (event.keycode === key) {
                return true;
            }
        }
        return false;
    }

    private currentRay(): Ray {
        return new Ray(this.gizmo.getPosition(), this.gizmo.getDirection());
    }

    private applyRay(): void {
        this.updateInputGizmo();
        this.gizmo.setRay(new Ray(this.origin, this.direction()), 0.0);
    }

    private applyInputGizmoValues(): void {
        const values: number[] = this.getInputGizmo().getValuesWithEdits();

        this.origin = new Vector3Dd(values[0]!, values[1]!, values[2]!);
        this.yawInDegrees = values[3]!;
        this.pitchInDegrees = values[4]!;
        this.clampPitch();
    }

    private updateInputGizmo(): void {
        this.getInputGizmo().setValue(0, this.origin.x());
        this.getInputGizmo().setValue(1, this.origin.y());
        this.getInputGizmo().setValue(2, this.origin.z());
        this.getInputGizmo().setValue(3, this.yawInDegrees);
        this.getInputGizmo().setValue(4, this.pitchInDegrees);
    }

    private updateAnglesFromDirection(direction: Vector3Dd): void {
        const d: Vector3Dd = direction.length() > 1e-9 ? direction.normalized() : new Vector3Dd(0, 0, 1);

        this.yawInDegrees = JavaMath.toDegrees(Math.atan2(d.y(), d.x()));
        this.pitchInDegrees = JavaMath.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, d.z()))));
        this.clampPitch();
    }

    private direction(): Vector3Dd {
        const yaw: number = JavaMath.toRadians(this.yawInDegrees);
        const pitch: number = JavaMath.toRadians(this.pitchInDegrees);
        const cp: number = Math.cos(pitch);

        return new Vector3Dd(cp * Math.cos(yaw), cp * Math.sin(yaw), Math.sin(pitch));
    }

    private clampPitch(): void {
        if (this.pitchInDegrees > 89.0) {
            this.pitchInDegrees = 89.0;
        }
        if (this.pitchInDegrees < -89.0) {
            this.pitchInDegrees = -89.0;
        }
    }
}
