package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

/**
The operation could not produce a result: an Euler operator reported a
fatal inconsistency (the cause is the original
`vsdk.toolkit.common.VSDKFatalException`), the pipeline returned no solid,
or it returned an empty solid where the operands prove that the result is
not empty (for instance a union of non-empty solids, or an intersection of
solids whose interiors overlap). An empty result that is right, such as
A \ A, is returned normally; see
{@link PolyhedralBoundedSolidModeler#setOp(
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid,
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid,
int)}.
 */
public class SolidOperationFailedException
    extends PolyhedralBoundedSolidOperationException
{
    private static final long serialVersionUID = 1L;

    public SolidOperationFailedException(String message)
    {
        super(message);
    }

    public SolidOperationFailedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
