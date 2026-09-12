import { ArrayList } from "../../../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Cone } from "../../../volume/Cone.js";
import type { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "../PolyhedralBoundedSolidModeler.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { _SetOperationTrace } from "../booleans/_SetOperationTrace.js";
import { _PolyhedralBoundedSolidSetOperator } from "../booleans/_PolyhedralBoundedSolidSetOperator.js";
import { _PolyhedralBoundedSolidFallbackGeometry } from "./_PolyhedralBoundedSolidFallbackGeometry.js";

const sameCoordinate = (a: number, b: number): boolean => _PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(a, b);
const addUniqueCoordinate = (values: number[], value: number): void =>
    _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(values, value);

class VerticalCylinderOperandSpec {
    public readonly centerX: number;
    public readonly centerY: number;
    public readonly zMin: number;
    public readonly radius: number;
    public readonly height: number;
    public readonly radialDivisions: number;
    public readonly heightDivisions: number;

    public constructor(
        centerX: number,
        centerY: number,
        zMin: number,
        radius: number,
        height: number,
        radialDivisions: number,
        heightDivisions: number,
    ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.zMin = zMin;
        this.radius = radius;
        this.height = height;
        this.radialDivisions = radialDivisions;
        this.heightDivisions = heightDivisions;
    }
}

class OffsetCylinderDifferenceFallbackSpec {
    public readonly operandA: VerticalCylinderOperandSpec;
    public readonly operandB: VerticalCylinderOperandSpec;

    public constructor(operandA: VerticalCylinderOperandSpec, operandB: VerticalCylinderOperandSpec) {
        this.operandA = operandA;
        this.operandB = operandB;
    }
}

/**
Structural-shape boolean fallback for an offset vertical-cylinder difference:
detects two vertical cylinders in a subtract and rebuilds the difference from
analytic cylinder descriptions. Extracted verbatim from
`_PolyhedralBoundedSolidSetOperator` in Stage 7 R2 (one family per class);
pure code motion, no behavior change.
 */
export class _PolyhedralBoundedSolidOffsetCylinderFallback extends _PolyhedralBoundedSolidOperator {
    private static addUniqueXy(values: ArrayList<Vector3Dd>, point: Vector3Dd): void {
        let i: number;

        for (i = 0; i < values.size(); i++) {
            const current = values.get(i);
            if (sameCoordinate(current.x(), point.x()) && sameCoordinate(current.y(), point.y())) {
                return;
            }
        }
        values.add(point);
    }

    private static describeVerticalCylinder(solid: PolyhedralBoundedSolid | null): VerticalCylinderOperandSpec | null {
        let centerX: number;
        let centerY: number;
        let radius: number;
        let i: number;

        if (solid === null || solid.getVerticesList().size() < 6) {
            return null;
        }

        const bounds = solid.getMinMax();
        if (
            bounds === null ||
            bounds.length < 6 ||
            bounds[5]! <= bounds[2]! + _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
        ) {
            return null;
        }

        centerX = (bounds[0]! + bounds[3]!) * 0.5;
        centerY = (bounds[1]! + bounds[4]!) * 0.5;
        radius = 0.0;
        const zs: number[] = [];
        const xy = new ArrayList<Vector3Dd>();
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const p = solid.getVerticesList().get(i)!.position;
            let radialDistance: number;

            addUniqueCoordinate(zs, p.z());
            _PolyhedralBoundedSolidOffsetCylinderFallback.addUniqueXy(xy, p);
            radialDistance = Math.sqrt((p.x() - centerX) * (p.x() - centerX) + (p.y() - centerY) * (p.y() - centerY));
            if (radialDistance > radius) {
                radius = radialDistance;
            }
        }

        if (xy.size() < 3 || zs.length < 2 || radius <= _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()) {
            return null;
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const p = solid.getVerticesList().get(i)!.position;
            const radialDistance = Math.sqrt(
                (p.x() - centerX) * (p.x() - centerX) + (p.y() - centerY) * (p.y() - centerY),
            );

            if (
                Math.abs(radialDistance - radius) >
                Math.max(_PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(), radius * 1.0e-6)
            ) {
                return null;
            }
        }

        return new VerticalCylinderOperandSpec(
            centerX,
            centerY,
            bounds[2]!,
            radius,
            bounds[5]! - bounds[2]!,
            xy.size(),
            Math.max(1, zs.length - 1),
        );
    }

    public static prepareOffsetCylinderDifferenceFallbackSpec(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        op: number,
    ): OffsetCylinderDifferenceFallbackSpec | null {
        let centerDistance: number;

        if (op !== _PolyhedralBoundedSolidOperator.SUBTRACT) {
            return null;
        }

        const operandA = _PolyhedralBoundedSolidOffsetCylinderFallback.describeVerticalCylinder(inSolidA);
        const operandB = _PolyhedralBoundedSolidOffsetCylinderFallback.describeVerticalCylinder(inSolidB);
        if (operandA === null || operandB === null) {
            return null;
        }

        if (operandA.radialDivisions === operandB.radialDivisions) {
            return null;
        }
        if (!sameCoordinate(operandA.radius, operandB.radius) || !sameCoordinate(operandA.height, operandB.height)) {
            return null;
        }

        centerDistance = Math.sqrt(
            (operandA.centerX - operandB.centerX) * (operandA.centerX - operandB.centerX) +
                (operandA.centerY - operandB.centerY) * (operandA.centerY - operandB.centerY),
        );
        if (
            centerDistance <= _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon() ||
            centerDistance >=
                operandA.radius + operandB.radius - _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
        ) {
            return null;
        }
        if (
            operandA.zMin >=
                operandB.zMin + operandB.height - _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon() ||
            operandB.zMin >=
                operandA.zMin + operandA.height - _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
        ) {
            return null;
        }

        return new OffsetCylinderDifferenceFallbackSpec(operandA, operandB);
    }

    private static createFallbackCylinder(
        spec: VerticalCylinderOperandSpec,
        radialDivisions: number,
        heightDivisions: number,
    ): PolyhedralBoundedSolid {
        let cylinder: PolyhedralBoundedSolid;
        let translation: Matrix4x4d;

        cylinder = new Cone(spec.radius, spec.radius, spec.height).exportToPolyhedralBoundedSolid(
            radialDivisions,
            heightDivisions,
        );
        translation = new Matrix4x4d();
        translation = translation.translation(spec.centerX, spec.centerY, spec.zMin);
        PolyhedralBoundedSolidModeler.applyTransformation(cylinder, translation);
        return cylinder;
    }

    public static buildOffsetCylinderDifferenceFallback(
        spec: OffsetCylinderDifferenceFallbackSpec | null,
    ): PolyhedralBoundedSolid | null {
        let fallbackA: PolyhedralBoundedSolid;
        let fallbackB: PolyhedralBoundedSolid;
        let result: PolyhedralBoundedSolid | null;
        let radialDivisions: number;
        let heightDivisions: number;

        if (spec === null) {
            return null;
        }

        radialDivisions = Math.max(spec.operandA.radialDivisions, spec.operandB.radialDivisions);
        heightDivisions = Math.max(spec.operandA.heightDivisions, spec.operandB.heightDivisions);
        fallbackA = _PolyhedralBoundedSolidOffsetCylinderFallback.createFallbackCylinder(
            spec.operandA,
            radialDivisions,
            heightDivisions,
        );
        fallbackB = _PolyhedralBoundedSolidOffsetCylinderFallback.createFallbackCylinder(
            spec.operandB,
            radialDivisions,
            heightDivisions,
        );

        try {
            result = _PolyhedralBoundedSolidSetOperator.setOp(
                fallbackA,
                fallbackB,
                _PolyhedralBoundedSolidOperator.SUBTRACT,
                false,
                true,
            );
        } catch (e) {
            _SetOperationTrace.tracePipelineSummary(
                "offset cylinder fallback failed: " + (e as Error).constructor.name,
            );
            return null;
        }
        if (!_PolyhedralBoundedSolidSetOperator.isStructurallyUsableSetOpResult(result)) {
            _SetOperationTrace.tracePipelineSummary("offset cylinder fallback rejected");
            return null;
        }
        _SetOperationTrace.tracePipelineSummary(
            "offset cylinder fallback accepted faces=" +
                result!.getPolygonsList().size() +
                " edges=" +
                result!.getEdgesList().size() +
                " vertices=" +
                result!.getVerticesList().size(),
        );
        return result;
    }
}

type _VerticalCylinderOperandSpec = VerticalCylinderOperandSpec;
type _OffsetCylinderDifferenceFallbackSpec = OffsetCylinderDifferenceFallbackSpec;

export namespace _PolyhedralBoundedSolidOffsetCylinderFallback {
    export type VerticalCylinderOperandSpec = _VerticalCylinderOperandSpec;
    export type OffsetCylinderDifferenceFallbackSpec = _OffsetCylinderDifferenceFallbackSpec;
}
