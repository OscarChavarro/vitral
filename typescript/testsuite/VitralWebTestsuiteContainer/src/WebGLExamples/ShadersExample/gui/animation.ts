import type { ShadersModel } from '../model/shaders-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/gui/Animation.java`.
 *
 * One full turn of the sphere every eight seconds, advanced by the elapsed
 * wall-clock time rather than by a fixed step, so the speed does not depend on
 * how often the timer actually fires. The clamp at a quarter of a second is
 * Java's guard against a long stall turning into a jump.
 *
 * Java reads `System.nanoTime()`; `performance.now()` is the browser's
 * monotonic clock and the same reading in milliseconds, so the conversion
 * divides by a thousand where Java divides by a billion.
 */
export class Animation {
  static readonly FRAMES_PER_SECOND = 30;
  static readonly FRAME_DELAY_MILLIS = Math.trunc(1000 / Animation.FRAMES_PER_SECOND);

  private static readonly FULL_ROTATION_RADIANS = 2.0 * Math.PI;
  private static readonly ROTATION_PERIOD_SECONDS = 8.0;
  private static readonly ANGULAR_SPEED_RAD_PER_SECOND =
    Animation.FULL_ROTATION_RADIANS / Animation.ROTATION_PERIOD_SECONDS;

  private lastTickMillis = -1;

  reset(): void {
    this.lastTickMillis = -1;
  }

  tick(model: ShadersModel | null): void {
    if (model === null || !model.isAnimationEnabled()) {
      this.reset();
      return;
    }

    const now: number = performance.now();
    if (this.lastTickMillis < 0) {
      this.lastTickMillis = now;
      return;
    }

    let elapsedSeconds: number = (now - this.lastTickMillis) / 1000.0;
    this.lastTickMillis = now;

    if (elapsedSeconds < 0.0) {
      return;
    }
    if (elapsedSeconds > 0.25) {
      elapsedSeconds = 0.25;
    }

    model.advanceSphereRotationRadians(Animation.ANGULAR_SPEED_RAD_PER_SECOND * elapsedSeconds);
  }
}
