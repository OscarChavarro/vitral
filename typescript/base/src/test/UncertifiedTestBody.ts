// Port of cpp/base/src/test/UncertifiedTestBody.{h,cpp}
import type { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";

// An application body that preserves its core solid for equivalence but
// cannot certify point containment through its own policies.
export class UncertifiedTestBody extends SimpleBody {
    public override classifyPointCertified(_point: Vector3Dd, _tolerance: number): GeometryContainment {
        return GeometryContainment.Unsupported;
    }
    public override getSolidEquivalenceGeometry(): Geometry | null {
        return this.getGeometry();
    }
}
