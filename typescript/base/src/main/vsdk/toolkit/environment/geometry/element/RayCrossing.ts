import type { Geometry } from "../Geometry.js";
import type { SimpleBody } from "../../scene/SimpleBody.js";
import type { Material } from "../../material/Material.js";
import type { RayCrossingKind } from "./RayCrossingKind.js";
export class RayCrossing {
    public body: SimpleBody | null = null;
    public material: Material | null = null;
    constructor(public t: number, public geometry: Geometry, public kind: RayCrossingKind) {}
}
