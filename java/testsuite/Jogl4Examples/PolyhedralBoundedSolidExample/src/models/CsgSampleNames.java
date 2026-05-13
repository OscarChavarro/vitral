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
    MANT1988_3(8),
    MANT1988_15_2_HOLED(9),
    MANT1988_15_2_LIMIT_DIFFERENCE(10),
    MANT1988_15_2_OPEN_DIFFERENCE(11),
    KURLANDER_BOWL_SINGLE_MOTIF(12),
    KURLANDER_BOWL_ALL_MOTIFS(13);

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

    public CsgOperationNames getPreferredOperation(
        CsgOperationNames currentOperation)
    {
        if ( this == MANT1988_15_2_LIMIT_DIFFERENCE ||
             this == MANT1988_15_2_OPEN_DIFFERENCE ||
             this == KURLANDER_BOWL_SINGLE_MOTIF ||
             this == KURLANDER_BOWL_ALL_MOTIFS ) {
            return CsgOperationNames.DIFFERENCE_A_MINUS_B;
        }
        return currentOperation;
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
