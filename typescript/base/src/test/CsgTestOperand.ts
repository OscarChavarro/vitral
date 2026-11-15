// Port of cpp/base/src/test/CsgTestOperand.{h,cpp}
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import type { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import type { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import type { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import type { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { CsgOperandView } from "vsdk/toolkit/environment/scene/csg/CsgOperandView.js";

export class CsgTestOperand extends CsgOperandView {
    private readonly result: GeometryContainment;
    private readonly smallerResult: GeometryContainment;
    private readonly switchLimit: number;
    private malformedRay: boolean;

    public constructor(value: GeometryContainment);
    public constructor(value: GeometryContainment, smaller: GeometryContainment, limit: number);
    public constructor(value: GeometryContainment, smaller?: GeometryContainment, limit?: number) {
        super();
        this.result = value;
        this.smallerResult = smaller ?? value;
        this.switchLimit = limit ?? -1;
        this.malformedRay = false;
    }
    public getGeometry(): Geometry | null {
        return null;
    }
    public getAffinePlacement(): Matrix4x4d {
        return Matrix4x4d.identityMatrix();
    }
    public override classifyPointCertified(_point: Vector3Dd, tolerance: number): GeometryContainment {
        return tolerance <= this.switchLimit ? this.smallerResult : this.result;
    }
    public setMalformedRay(value: boolean): void {
        this.malformedRay = value;
    }
    public override classifyRay(
        _ray: Ray,
        _query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
        return this.malformedRay ? RaySolidQueryStatus.Supported : RaySolidQueryStatus.Unsupported;
    }
}
