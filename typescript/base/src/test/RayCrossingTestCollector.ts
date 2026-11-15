// Port of cpp/base/src/test/RayCrossingTestCollector.{h,cpp}
import { ArrayList } from "java/util/ArrayList.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import type { RayCrossing } from "vsdk/toolkit/environment/geometry/element/RayCrossing.js";
import type { RayCrossingKind } from "vsdk/toolkit/environment/geometry/element/RayCrossingKind.js";
import type { RayCrossingSink } from "vsdk/toolkit/environment/geometry/element/RayCrossingSink.js";

export class RayCrossingTestCollector implements RayCrossingSink {
    public readonly roots = new ArrayList<number>();
    public readonly kinds = new ArrayList<RayCrossingKind>();
    public readonly geometries = new ArrayList<Geometry | null>();
    public readonly surfaceIndices = new ArrayList<number>();
    public readonly instanceCounts = new ArrayList<number>();

    public crossing(event: RayCrossing): void {
        this.roots.add(event.t);
        this.kinds.add(event.kind);
        this.geometries.add(event.geometry);
        this.surfaceIndices.add(Math.trunc(event.surfaceIndex));
        this.instanceCounts.add(Math.trunc(event.getInstanceCount()));
    }
}
