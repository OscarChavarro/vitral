/** MD2 animation range metadata (Java's package private `_AnimationInfo`). */
export class _AnimationInfo {
    public name: string | null = null;
    //int number; // Not the number of frames but the order.
    /** Java `short` */
    public start = 0;
    /** Java `short` */
    public end = 0;

    public constructor();
    public constructor(ai: _AnimationInfo);
    public constructor(ai?: _AnimationInfo) {
        if (ai !== undefined) {
            this.name = ai.name;
            this.start = ai.start;
            this.end = ai.end;
        }
    }
}
