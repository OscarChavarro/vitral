package vsdk.toolkit.gui.feedback;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.feedback.ProgressMonitor;

/**
 * This is the concrete implementation for the output console, not thread safe, not re-entrant. It should
 * take exclusive control of the console and be used from a single thread. For parallel operations, this
 * class should be used from the ParallelProgressMonitorConsumer thread.
 */
public class ProgressMonitorConsoleLongFormat extends ProgressMonitor {

    private long n;
    private int charactersPrintedInLastLine = 0;
    private double currentPercent;

    public ProgressMonitorConsoleLongFormat() {
    }

    @Override
    public void begin() {
        n = 0;
        currentPercent = 0;
        charactersPrintedInLastLine = 0;
        System.out.print("    ");
    }

    @Override
    public void end() {
        currentPercent = 100;
        int pending = 55 - charactersPrintedInLastLine;
        for (int i = 0; i < pending; i++) {
            System.out.print(" ");
        }
        System.out.print(" - [100% / Operation finished!] \n");
    }

    @Override
    public void
    update(double minValue, double maxValue, double currentValue) {
        if ( Math.abs(maxValue - minValue) < VSDK.EPSILON ) {
            return;
        }
        double v = 100 * (currentValue - minValue) / (maxValue - minValue);
        currentPercent = v;

        n++;

        System.out.print(".");
        charactersPrintedInLastLine++;

        if (n % 10 == 0) {
            System.out.print(" ");
            charactersPrintedInLastLine++;
        }
        if (n % 50 == 0) {
            System.out.print(" - [" + VSDK.formatDouble(v) + "% of " + Math.round(maxValue) + "]\n    ");
            charactersPrintedInLastLine = 0;
        }
    }

    @Override
    public double getCurrentPercent() {
        return currentPercent;
    }
}
