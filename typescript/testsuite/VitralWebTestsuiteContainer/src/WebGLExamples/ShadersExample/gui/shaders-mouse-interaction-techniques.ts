import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/gui/ShadersMouseInteractionTechniques.java`.
 *
 * Every technique forwards to the camera controller and answers whether the
 * view has to be repainted, exactly as Java's do. Java's controller is a
 * `CameraControllerOrbiter`; until Phase 38 ports that family the container's
 * shared `ExampleCameraInteraction` stands in, with the same contract.
 *
 * Java's `processMouseEntered` asks the canvas for the keyboard focus and its
 * `processMouseExited` does nothing. A browser element takes focus on a
 * pointer press, which is where the component asks for it, so neither has a
 * counterpart here.
 */
export class ShadersMouseInteractionTechniques {
  constructor(private readonly cameraController: ExampleCameraInteraction) {}

  processPointerDownEvent(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return this.cameraController.processPointerDown(event, canvas);
  }

  processPointerMoveEvent(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return this.cameraController.processPointerMove(event, canvas);
  }

  processPointerUpEvent(): boolean {
    return this.cameraController.processPointerUp();
  }

  processMouseWheelEvent(event: WheelEvent, canvas: HTMLCanvasElement): boolean {
    return this.cameraController.processWheel(event, canvas);
  }
}
