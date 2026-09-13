import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MD2Example/src/gui/MouseInteractionTechniques.java`.
 *
 * Every Java technique answers false and only the wheel one has a body, a
 * `System.out.println(".")`; `Md2MeshExample` then hands the same event to the
 * `CameraController`, which is what orbits the view. The pointer-event model
 * replaces AWT's pressed / released / clicked / moved / dragged split with
 * down / move / up plus capture, so the three techniques with no pointer-event
 * counterpart are not carried over, as in the other ported modules.
 */
export class MouseInteractionTechniques {
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
