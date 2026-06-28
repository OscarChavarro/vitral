package vsdk.toolkit.numericalAnalysis.lookUpTables;

public final class LookUpTableSine
{
    private static final int SIZE = 1000;
    private final double[] table;

    public LookUpTableSine()
    {
        table = new double[SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            table[i] = Math.sin(i / (double)SIZE * (Math.PI * 2.0));
        }
    }

    public LookUpTableSine(int numberOfApproximationDecimals)
    {
        table = new double[SIZE];
        double scale = Math.pow(10.0, numberOfApproximationDecimals);
        double pi = Math.round(Math.PI * scale) / scale;
        for ( int i = 0; i < SIZE; i++ ) {
            table[i] = Math.sin(i / (double)SIZE * (pi * 2.0));
        }
    }

    public double eval(double fraction)
    {
        int index = (int)(fraction * SIZE);
        if ( index < 0 ) {
            index = 0;
        }
        else if ( index >= SIZE ) {
            index = SIZE - 1;
        }
        return table[index];
    }
}
