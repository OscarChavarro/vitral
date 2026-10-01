import type { Camera } from "../environment/camera/Camera.js";
import { Controller } from "./Controller.js";
import type { KeyEvent } from "./KeyEvent.js";
import type { MouseEvent } from "./MouseEvent.js";

export abstract class CameraController extends Controller {
    public abstract processKeyPressedEvent(keyEvent: KeyEvent): boolean;

    public abstract processKeyReleasedEvent(keyEvent: KeyEvent): boolean;

    public abstract processMousePressedEvent(e: MouseEvent): boolean;

    public abstract processMouseReleasedEvent(e: MouseEvent): boolean;

    public abstract processMouseClickedEvent(e: MouseEvent): boolean;

    public abstract processMouseMovedEvent(e: MouseEvent): boolean;

    public abstract processMouseDraggedEvent(e: MouseEvent): boolean;

    public abstract processMouseWheelEvent(e: MouseEvent): boolean;

    public abstract getCamera(): Camera;
    public abstract setCamera(camera: Camera): void;
    public abstract setDeltaMovement(factor: number): void;

    public tick(_inCurrentTime: number): void {}
}
