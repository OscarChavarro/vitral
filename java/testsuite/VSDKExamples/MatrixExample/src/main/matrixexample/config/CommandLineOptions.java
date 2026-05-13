package matrixexample.config;

import vsdk.toolkit.processing.linealAlgebra.StrategySelector;

public class CommandLineOptions
{
    private final int size;
    private final long seed;
    private final String operation;
    private final StrategySelector.ComputeStrategy strategy;
    private final String outputFile;

    public CommandLineOptions(int size, long seed, String operation,
        StrategySelector.ComputeStrategy strategy, String outputFile)
    {
        this.size = size;
        this.seed = seed;
        this.operation = operation;
        this.strategy = strategy;
        this.outputFile = outputFile;
    }

    public static CommandLineOptions parse(String[] args)
    {
        int size = 10;
        long seed = 42L;
        String operation = "both";
        StrategySelector.ComputeStrategy strategy = StrategySelector.ComputeStrategy.NAIVE_COFACTOR_CPU;
        String outputFile = "result.txt";

        int i;
        for ( i = 0; i < args.length; i++ ) {
            String arg = args[i];
            if ( "--size".equals(arg) && i + 1 < args.length ) {
                size = Integer.parseInt(args[++i]);
            }
            else if ( "--seed".equals(arg) && i + 1 < args.length ) {
                seed = Long.parseLong(args[++i]);
            }
            else if ( "--operation".equals(arg) && i + 1 < args.length ) {
                operation = args[++i].toLowerCase();
            }
            else if ( "--strategy".equals(arg) && i + 1 < args.length ) {
                strategy = StrategySelector.ComputeStrategy.valueOf(args[++i].toUpperCase());
            }
            else if ( "--output".equals(arg) && i + 1 < args.length ) {
                outputFile = args[++i];
            }
            else if ( "--help".equals(arg) ) {
                printHelpAndExit(0);
            }
            else {
                printHelpAndExit(1);
            }
        }

        if ( size <= 0 ) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if ( !("det".equals(operation) || "inv".equals(operation) || "both".equals(operation)) ) {
            throw new IllegalArgumentException("operation must be one of: det, inv, both");
        }

        return new CommandLineOptions(size, seed, operation, strategy, outputFile);
    }

    private static void printHelpAndExit(int code)
    {
        System.out.println("Usage: MatrixExample [--size N] [--seed N] [--operation det|inv|both] " +
            "[--strategy NAIVE_COFACTOR_CPU|LU_CPU|GAUSS_CPU] [--output FILE]");
        System.exit(code);
    }

    public int size()
    {
        return size;
    }

    public long seed()
    {
        return seed;
    }

    public String operation()
    {
        return operation;
    }

    public StrategySelector.ComputeStrategy strategy()
    {
        return strategy;
    }

    public String outputFile()
    {
        return outputFile;
    }
}
