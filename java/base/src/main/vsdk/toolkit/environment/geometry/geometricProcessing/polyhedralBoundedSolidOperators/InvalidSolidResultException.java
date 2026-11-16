package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

/**
The operation completed, but its result fails strict validation
(`PolyhedralBoundedSolidValidationEngine.validateStrict`): self-intersecting
faces, broken shells or a wrong Euler characteristic. The message carries
the operation, the result path and the validator report.
 */
public class InvalidSolidResultException
    extends PolyhedralBoundedSolidOperationException
{
    private static final long serialVersionUID = 1L;

    public InvalidSolidResultException(String message)
    {
        super(message);
    }
}
