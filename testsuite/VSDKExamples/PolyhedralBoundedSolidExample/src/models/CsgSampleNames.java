public enum CsgSampleNames
{
    MANT1986_2("MANT1986_2"),
    STACKED_BLOCKS("STACKED_BLOCKS"),
    MANT1986_3("MANT1986_3"),
    HOLLOW_BRICK("HOLLOW_BRICK"),
    CROSS_PAIR("CROSS_PAIR"),
    MOON_BLOCK("MOON_BLOCK"),
    MANT1988_15_2_HOLED("MANT1988_15_2_HOLED"),
    MANT1988_6_13("MANT1988_6_13"),
    MANT1988_15_1("MANT1988_15_1");

    private final String label;

    CsgSampleNames(String label)
    {
        this.label = label;
    }

    public CsgSampleNames nextCircular()
    {
        CsgSampleNames[] values = values();
        int nextIndex = (ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    public String getLabel()
    {
        return label;
    }

    public int getDisplayIndex()
    {
        return ordinal() + 1;
    }

    public static int getTotalSamples()
    {
        return values().length;
    }
}
