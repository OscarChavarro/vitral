import { StringBuilder } from "java/lang/StringBuilder.js";
import type { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidStructuralCopy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Package-visible `SplitterOracleFuzzTest.check` from the Java suite. It
lives in a sibling module because `SplitterGeneralPlaneOracleFuzzTest`
reuses it and importing a `.spec.ts` file would re-run that file's tests.
 */
export class SplitterOracleFuzzTestSupport {
    public static readonly TOLERANCE = 1.0e-6;

    private constructor() {}

    public static check(base: PolyhedralBoundedSolid, plane: InfinitePlane): string | null {
        const TOLERANCE = SplitterOracleFuzzTestSupport.TOLERANCE;
        const solid = PolyhedralBoundedSolidStructuralCopy.copy(base);
        const above: PolyhedralBoundedSolid[] = [];
        const below: PolyhedralBoundedSolid[] = [];
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(base);
        let min = Number.MAX_VALUE;
        let max = -Number.MAX_VALUE;
        let total = 0.0;
        let i: number;

        for (i = 0; i < base.getVerticesList().size(); i++) {
            const d = plane.pointDistance(base.getVerticesList().get(i)!.position);
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        const cuts = min < -TOLERANCE && max > TOLERANCE;
        try {
            PolyhedralBoundedSolidModeler.split(solid, plane, above, below);
        } catch (t) {
            return "exception " + (t as Error).constructor.name;
        }
        let strict = true;
        let empty = false;
        let sides = true;
        for (let side = 1; side >= -1; side -= 2) {
            for (const piece of side > 0 ? above : below) {
                strict = PolyhedralBoundedSolidValidationEngine.validateStrict(piece, new StringBuilder()) && strict;
                total += PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
                empty = empty || (cuts && piece.getPolygonsList().size() === 0);
                for (i = 0; i < piece.getVerticesList().size(); i++) {
                    if (side * plane.pointDistance(piece.getVerticesList().get(i)!.position) < -TOLERANCE) {
                        sides = false;
                    }
                }
            }
        }
        if (Math.abs(total - volume) > TOLERANCE * Math.max(1.0, volume)) {
            return "volume";
        }
        if (empty) {
            return "empty piece";
        }
        if (!sides) {
            return "side";
        }
        if (!strict) {
            return "strict";
        }
        return null;
    }
}
