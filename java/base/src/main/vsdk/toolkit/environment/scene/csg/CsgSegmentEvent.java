package vsdk.toolkit.environment.scene.csg;

/**
One boundary event of a ray's alternating in/out classification: the ray
parameter, whether the ray enters the solid there, and the caller's event
(a renderer's hit record, a crossing reference). [ROTH1982] section 2.2, the
t[i]/S[i] pairs of RAYCAST. The payload is copied, never interpreted: in Java
the payload reference is kept, so callers pass values they do not mutate.
*/
public class CsgSegmentEvent<Payload> {
    private final double t;
    private final boolean entering;
    private final Payload payload;

    public CsgSegmentEvent() { t = 0.0; entering = false; payload = null; }

    public CsgSegmentEvent(double t, boolean entering, Payload payload)
    {
        this.t = t;
        this.entering = entering;
        this.payload = payload;
    }

    public double getT() { return t; }
    public boolean isEntering() { return entering; }
    public Payload getPayload() { return payload; }
}
