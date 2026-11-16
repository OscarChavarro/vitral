package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

/**
Base of the typed failures raised by the public operations of
{@link PolyhedralBoundedSolidModeler} (set operations and split). It extends
{@link IllegalStateException} so that callers written against the earlier
untyped failures keep working (Fable5.2Analysis.md Stage F).

<ul>
  <li>{@link NonManifoldBooleanResultException}: the regularized result
      would be non-manifold and is rejected by design.</li>
  <li>{@link InvalidSolidResultException}: the operation completed but its
      result fails the strict B-Rep postcondition.</li>
  <li>{@link SolidOperationFailedException}: the operation could not
      produce a result (an internal Euler-operator failure, or a result
      that lost the solid, such as an empty union).</li>
</ul>

<p>With the non-destructive entry points the operands are intact after any
of these failures.</p>
 */
public class PolyhedralBoundedSolidOperationException
    extends IllegalStateException
{
    private static final long serialVersionUID = 1L;

    public PolyhedralBoundedSolidOperationException(String message)
    {
        super(message);
    }

    public PolyhedralBoundedSolidOperationException(String message,
        Throwable cause)
    {
        super(message, cause);
    }
}
