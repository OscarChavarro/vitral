package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;

/**
Caller-owned scratch storage for certified point containment and the POV
heuristic's event selection. It is reused across queries by one worker and
must not be shared between concurrent queries; its contents are meaningful
only during a query. The lists are used as stacks by the recursive queries.
*/
public class CsgPointWorkspace {
    public final ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();

    public final ArrayList<GeometryContainment> results = new ArrayList<GeometryContainment>();

    public final ArrayList<CsgRayEventReference> events = new ArrayList<CsgRayEventReference>();

    // Scratch storage of the leaf ray queries.
    public final CsgRayWorkspace ray = new CsgRayWorkspace();

    public CsgPointWorkspace() {
        }

    public void clear() { path.clear(); results.clear(); events.clear(); }
}

