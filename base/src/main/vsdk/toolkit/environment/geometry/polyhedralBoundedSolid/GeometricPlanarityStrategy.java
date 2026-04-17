package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

public class GeometricPlanarityStrategy
    implements PolyhedralBoundedSolidValidationStrategy
{
    @Override
    public boolean validate(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        StringBuilder msg)
    {
        return PolyhedralBoundedSolidGeometricValidator
            .validateAllFacesPlanarityAndPlanes(solid, numericContext, msg);
    }
}
