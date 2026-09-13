import type { MeshModel } from '../model/mesh-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MeshExample/src/animation/AnimationController.java`.
 *
 * Java starts a daemon `Thread` running a `vsdk.toolkit.animation.
 * AnimationEventGenerator` and, on every tick at least one second after the
 * last one, updates the ray gizmo and repaints the canvas. That animation
 * package is now ported (Phase 42), but a browser page still has no thread to
 * give it, and what this Java listener drives is repaints and gizmo aging
 * rather than a mesh, so the tick source here stays the page's own timer, at
 * the same one-second period the Java listener filters down to, and the
 * callbacks it fires are the Java ones.
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
