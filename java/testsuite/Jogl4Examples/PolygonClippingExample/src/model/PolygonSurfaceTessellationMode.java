package model;

public enum PolygonSurfaceTessellationMode
{
    GLU("GLU"),
    MONOTONE_DECOMPOSITION("Monotone decomposition");

    private final String displayName;

    PolygonSurfaceTessellationMode(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public PolygonSurfaceTessellationMode next()
    {
        PolygonSurfaceTessellationMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
