package models;

public enum CsgSampleNames
{
    MANT1986_2(1),
    STACKED_BLOCKS(2),
    MOON_BLOCK(3),
    CROSS_PAIR(4),
    HOLLOW_BRICK(5),
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
