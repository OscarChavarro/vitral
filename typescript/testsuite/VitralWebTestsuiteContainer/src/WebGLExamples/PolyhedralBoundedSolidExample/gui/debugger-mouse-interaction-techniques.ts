import type { DebuggerModel } from '../models/debugger-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/gui/DebuggerMouseInteractionTechniques.java`.
 *
 * Every technique forwards to the model's camera controller and answers
 * whether the view has to be repainted, as Java's do; the wheel handler prints
 * its `.` first, as Java's does. The controller is the container's stand-in
 * for `CameraControllerOrbiter`.
 *
 * Java's `processMouseEntered` asks the canvas for the keyboard focus and its
 * `processMouseExited` does nothing; a browser element takes focus on a
 * pointer press, which is where the component asks for it. Java's clicked and
 * moved handlers forward to controller handlers the orbiter answers `false`
 * to, and a page delivers pointer `down`, `move` and `up`, so those collapse
 * into the three below — the reading the `PolygonClippingExample` module
 * records.
 */
export class DebuggerMouseInteractionTechniques {
  processMousePressed(model: DebuggerModel, e: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return model.getCameraController().processPointerDown(e, canvas);
  }

  processMouseReleased(model: DebuggerModel): boolean {
    return model.getCameraController().processPointerUp();
  }

  processMouseDragged(model: DebuggerModel, e: PointerEvent, canvas: HTMLCanvasElement): boolean {
    return model.getCameraController().processPointerMove(e, canvas);
  }

  processMouseWheelMoved(model: DebuggerModel, e: WheelEvent, canvas: HTMLCanvasElement): boolean {
    console.log('.');
    return model.getCameraController().processWheel(e, canvas);
  }
}
