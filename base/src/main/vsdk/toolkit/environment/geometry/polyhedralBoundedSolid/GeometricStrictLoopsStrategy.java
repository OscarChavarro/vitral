package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

public class GeometricStrictLoopsStrategy
    implements PolyhedralBoundedSolidValidationStrategy
{
    @Override
    public boolean validate(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        StringBuilder msg)
    {
        return PolyhedralBoundedSolidGeometricValidator
            .validateLoopsStrict(solid, numericContext, msg);
    }
}
