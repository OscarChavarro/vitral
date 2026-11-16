package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

/**
An operand of a boolean operation is not a valid boundary model
(`PolyhedralBoundedSolidValidationEngine.validateBooleanInputs` fails), even
after coincident vertices were welded. The operation does not start: the
pipeline of [MANT1988] Ch. 15 assumes valid operands. The message carries
the validator report.
 */
public class InvalidOperandException
    extends PolyhedralBoundedSolidOperationException
{
    private static final long serialVersionUID = 1L;

    public InvalidOperandException(String message)
    {
        super(message);
    }

    public InvalidOperandException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
