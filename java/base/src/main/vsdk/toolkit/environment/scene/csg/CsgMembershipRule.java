package vsdk.toolkit.environment.scene.csg;

/** Boolean rules shared by every CSG evaluator over an ordered expression. */
public class CsgMembershipRule {
    // [ROTH1982] Table 3, folded left to right over the operands: the value of
    // operand 0 starts the fold and each later operand is combined with it, so
    // a difference subtracts operands 1..N from operand 0.
    public static boolean combine(CsgOperation operation, boolean accumulated, boolean operandInside)
    {
        if (operation==CsgOperation.UNION) return accumulated || operandInside;
        if (operation==CsgOperation.INTERSECTION) return accumulated && operandInside;
        return accumulated && !operandInside;
    }

    // Whether no later operand can change an accumulated fold value: true for
    // a union already inside and for an intersection or difference already
    // outside. Point evaluators stop querying operands there.
    public static boolean isDecided(CsgOperation operation, boolean accumulated)
    {
        return operation==CsgOperation.UNION ? accumulated : !accumulated;
    }

    // POV crossing-membership rule: whether an event of operand eventOperand
    // must lie inside sibling (true) or outside it (false). Union events are
    // never filtered, so it is only meaningful when filtersEvents is true.
    public static boolean filtersEvents(CsgOperation operation)
    {
        return operation!=CsgOperation.UNION;
    }

    public static boolean requiresContainment(CsgOperation operation, long eventOperand, long sibling)
    {
        return operation==CsgOperation.INTERSECTION || (operation==CsgOperation.DIFFERENCE && sibling==0);
    }
}
