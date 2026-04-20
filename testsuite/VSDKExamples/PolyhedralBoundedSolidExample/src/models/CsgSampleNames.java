package models;

public enum CsgSampleNames
{
    HOLLOW_BRICK(1),
    MANT1986_2(2),
    STACKED_BLOCKS(3),
    CROSS_PAIR(4),
    MOON_BLOCK(5),
    MANT1988_6_13(6),
    MANT1988_15_1(7),
    MANT1986_3(8),
    MANT1988_15_2_HOLED(9);

    private final int id;

    CsgSampleNames(int id)
    {
        this.id = id;
    }

    public CsgSampleNames nextCircular()
    {
        CsgSampleNames[] values = values();
        int nextIndex = (ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    public String getLabel()
    {
        return name();
    }

    public int getDisplayIndex()
    {
        return id;
    }

    public static int getTotalSamples()
    {
        return values().length;
    }
}
