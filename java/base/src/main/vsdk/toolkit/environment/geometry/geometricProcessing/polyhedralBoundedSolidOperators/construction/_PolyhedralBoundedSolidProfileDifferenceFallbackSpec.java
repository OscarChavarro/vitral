package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

final class _PolyhedralBoundedSolidProfileDifferenceFallbackSpec
{
    final ArrayList<Vector3Dd> clippedProfileAtCut;
    final double xCut;
    final double xMax;
    final double[] minuendBounds;

    _PolyhedralBoundedSolidProfileDifferenceFallbackSpec(
        ArrayList<Vector3Dd> clippedProfileAtCut,
        double xCut,
        double xMax,
        double[] minuendBounds)
    {
        this.clippedProfileAtCut = clippedProfileAtCut;
        this.xCut = xCut;
        this.xMax = xMax;
        this.minuendBounds = minuendBounds;
    }
}
