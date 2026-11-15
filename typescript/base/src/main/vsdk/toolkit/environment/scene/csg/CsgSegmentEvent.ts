/**
One boundary event of a ray's alternating in/out classification: the ray
parameter, whether the ray enters the solid there, and the caller's event
(a renderer's hit record, a crossing reference). [ROTH1982] section 2.2, the
t[i]/S[i] pairs of RAYCAST. The payload is copied, never interpreted; in this
port the payload reference is kept, so callers pass values they do not mutate.
*/
export class CsgSegmentEvent<Payload> {
    private readonly t: number;
    private readonly entering: boolean;
    private readonly payload: Payload | null;

    public constructor();
    public constructor(t: number, entering: boolean, payload: Payload);
    public constructor(t?: number, entering?: boolean, payload?: Payload) {
        this.t = t ?? 0.0;
        this.entering = entering ?? false;
        this.payload = payload === undefined ? null : payload;
    }

    public getT(): number {
        return this.t;
    }

    public isEntering(): boolean {
        return this.entering;
    }

    public getPayload(): Payload {
        return this.payload as Payload;
    }
}
