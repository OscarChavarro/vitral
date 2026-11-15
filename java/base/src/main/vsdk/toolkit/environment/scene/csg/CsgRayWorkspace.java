package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
Caller-owned scratch storage for CSG ray queries. It is reused across
queries by one worker and must not be shared between concurrent queries.
Its contents are meaningful only during a query.
*/
public class CsgRayWorkspace {
    public final ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();

    public final ArrayList<CsgRayEventCursor> cursors = new ArrayList<CsgRayEventCursor>();

    public final ArrayList<CsgRayEventReference> group = new ArrayList<CsgRayEventReference>();

    public final ArrayList<Boolean> states = new ArrayList<Boolean>();

    public final ArrayList<Boolean> previous = new ArrayList<Boolean>();

    public final ArrayList<Boolean> trial = new ArrayList<Boolean>();

    public CsgRayWorkspace() {
        }
}

