import { AnimationEventGenerator, AnimationListener, type AnimationEvent } from '@vitral/base';
import type { DebuggerModel } from '../models/debugger-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/animation/SolidAnimationController.java`.
 *
 * One `AnimationEventGenerator` whose listener, on every tick while the
 * model's solid animation is on, turns the solid one degree about Z and asks
 * the canvas to repaint. Java hands the generator to a daemon `Thread`; the
 * ported generator's `run()` arms the host timer instead, as its own record
 * explains, which is why this class also has a `stop`, called when the module
 * is torn down, where Java's thread simply dies with the process.
 *
 * Java's `panel.repaint()` is the component's repaint request, passed in.
 */
export class SolidAnimationController {
  private static readonly ROTATION_STEP_DEGREES = 1.0;

  private animator: AnimationEventGenerator | null = null;
  private started = false;

  start(model: DebuggerModel, repaint: () => void): void {
    if (this.started) {
      return;
    }

    this.animator = new AnimationEventGenerator();
    this.animator.addAnimationListener(
      new (class extends AnimationListener {
        override tick(_e: AnimationEvent): void {
          if (!model.isSolidAnimationEnabled()) {
            return;
          }
          model.rotateSolidAroundZDegrees(SolidAnimationController.ROTATION_STEP_DEGREES);
          repaint();
        }
      })(),
    );

    this.animator.run();
    this.started = true;
  }

  stop(): void {
    this.animator?.stop();
    this.animator = null;
    this.started = false;
  }

  toggleAnimation(model: DebuggerModel): void {
    model.toggleSolidAnimationEnabled();
  }
}
