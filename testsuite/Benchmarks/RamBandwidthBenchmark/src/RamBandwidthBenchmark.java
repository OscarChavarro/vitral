//===========================================================================

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.StopWatch;

/**
This class implements a simple brute-force benchmark for testing the available
bandwith between CPU and main RAM memory.  It allocates memory on byte arrays
of one megabyte (1024*1024 bytes) and later increases the arrays contents
using single integer arithmetic operations.

For small values of ram usage, this benchmark is usefull in testing
L1 and L2 CPU's cache performance.  For large values of ram usage, this
benchmark is usefull for measuring main RAM performance.

It is important to run this benchmark with virtual memory turned off!
*/
public class RamBandwidthBenchmark
{
    private int ramUsageLimitInMegaBytes;
    private int testIterations;
    private byte ram[][];
    private StopWatch stopWatch;
    private double createTime;
    private double initTime;
    private double runTime;

    public RamBandwidthBenchmark(int ramUsageLimitInMegaBytes, int testIterations)
    {
        this.ramUsageLimitInMegaBytes = ramUsageLimitInMegaBytes;
        if ( this.ramUsageLimitInMegaBytes < 1 ) {
            this.ramUsageLimitInMegaBytes = 1;
        }
        this.testIterations = testIterations;
        if ( this.testIterations < 1 ) {
            this.testIterations = 1;
        }
        stopWatch = new StopWatch();

        createTime = initTime = runTime = 0;
    }

    private byte[][] createArrays(int n)
    {
        System.out.print("Allocating memory ... ");
        stopWatch.start();

        byte[][] mem;
        mem = new byte[n][];
        int i;

        for ( i = 0; i < n; i++ ) {
            mem[i] = new byte[1024*1024];
        }

        stopWatch.stop();
        createTime = stopWatch.getElapsedRealTime();
        System.out.println("Ok!");
        return mem;
    }

    private void initializeArrays()
    {
        System.out.print("Filling memory with 0's ... ");
        stopWatch.start();

        int i, j;

        for ( i = 0; i < ramUsageLimitInMegaBytes; i++ ) {
            for ( j = 0; j < 1024*1024; j++ ) {
                ram[i][j] = 0x00;
            }
        }

        stopWatch.stop();
        initTime = stopWatch.getElapsedRealTime();
        System.out.println("Ok!");
    }

    private void incrementArrays()
    {
        int i, j, n;
        long s;
        double dt;
        byte array[];

        System.out.println("Incrementing numbers in memory ->");

        for ( n = 0; n < testIterations; n++ ) {
            System.out.print("{" + (n+1) + " / " + testIterations + "}: ");
            stopWatch.start();

            for ( i = 0; i < ramUsageLimitInMegaBytes; i++ ) {
                array = ram[i];
                for ( j = 0; j < 1024*1024; j++ ) {
                    array[j]++;
                }
            }

            stopWatch.stop();
            dt = stopWatch.getElapsedRealTime();
            runTime += dt;
            s = (1024*1024*ramUsageLimitInMegaBytes);
            System.out.println(s + " bytes incremented on RAM in " + VSDK.formatDouble(dt, 2) + " seconds (" + VSDK.formatDouble(((((double)s)*8/dt)/1000000.0), 2) + " Mbps).");
        }
    }

    private void printReport()
    {
        double a;

        a = (((double)ramUsageLimitInMegaBytes) * 1024.0 * 1024.0 * 8.0 * ((double)testIterations)) / runTime;

        System.out.println("Performance report:");
        System.out.println("  - Test RAM Size: " + ramUsageLimitInMegaBytes + " MBytes.");
        System.out.println("  - RAM allocation time: " + VSDK.formatDouble(createTime, 3) + " seconds.");
        System.out.println("  - RAM initialization time: " + VSDK.formatDouble(initTime, 3) + " seconds.");
        System.out.println("  - RAM access time for " + VSDK.formatDouble(testIterations, 3) + " iterations: " + runTime + " seconds.");
        System.out.println("  - RAM Bandwidth for increments: " + VSDK.formatDouble(a/1000000.0) + " Mbps." );
    }

    public void executeBenchmark()
    {
        ram = createArrays(ramUsageLimitInMegaBytes);
        initializeArrays();
        incrementArrays();
        printReport();
    }

    public static void main(String args[]) {
        RamBandwidthBenchmark app;
        int ram = 100;
        int n = 1;

        if ( args.length >= 1 ) {
            ram = Integer.parseInt(args[0]);
        }
        if ( args.length >= 2 ) {
            n = Integer.parseInt(args[1]);
        }

        app = new RamBandwidthBenchmark(ram, n);
        app.executeBenchmark();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
