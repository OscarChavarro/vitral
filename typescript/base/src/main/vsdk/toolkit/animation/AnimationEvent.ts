/**
Port of `vsdk.toolkit.animation.AnimationEvent`.

The single value an animation tick carries: the seconds elapsed since the
generator that dispatches it started. Java's `double` field is a `number` here,
and the accessors keep their Java names.
*/
export class AnimationEvent {
    private t = 0.0;

    /**
    @return the t
    */
    public getT(): number {
        return this.t;
    }

    /**
    @param t the t to set
    */
    public setT(t: number): void {
        this.t = t;
    }
}
