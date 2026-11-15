import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgRayEventReference } from "./CsgRayEventReference.js";
import { CsgRayWorkspace } from "./CsgRayWorkspace.js";

// Caller-owned scratch storage for certified point containment and the POV
// heuristic's event selection. It is reused across queries by one worker and
// must not be shared between concurrent queries; its contents are meaningful
// only during a query. The lists are used as stacks by the recursive queries.
export class CsgPointWorkspace {
    public readonly path: ArrayList<CsgExpression>;
    public readonly results: ArrayList<GeometryContainment>;
    public readonly events: ArrayList<CsgRayEventReference>;
    // Scratch storage of the leaf ray queries.
    public readonly ray: CsgRayWorkspace;

    public constructor() {
        this.path = new ArrayList<CsgExpression>(8);
        this.results = new ArrayList<GeometryContainment>(16);
        this.events = new ArrayList<CsgRayEventReference>(16);
        this.ray = new CsgRayWorkspace();
    }

    public clear(): void {
        this.path.clear();
        this.results.clear();
        this.events.clear();
    }
}
