import { Matrix4x4d } from "../../../../../common/linealAlgebra/Matrix4x4d.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import { PolyhedralBoundedSolidModeler } from "../PolyhedralBoundedSolidModeler.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidFallbackGeometry } from "../fallbacks/_PolyhedralBoundedSolidFallbackGeometry.js";
import { _PolyhedralBoundedSolidProfileDifferenceFallbackSpec } from "./_PolyhedralBoundedSolidProfileDifferenceFallbackSpec.js";

/** Structural-shape fallback for subtraction of extruded YZ profiles. */
export class _PolyhedralBoundedSolidProfileDifferenceFallback extends _PolyhedralBoundedSolidOperator {
    private static isBetween(value: number, min: number, max: number): boolean {
        return value > min + this.numericContext.bigEpsilon() && value < max - this.numericContext.bigEpsilon();
    }
    public static prepareProfileDifferenceFallbackSpec(
        minuend: PolyhedralBoundedSolid,
        subtrahend: PolyhedralBoundedSolid,
        operation: number,
    ): _PolyhedralBoundedSolidProfileDifferenceFallbackSpec | null {
        if (
            operation !== this.SUBTRACT ||
            minuend.getVerticesList().size() <= 0 ||
            subtrahend.getVerticesList().size() <= 0
        )
            return null;
        const minuendBounds = minuend.getMinMax();
        const subtrahendBounds = subtrahend.getMinMax();
        if (
            !_PolyhedralBoundedSolidFallbackGeometry.boundsMatch(
                Array.from(minuendBounds),
                Array.from(subtrahendBounds),
            )
        )
            return null;
        const minuendX = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(minuend, 0);
        const subtrahendX = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(subtrahend, 0);
        const subtrahendZ = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(subtrahend, 2);
        if (minuendX.length !== 2 || subtrahendX.length !== 3 || subtrahendZ.length !== 3) return null;
        const xCut = subtrahendX[1]!;
        const zCut = subtrahendZ[1]!;
        if (
            !this.isBetween(xCut, minuendBounds[0]!, minuendBounds[3]!) ||
            !this.isBetween(zCut, minuendBounds[2]!, minuendBounds[5]!)
        )
            return null;
        const profile = _PolyhedralBoundedSolidFallbackGeometry.extractProfileAtX(minuend, minuendBounds[0]!);
        const clippedProfile = _PolyhedralBoundedSolidFallbackGeometry.clipProfileAboveZ(profile, xCut, zCut);
        if (clippedProfile.length < 3) return null;
        clippedProfile.reverse();
        return new _PolyhedralBoundedSolidProfileDifferenceFallbackSpec(
            clippedProfile,
            xCut,
            minuendBounds[3]!,
            Array.from(minuendBounds),
        );
    }
    private static buildProfileDifferenceFallback(
        spec: _PolyhedralBoundedSolidProfileDifferenceFallbackSpec | null,
    ): PolyhedralBoundedSolid | null {
        if (
            spec === null ||
            spec.clippedProfileAtCut === null ||
            spec.clippedProfileAtCut.length < 3 ||
            spec.xMax <= spec.xCut + this.numericContext.bigEpsilon()
        )
            return null;
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, spec.clippedProfileAtCut[0]!, 1, 1);
        for (let i = 1; i < spec.clippedProfileAtCut.length; i++)
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, spec.clippedProfileAtCut[i]!);
        PolyhedralBoundedSolidEulerOperators.mef(
            solid,
            1,
            1,
            spec.clippedProfileAtCut.length,
            spec.clippedProfileAtCut.length - 1,
            1,
            2,
            2,
        );
        let translation = new Matrix4x4d();
        translation = translation.translation(spec.xMax - spec.xCut, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, translation);
        PolyhedralBoundedSolidTopologyEditing.compactIds(solid);
        return solid;
    }
    public static applyProfileDifferenceFallbackIfNeeded(
        spec: _PolyhedralBoundedSolidProfileDifferenceFallbackSpec | null,
        result: PolyhedralBoundedSolid | null,
    ): PolyhedralBoundedSolid | null {
        if (
            spec === null ||
            result === null ||
            !_PolyhedralBoundedSolidFallbackGeometry.boundsMatch(Array.from(result.getMinMax()), spec.minuendBounds)
        )
            return result;
        const fallback = this.buildProfileDifferenceFallback(spec);
        return fallback === null ? result : fallback;
    }
}
