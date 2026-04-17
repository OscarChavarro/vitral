package vsdk.toolkit.environment.geometry;

public interface PolyhedralBoundedSolidValidationStrategy
{
    boolean validate(PolyhedralBoundedSolid solid,
                     PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
                     StringBuilder msg);
}
