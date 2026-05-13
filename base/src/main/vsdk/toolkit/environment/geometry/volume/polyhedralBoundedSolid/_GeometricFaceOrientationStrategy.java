//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

/**
Applies the face-orientation consistency expected from the 2-manifold
boundary model of [MANT1988].10.2.1. An inverted face plane normal breaks
both the visual rendering (back-facing triangles) and the face-equation
half-space test of [MANT1988].13.1 used downstream by the boolean pipeline.
*/
public class _GeometricFaceOrientationStrategy
    implements _PolyhedralBoundedSolidValidationStrategy
{
    /**
    Delegates to the centroid-based heuristic in
    `PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations`.
    */
    @Override
    public boolean validate(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        StringBuilder msg)
    {
        return PolyhedralBoundedSolidGeometricValidator
            .validateConsistentFaceOrientations(solid, numericContext, msg);
    }
}
