import { ArrayList } from "../../../../../java/util/ArrayList.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgRayEventCursor } from "./CsgRayEventCursor.js";
import type { CsgRayEventReference } from "./CsgRayEventReference.js";

// Caller-owned scratch storage for CSG ray queries. It is reused across
// queries by one worker and must not be shared between concurrent queries.
// Its contents are meaningful only during a query.
export class CsgRayWorkspace {
    public readonly path: ArrayList<CsgExpression>;
    public readonly cursors: ArrayList<CsgRayEventCursor>;
    public readonly group: ArrayList<CsgRayEventReference>;
    public readonly states: ArrayList<boolean>;
    public readonly previous: ArrayList<boolean>;
    public readonly trial: ArrayList<boolean>;

    public constructor() {
        this.path = new ArrayList<CsgExpression>(8);
        this.cursors = new ArrayList<CsgRayEventCursor>(8);
        this.group = new ArrayList<CsgRayEventReference>(8);
        this.states = new ArrayList<boolean>(8);
        this.previous = new ArrayList<boolean>(8);
        this.trial = new ArrayList<boolean>(8);
    }
}
