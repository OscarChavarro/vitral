package models;

/**
Display states for the Appel hidden-line algorithm, cycled with key [8]:
OFF disables it, EDGES_VISIBLE_HIDDEN draws contour, visible and hidden lines,
and EDGES_VISIBLE draws only contour and visible lines (the hidden ones are
suppressed).
*/
public enum AppelDisplayMode
{
    OFF("OFF"),
    EDGES_VISIBLE_HIDDEN("edges + visible + hidden"),
    EDGES_VISIBLE("edges + visible");

    private final String label;

    AppelDisplayMode(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public AppelDisplayMode nextCircular()
    {
        AppelDisplayMode[] values = AppelDisplayMode.values();
        return values[(ordinal() + 1) % values.length];
    }
}
