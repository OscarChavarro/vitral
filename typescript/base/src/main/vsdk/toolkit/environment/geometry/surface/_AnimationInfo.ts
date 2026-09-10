/** MD2 animation range metadata, kept as a mutable data record like Java. */
export class _AnimationInfo {
    public name = "";
    public start = 0;
    public end = 0;
    public constructor(other?: _AnimationInfo) {
        if (other !== undefined) {
            this.name = other.name;
            this.start = other.start;
            this.end = other.end;
        }
    }
}
