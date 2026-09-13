import {
  AnimationEventGenerator,
  AnimationListener,
  Md2AnimationListener,
  type AnimationEvent,
  type Md2Mesh,
} from '@vitral/base';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MD2Example/src/animation/DebuggerAnimationController.java`.
 *
 * Java builds an `AnimationEventGenerator`, registers a `Md2AnimationListener`
 * on the mesh and an anonymous listener that repaints the canvas, then hands
 * the generator to a `Thread` and starts it. All four of those classes are now
 * ported (`vsdk.toolkit.animation`), so this controller is Java's, with one
 * substitution: `new Thread(animator).start()` becomes `animator.run()`, which
 * arms the host timer rather than blocking a thread — see
 * `AnimationEventGenerator` for why. The repainter listener is Java's anonymous
 * subclass, named here because TypeScript has no anonymous class expression
 * that can extend an abstract class as tersely.
 *
 * The `stop` method has no Java counterpart: its thread is a daemon and dies
 * with the JVM, while a module inside the container is torn down while the page
 * lives on.
 */
export class DebuggerAnimationController {
  private animator: AnimationEventGenerator | null = null;

  start(md2Mesh: Md2Mesh, repaintCallback: () => void): void {
    if (this.animator !== null) {
      return;
    }

    const animator = new AnimationEventGenerator();
    const md2AniListener = new Md2AnimationListener(md2Mesh);
    animator.addAnimationListener(md2AniListener);

    const repainterListener: AnimationListener = new RepainterAnimationListener(repaintCallback);
    animator.addAnimationListener(repainterListener);

    animator.run();
    this.animator = animator;
  }

  stop(): void {
    this.animator?.stop();
    this.animator = null;
  }
}

/**
 * Java's anonymous `AnimationListener` whose `tick` calls `panel.repaint()`.
 */
class RepainterAnimationListener extends AnimationListener {
  constructor(private readonly repaintCallback: () => void) {
    super();
  }

  override tick(_e: AnimationEvent): void {
    this.repaintCallback();
  }
}
