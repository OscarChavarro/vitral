import type { Camera } from "../../environment/camera/Camera.js";
import type { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import type { CameraController } from "../CameraController.js";
import { CameraControllerAquynza } from "../CameraControllerAquynza.js";
import type { KeyEvent } from "../KeyEvent.js";
import type { MouseEvent } from "../MouseEvent.js";
import { RendererConfigurationController } from "../RendererConfigurationController.js";
import type { InputGizmo } from "../gizmo/InputGizmo.js";
import { RotateGizmo } from "../gizmo/RotateGizmo.js";
import { RotateGizmoInteractionTechnique } from "../gizmo/RotateGizmoInteractionTechnique.js";
import { ScaleGizmo } from "../gizmo/ScaleGizmo.js";
import { ScaleGizmoInteractionTechnique } from "../gizmo/ScaleGizmoInteractionTechnique.js";
import { TranslateGizmo } from "../gizmo/TranslateGizmo.js";
import { type CursorWarp, TranslateGizmoInteractionTechnique } from "../gizmo/TranslateGizmoInteractionTechnique.js";
import type { Viewport } from "./Viewport.js";

export class ViewportInteractionTechniques {
    private readonly cameraController: CameraController;
    private readonly qualityController: RendererConfigurationController;
    private readonly translationGizmo: TranslateGizmo;
    private readonly translationTechnique: TranslateGizmoInteractionTechnique;
    private readonly rotateGizmo: RotateGizmo;
    private readonly rotationTechnique: RotateGizmoInteractionTechnique;
    private readonly scaleGizmo: ScaleGizmo;
    private readonly scaleTechnique: ScaleGizmoInteractionTechnique;

    public constructor(camera: Camera, rendererConfiguration: RendererConfiguration) {
        this.cameraController = new CameraControllerAquynza(camera);
        this.qualityController = new RendererConfigurationController(rendererConfiguration);
        this.translationGizmo = new TranslateGizmo(camera);
        this.translationTechnique = new TranslateGizmoInteractionTechnique(this.translationGizmo);
        this.rotateGizmo = new RotateGizmo(camera);
        this.rotationTechnique = new RotateGizmoInteractionTechnique(this.rotateGizmo);
        this.scaleGizmo = new ScaleGizmo(camera);
        this.scaleTechnique = new ScaleGizmoInteractionTechnique(this.scaleGizmo);
    }

    public getCameraController(): CameraController {
        return this.cameraController;
    }

    public getQualityController(): RendererConfigurationController {
        return this.qualityController;
    }

    public getTranslationGizmo(): TranslateGizmo {
        return this.translationGizmo;
    }

    public getTranslationTechnique(): TranslateGizmoInteractionTechnique {
        return this.translationTechnique;
    }

    public getRotateGizmo(): RotateGizmo {
        return this.rotateGizmo;
    }

    public getRotationTechnique(): RotateGizmoInteractionTechnique {
        return this.rotationTechnique;
    }

    public getScaleGizmo(): ScaleGizmo {
        return this.scaleGizmo;
    }

    public setCamera(camera: Camera): void {
        this.cameraController.setCamera(camera);
    }

    public setRendererConfiguration(rendererConfiguration: RendererConfiguration): void {
        this.qualityController.setRendererConfiguration(rendererConfiguration);
    }

    public processCameraKeyPressedEvent(event: KeyEvent): boolean {
        return this.cameraController.processKeyPressedEvent(event);
    }

    public processCameraKeyReleasedEvent(event: KeyEvent): boolean {
        return this.cameraController.processKeyReleasedEvent(event);
    }

    public processCameraMousePressedEvent(event: MouseEvent): boolean {
        return this.cameraController.processMousePressedEvent(event);
    }

    public processCameraMouseReleasedEvent(event: MouseEvent): boolean {
        return this.cameraController.processMouseReleasedEvent(event);
    }

    public processCameraMouseClickedEvent(event: MouseEvent): boolean {
        return this.cameraController.processMouseClickedEvent(event);
    }

    public processCameraMouseMovedEvent(event: MouseEvent): boolean {
        return this.cameraController.processMouseMovedEvent(event);
    }

    public processCameraMouseDraggedEvent(event: MouseEvent): boolean {
        return this.cameraController.processMouseDraggedEvent(event);
    }

    public processCameraMouseWheelEvent(event: MouseEvent): boolean {
        return this.cameraController.processMouseWheelEvent(event);
    }

    public processQualityKeyPressedEvent(event: KeyEvent): boolean {
        return this.qualityController.processKeyPressedEvent(event);
    }

    /**
    @return the input gizmo that shows and edits the coordinates of the
    translation gizmo
    */
    public getTranslationInputGizmo(): InputGizmo {
        return this.translationGizmo.getInputGizmo();
    }

    /**
    @param event key press
    @return true if the input gizmo of the translation gizmo uses the key
    */
    public isTranslationInputGizmoKey(event: KeyEvent): boolean {
        return this.translationTechnique.isInputGizmoKey(event);
    }

    public processTranslationKeyPressedEvent(event: KeyEvent): boolean {
        return this.translationTechnique.processKeyPressedEvent(event);
    }

    /**
    Java's two `processTranslationMousePressedEvent` overloads. With a
    viewport, it processes the press of a mouse button over it, starting a
    translation gesture confined to it (see `getTranslationDragViewport`).
    @param event event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processTranslationMousePressedEvent(event: MouseEvent, viewport?: Viewport | null): boolean {
        return this.translationTechnique.processMousePressedEvent(event, viewport);
    }

    /**
    @return the viewport where the translation gesture in course started, or
    null if there is none
    */
    public getTranslationDragViewport(): Viewport | null {
        return this.translationTechnique.getDragViewport();
    }

    /**
    @param enabled true if the caller is able to place the cursor when the
    translation technique requests it
    */
    public setTranslationCursorWrapEnabled(enabled: boolean): void {
        this.translationTechnique.setCursorWrapEnabled(enabled);
    }

    /**
    @return the pending request to place the cursor while dragging, or null
    */
    public consumeTranslationCursorWarp(): CursorWarp | null {
        return this.translationTechnique.consumeCursorWarp();
    }

    public processTranslationMouseReleasedEvent(event: MouseEvent): boolean {
        return this.translationTechnique.processMouseReleasedEvent(event);
    }

    public processTranslationMouseClickedEvent(event: MouseEvent): boolean {
        return this.translationTechnique.processMouseClickedEvent(event);
    }

    public processTranslationMouseMovedEvent(event: MouseEvent): boolean {
        return this.translationTechnique.processMouseMovedEvent(event);
    }

    public processTranslationMouseDraggedEvent(event: MouseEvent): boolean {
        return this.translationTechnique.processMouseDraggedEvent(event);
    }

    /**
    @return the input gizmo that shows and edits the angles of the rotation
    gizmo
    */
    public getRotationInputGizmo(): InputGizmo {
        return this.rotateGizmo.getInputGizmo();
    }

    /**
    @param event key press
    @return true if the input gizmo of the rotation gizmo uses the key
    */
    public isRotationInputGizmoKey(event: KeyEvent): boolean {
        return this.rotationTechnique.isInputGizmoKey(event);
    }

    public processRotateKeyPressedEvent(event: KeyEvent): boolean {
        return this.rotationTechnique.processKeyPressedEvent(event);
    }

    /**
    Java's two `processRotationMousePressedEvent` overloads. With a viewport,
    it processes the press of a mouse button over it, starting a rotation
    gesture confined to it if the pointer is over a ring (see
    `getRotationDragViewport`).
    @param event event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processRotationMousePressedEvent(event: MouseEvent, viewport?: Viewport | null): boolean {
        return this.rotationTechnique.processMousePressedEvent(event, viewport);
    }

    /**
    @return the viewport where the rotation gesture in course started, or null
    if there is none
    */
    public getRotationDragViewport(): Viewport | null {
        return this.rotationTechnique.getDragViewport();
    }

    public processRotationMouseReleasedEvent(event: MouseEvent): boolean {
        return this.rotationTechnique.processMouseReleasedEvent(event);
    }

    public processRotationMouseDraggedEvent(event: MouseEvent): boolean {
        return this.rotationTechnique.processMouseDraggedEvent(event);
    }

    public processRotationMouseClickedEvent(event: MouseEvent): boolean {
        return this.rotationTechnique.processMouseClickedEvent(event);
    }

    public processRotationMouseMovedEvent(event: MouseEvent): boolean {
        return this.rotationTechnique.processMouseMovedEvent(event);
    }

    public processScaleKeyPressedEvent(event: KeyEvent): boolean {
        return this.scaleTechnique.processKeyPressedEvent(event);
    }

    /**
    @return the input gizmo that shows and edits the scale factors of the
    scale gizmo
    */
    public getScaleInputGizmo(): InputGizmo {
        return this.scaleGizmo.getInputGizmo();
    }

    /**
    @param event key press
    @return true if the input gizmo of the scale gizmo uses the key
    */
    public isScaleInputGizmoKey(event: KeyEvent): boolean {
        return this.scaleTechnique.isInputGizmoKey(event);
    }

    /**
    @return the interaction technique that hovers, selects and drags the
    handles of the scale gizmo
    */
    public getScaleTechnique(): ScaleGizmoInteractionTechnique {
        return this.scaleTechnique;
    }

    /**
    Java's two `processScaleMousePressedEvent` overloads. With a viewport, it
    processes the press of a mouse button over it, starting a scale gesture
    confined to it if the pointer is over a handle (see
    `getScaleDragViewport`).
    @param event event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processScaleMousePressedEvent(event: MouseEvent, viewport?: Viewport | null): boolean {
        return this.scaleTechnique.processMousePressedEvent(event, viewport);
    }

    /**
    @return the viewport where the scale gesture in course started, or null
    if there is none
    */
    public getScaleDragViewport(): Viewport | null {
        return this.scaleTechnique.getDragViewport();
    }

    public processScaleMouseReleasedEvent(event: MouseEvent): boolean {
        return this.scaleTechnique.processMouseReleasedEvent(event);
    }

    public processScaleMouseDraggedEvent(event: MouseEvent): boolean {
        return this.scaleTechnique.processMouseDraggedEvent(event);
    }

    public processScaleMouseClickedEvent(event: MouseEvent): boolean {
        return this.scaleTechnique.processMouseClickedEvent(event);
    }

    public processScaleMouseMovedEvent(event: MouseEvent): boolean {
        return this.scaleTechnique.processMouseMovedEvent(event);
    }
}
