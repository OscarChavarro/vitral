package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3D;

final class _PolyhedralBoundedSolidProfileDifferenceFallbackSpec
{
    final ArrayList<Vector3D> clippedProfileAtCut;
    final double xCut;
    final double xMax;
    final double[] minuendBounds;

    _PolyhedralBoundedSolidProfileDifferenceFallbackSpec(
        ArrayList<Vector3D> clippedProfileAtCut,
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
