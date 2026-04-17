package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

public interface PolyhedralBoundedSolidValidationStrategy
{
    boolean validate(PolyhedralBoundedSolid solid,
                     PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
                     StringBuilder msg);
}
