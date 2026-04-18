public enum CsgOperationNames
{
    UNION("UNION"),
    INTERSECTION("INTERSECTION"),
    DIFFERENCE_A_MINUS_B("DIFFERENCE A-B"),
    DIFFERENCE_B_MINUS_A("DIFFERENCE B-A");

    private final String label;

    CsgOperationNames(String label)
    {
        this.label = label;
    }

    public CsgOperationNames nextCircular()
    {
        CsgOperationNames[] values = values();
        int nextIndex = (ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    public String getLabel()
    {
        return label;
    }
}
