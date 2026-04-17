package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

public class GeometricStrictFaceIntersectionsStrategy
    implements PolyhedralBoundedSolidValidationStrategy
{
    @Override
    public boolean validate(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        StringBuilder msg)
    {
        return PolyhedralBoundedSolidGeometricValidator
            .validateFaceIntersectionsStrict(solid, numericContext, msg);
    }
}
