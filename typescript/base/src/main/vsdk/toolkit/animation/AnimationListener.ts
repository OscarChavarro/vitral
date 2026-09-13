import type { AnimationEvent } from "./AnimationEvent.js";

/**
Port of `vsdk.toolkit.animation.AnimationListener`.

Java declares `tick` abstract and gives `isPaused` / `setPaused` bodies that do
nothing, so a subclass that ignores pausing inherits a working pair. The same
split is kept here: `tick` is abstract, the other two carry Java's bodies.
*/
export abstract class AnimationListener {
    public abstract tick(e: AnimationEvent): void;

    public isPaused(): boolean {
        return false;
    }

    public setPaused(_paused: boolean): void {}
}
