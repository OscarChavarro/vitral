import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "./PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "./PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTopologyEditing } from "./PolyhedralBoundedSolidTopologyEditing.js";
import { _GeometricPlanarityStrategy } from "./_GeometricPlanarityStrategy.js";
import { _GeometricStrictFaceIntersectionsStrategy } from "./_GeometricStrictFaceIntersectionsStrategy.js";
import { _GeometricStrictLoopsStrategy } from "./_GeometricStrictLoopsStrategy.js";
import { _TopologicalIntegrityStrategy } from "./_TopologicalIntegrityStrategy.js";

/** Validation passes for intermediate and boolean-ready boundary representations. */
export class PolyhedralBoundedSolidValidationEngine {
    private static strictCount = 0;
    public static validateIntermediate(solid: PolyhedralBoundedSolid, message: string[] = []): boolean {
        return this.run(solid, [_GeometricPlanarityStrategy, _TopologicalIntegrityStrategy], message);
    }
    public static validateStrict(solid: PolyhedralBoundedSolid, message: string[] = []): boolean {
        this.strictCount++;
        return this.run(
            solid,
            [
                _GeometricPlanarityStrategy,
                _TopologicalIntegrityStrategy,
                _GeometricStrictLoopsStrategy,
                _GeometricStrictFaceIntersectionsStrategy,
            ],
            message,
        );
    }
    public static getStrictValidationInvocationCount(): number {
        return this.strictCount;
    }
    public static resetStrictValidationInvocationCount(): void {
        this.strictCount = 0;
    }
    public static validateBooleanInputs(
        first: PolyhedralBoundedSolid,
        second: PolyhedralBoundedSolid,
        message: string[] = [],
    ): boolean {
        if (!this.validateIntermediate(first, message) || !this.validateIntermediate(second, message)) return false;
        for (const [label, solid] of [
            ["solidA", first],
            ["solidB", second],
        ] as const) {
            const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
            if (!PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(solid, context)) {
                const welded = PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(solid, context);
                message.push(`${label} had coincident vertices; welded ${welded} pair(s).`);
                if (!this.validateIntermediate(solid, message))
                    throw new Error(`${label} is topologically invalid after coincident-vertex weld.`);
            }
            if (!PolyhedralBoundedSolidGeometricValidator.validateUniqueFaceAndVertexIds(solid, message)) return false;
        }
        return true;
    }
    public static areGeometricallyIdentical(
        a: PolyhedralBoundedSolid | null,
        b: PolyhedralBoundedSolid | null,
        tolerance: number,
    ): boolean {
        if (
            a === null ||
            b === null ||
            a.getVerticesList().length !== b.getVerticesList().length ||
            a.getEdgesList().length !== b.getEdgesList().length ||
            a.getPolygonsList().length !== b.getPolygonsList().length
        )
            return false;
        const ma = a.getMinMax(),
            mb = b.getMinMax();
        for (let i = 0; i < 6; i++) if (Math.abs(ma[i]! - mb[i]!) > tolerance) return false;
        const used = new Set<number>();
        return a.getVerticesList().every((v) => {
            const index = b
                .getVerticesList()
                .findIndex(
                    (w, i) =>
                        !used.has(i) &&
                        Math.abs(v.position.x() - w.position.x()) <= tolerance &&
                        Math.abs(v.position.y() - w.position.y()) <= tolerance &&
                        Math.abs(v.position.z() - w.position.z()) <= tolerance,
                );
            if (index < 0) return false;
            used.add(index);
            return true;
        });
    }
    private static run(
        solid: PolyhedralBoundedSolid,
        strategyTypes: (new () => {
            validate: (
                solid: PolyhedralBoundedSolid,
                context: ReturnType<typeof PolyhedralBoundedSolidNumericPolicy.forSolid>,
                message: string[],
            ) => boolean;
        })[],
        message: string[],
    ): boolean {
        const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        const valid = strategyTypes.every((Strategy) => new Strategy().validate(solid, context, message));
        solid.setValidationState(valid);
        return valid;
    }
}
