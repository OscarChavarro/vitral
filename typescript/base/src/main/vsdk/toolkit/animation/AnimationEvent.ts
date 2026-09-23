/**
Port of `vsdk.toolkit.animation.AnimationEvent`.

The single value an animation tick carries: the seconds elapsed since the
generator that dispatches it started. Java's `double` field is a `number` here,
and the accessors keep their Java names.
*/
export class AnimationEvent {
    private time = 0.0;

    /**
    @return the time of the event
    */
    public getTime(): number {
        return this.time;
    }

    /**
    @param value the time of the event
    */
    public setTime(value: number): void {
        this.time = value;
    }
}
