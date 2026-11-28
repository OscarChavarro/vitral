import { MouseEvent as VitralMouseEvent, type CameraController } from '@vitral/base';
import { WebSystem, type WebKeyEvent } from '@vitral/webgl';
import { toVitralKeyEvent } from './web-key-event-conversion';

/**
 * Feeds browser events to a `vsdk.toolkit.gui.CameraController`, as the Java
 * examples under `java/testsuite/Jogl4Examples` do through
 * `AwtSystem.awt2vsdkEvent`.
 *
 * Each example constructs the controller its Java program constructs
 * (`CameraControllerOrbiter`, or `CameraControllerAquynza` for `CameraExample`
 * and `ImageExample`) and hands it here; this class only translates events.
 * The DOM has no separate drag event, so a pointer move with a button held
 * goes to `processMouseDraggedEvent` and any other move to
 * `processMouseMovedEvent`, the split AWT makes. Each handler answers whether
 * the view needs to be repainted, as the controller does.
 */
export class ExampleCameraInteraction {
  constructor(private readonly controller: CameraController) {}

  /**
   * @return the controller the events go to, for the Java code that casts it
   * (`instanceof CameraControllerOrbiter`) to move its point of interest
   */
  getController(): CameraController {
    return this.controller;
  }

  processPointerDown(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return this.controller.processMousePressedEvent(WebSystem.toVitralMouseEvent(event, canvas));
  }

  processPointerMove(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    const vitralEvent: VitralMouseEvent = WebSystem.toVitralMouseEvent(event, canvas);
    if (event.buttons !== 0) {
      return this.controller.processMouseDraggedEvent(vitralEvent);
    }
    return this.controller.processMouseMovedEvent(vitralEvent);
  }

  processPointerUp(event?: PointerEvent, canvas?: HTMLCanvasElement): boolean {
    const vitralEvent: VitralMouseEvent =
      event !== undefined ? WebSystem.toVitralMouseEvent(event, canvas) : new VitralMouseEvent();
    return this.controller.processMouseReleasedEvent(vitralEvent);
  }

  processWheel(event: WheelEvent, canvas: HTMLCanvasElement): boolean {
    return this.controller.processMouseWheelEvent(WebSystem.toVitralWheelEvent(event, canvas));
  }

  processKeyPressed(webEvent: WebKeyEvent): boolean {
    return this.controller.processKeyPressedEvent(toVitralKeyEvent(webEvent));
  }

  processKeyReleased(webEvent: WebKeyEvent): boolean {
    return this.controller.processKeyReleasedEvent(toVitralKeyEvent(webEvent));
  }
}
