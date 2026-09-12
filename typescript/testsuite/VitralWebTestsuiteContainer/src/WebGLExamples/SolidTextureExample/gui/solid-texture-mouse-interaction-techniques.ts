import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/gui/SolidTextureMouseInteractionTechniques.java`.
 *
 * Same shape as `MeshExample`'s: Java forwards a `vsdk.toolkit.gui.MouseEvent`
 * to a `CameraController`, and the browser examples forward the DOM pointer
 * event to the shared `ExampleCameraInteraction`, keeping the
 * "answer whether the view needs a repaint" contract. Java's `pressed` /
 * `released` / `clicked` / `moved` / `dragged` split is the AWT one, and the
 * pointer-event model replaces it with down / move / up plus capture, so the
 * three techniques with no pointer-event counterpart are not carried over.
 */
export class SolidTextureMouseInteractionTechniques {
  constructor(private readonly cameraController: ExampleCameraInteraction) {}

  processPointerDownEvent(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return this.cameraController.processPointerDown(event, canvas);
  }

  processPointerUpEvent(): boolean {
    return this.cameraController.processPointerUp();
  }

  processPointerMoveEvent(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return this.cameraController.processPointerMove(event, canvas);
  }

  processMouseWheelEvent(event: WheelEvent, canvas: HTMLCanvasElement): boolean {
    console.log('.');
    return this.cameraController.processWheel(event, canvas);
  }
}
