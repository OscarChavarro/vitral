import { AnimationEvent } from "./AnimationEvent.js";
import type { AnimationListener } from "./AnimationListener.js";

/**
Port of `vsdk.toolkit.animation.AnimationEventGenerator`.

The tick source of the animation package: twenty-four times a second it
computes the seconds elapsed since it started, writes them into a single
reused {@link AnimationEvent}, and dispatches that event to every registered
listener, in registration order.

One boundary is crossed, and it is the class's whole reason for existing in
Java. `AnimationEventGenerator implements Runnable`, and its `run()` is an
unbounded `while ( true )` loop with a `Thread.sleep(1000/fps)` in it; a caller
hands it to a `Thread` and lets it run for the life of the JVM. A browser has
one thread and a task that never yields freezes the page, so `run()` here arms
the host's own interval timer at the same period and returns, and a matching
{@link stop} cancels it — Java needs no such method because its thread is a
daemon that dies with the process, while a module inside a page is torn down
while the page lives on. What each tick does, and the order it does it in, is
Java's.

The event object is reused across ticks exactly as Java reuses it, and the
elapsed time is read before the wait rather than after, as Java reads it:
`e.setT(t)` precedes the sleep and `dispatch(e)` follows it, so the event a
listener sees carries the time of one period earlier.
*/
export class AnimationEventGenerator {
    private readonly fps: number;
    private readonly listeners: AnimationListener[];
    private timerId: ReturnType<typeof setInterval> | null = null;

    public constructor() {
        this.fps = 24;
        this.listeners = [];
    }

    public addAnimationListener(l: AnimationListener): void {
        this.listeners.push(l);
    }

    private dispatch(e: AnimationEvent): void {
        let i: number;

        for (i = 0; i < this.listeners.length; i++) {
            this.listeners[i]!.tick(e);
        }
    }

    private getRealTimeSeconds(): number {
        return Date.now() / 1000.0;
    }

    /**
    Java's `Runnable.run`, as a browser can run it: the loop body below is the
    body of Java's `while ( true )`, and the host timer plays the part of both
    the loop and its `Thread.sleep`.
    */
    public run(): void {
        if (this.timerId !== null) {
            return;
        }

        let t: number;
        const t0: number = this.getRealTimeSeconds();
        const e: AnimationEvent = new AnimationEvent();
        // Java's loop is `t = now - t0; e.setT(t); sleep; dispatch(e)`, so the
        // event a listener sees carries the time read one period earlier. The
        // loop is rotated around the wait here, which is what a timer callback
        // is: the first reading happens before the timer is armed, and each
        // callback dispatches the reading the previous iteration took before
        // taking the next one.
        t = this.getRealTimeSeconds() - t0;
        e.setT(t);
        this.timerId = setInterval(
            () => {
                this.dispatch(e);
                t = this.getRealTimeSeconds() - t0;
                e.setT(t);
            },
            Math.trunc(1000 / this.fps),
        );
    }

    /**
    No Java counterpart: its thread is a daemon and ends with the JVM. Here the
    generator outlives nothing, so whoever started it must be able to end it.
    */
    public stop(): void {
        if (this.timerId !== null) {
            clearInterval(this.timerId);
            this.timerId = null;
        }
    }
}
