import type { MeshModel } from '../model/mesh-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MeshExample/src/animation/AnimationController.java`.
 *
 * Java starts a daemon `Thread` running a `vsdk.toolkit.animation.
 * AnimationEventGenerator` and, on every tick at least one second after the
 * last one, updates the ray gizmo and repaints the canvas. That animation
 * package has no TypeScript port yet, and a browser page has no thread to give
 * it: the tick source here is the page's own timer, at the same one-second
 * period the Java listener filters down to, and the callbacks it fires are the
 * Java ones.
 */
export class AnimationController {
  private static readonly TICK_PERIOD_MILLISECONDS = 1000;

  private timerId: ReturnType<typeof setInterval> | null = null;

  start(model: MeshModel, repaintCallback: () => void): void {
    if (this.timerId !== null) {
      return;
    }

    this.timerId = setInterval(() => {
      model.getRayGizmo().update();
      repaintCallback();
    }, AnimationController.TICK_PERIOD_MILLISECONDS);
  }

  /**
   * Java's thread is a daemon and dies with the JVM. A module inside the
   * container is torn down while the page lives on, so it stops its own timer.
   */
  stop(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
