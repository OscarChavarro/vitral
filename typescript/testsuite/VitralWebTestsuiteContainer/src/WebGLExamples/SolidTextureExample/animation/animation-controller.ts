import type { SolidTextureModel } from '../model/solid-texture-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/animation/AnimationController.java`.
 *
 * Java starts a daemon `Thread` running a `vsdk.toolkit.animation.
 * AnimationEventGenerator`, which dispatches a tick carrying the seconds since
 * the generator started, twenty-four times a second. The listener does two
 * separate things with it: once a second it ages both gizmos, so that one with
 * no fresh data hides itself, and on every tick it advances the scene rotation
 * by the elapsed time when the animation is running. It repaints when the
 * rotation moved or when a gizmo was aged, and not otherwise.
 *
 * That animation package is now ported (Phase 42), but a browser page still has
 * no thread to give it, and what this Java listener drives is the scene
 * rotation and gizmo aging rather than a mesh, so the tick source here stays
 * the page's own timer at the same twenty-four ticks a second, and the listener
 * body is the Java one, its `e.getT()` becoming the seconds since
 * {@link start} was called. A
 * `requestAnimationFrame` loop would have been the other candidate, and was not
 * chosen because it ties the tick rate to the display refresh, whereas the
 * gizmo aging this tick also performs is a wall-clock concern.
 */
export class AnimationController {
  private static readonly FRAMES_PER_SECOND = 24;
  private static readonly FULL_ROTATION_RADIANS = 2.0 * Math.PI;
  private static readonly ROTATION_PERIOD_SECONDS = 8.0;
  private static readonly ANGULAR_SPEED_RAD_PER_SECOND =
    AnimationController.FULL_ROTATION_RADIANS / AnimationController.ROTATION_PERIOD_SECONDS;
  private static readonly MAX_ELAPSED_SECONDS = 0.25;

  private timerId: ReturnType<typeof setInterval> | null = null;

  start(model: SolidTextureModel, repaintCallback: () => void): void {
    if (this.timerId !== null) {
      return;
    }

    const startedAtMilliseconds: number = Date.now();
    let lastTickT = 0.0;
    let lastRayGizmoUpdateT = 0.0;

    this.timerId = setInterval(
      () => {
        const t: number = (Date.now() - startedAtMilliseconds) / 1000.0;
        let elapsedSeconds: number = t - lastTickT;
        lastTickT = t;

        let gizmoUpdated = false;
        if (t - lastRayGizmoUpdateT >= 1.0) {
          model.getRayGizmo().update();
          model.getInfinitePlaneGizmo().update();
          lastRayGizmoUpdateT = t;
          gizmoUpdated = true;
        }

        if (model.isAnimationEnabled()) {
          if (elapsedSeconds < 0.0) {
            elapsedSeconds = 0.0;
          }
          if (elapsedSeconds > AnimationController.MAX_ELAPSED_SECONDS) {
            elapsedSeconds = AnimationController.MAX_ELAPSED_SECONDS;
          }
          model.advanceObjectRotationRadians(
            AnimationController.ANGULAR_SPEED_RAD_PER_SECOND * elapsedSeconds,
          );
          repaintCallback();
        } else if (gizmoUpdated) {
          repaintCallback();
        }
      },
      Math.trunc(1000 / AnimationController.FRAMES_PER_SECOND),
    );
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
