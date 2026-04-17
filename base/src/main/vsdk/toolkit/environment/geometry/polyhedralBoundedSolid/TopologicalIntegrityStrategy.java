package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

public class TopologicalIntegrityStrategy
    implements PolyhedralBoundedSolidValidationStrategy
{
    @Override
    public boolean validate(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        StringBuilder msg)
    {
        PolyhedralBoundedSolidTopologicalValidator
            .remakeEmanatingHalfedgesReferences(solid);
        if ( !PolyhedralBoundedSolidTopologicalValidator
            .validateTopologicalIntegrity(solid) ) {
            msg.append("  - Topological integrity test failed.\n");
            return false;
        }
        return true;
    }
}
