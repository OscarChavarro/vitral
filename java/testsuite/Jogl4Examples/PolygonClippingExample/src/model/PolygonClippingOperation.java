package model;

public enum PolygonClippingOperation
{
    INTERSECTION("INTERSECTION"),
    UNION("UNION"),
    A_MINUS_B("A_MINUS_B"),
    B_MINUS_A("B_MINUS_A");

    private final String displayName;

    PolygonClippingOperation(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public PolygonClippingOperation next()
    {
        PolygonClippingOperation[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
