#include <math.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
ProgressMonitorConsoleLongFormat::ProgressMonitorConsoleLongFormat()
    : updateCount(0), charactersPrintedInLastLine(0), currentPercent(0)
{
}

void ProgressMonitorConsoleLongFormat::begin()
{
    updateCount = 0;
    currentPercent = 0;
    charactersPrintedInLastLine = 0;
    printf("    ");
    fflush(stdout);
}

void ProgressMonitorConsoleLongFormat::end()
{
    currentPercent = 100;
    int pending = 55 - charactersPrintedInLastLine;
    for (int i = 0; i < pending; i++) {
        printf(" ");
    }
    printf(" - [100%% / Operation finished!] \n");
    fflush(stdout);
}

void ProgressMonitorConsoleLongFormat::update(double minValue, double maxValue, double currentValue)
{
    if ( Cpp98Support::abs(maxValue - minValue) < VSDK::EPSILON ) {
        return;
    }

    double v = 100 * (currentValue - minValue) / (maxValue - minValue);
    currentPercent = v;
    updateCount++;

    printf(".");
    charactersPrintedInLastLine++;

    if (updateCount % 10 == 0) {
        printf(" ");
        charactersPrintedInLastLine++;
    }
    if (updateCount % 50 == 0) {
        printf(" - [%s%% of %lld]\n    ", VSDK::formatDouble(v).c_str(), ((long long)(Cpp98Support::llround(maxValue))));
        charactersPrintedInLastLine = 0;
    }
    fflush(stdout);
}

double ProgressMonitorConsoleLongFormat::getCurrentPercent()
{
    return currentPercent;
}

const Class ProgressMonitorConsoleLongFormat::CLASS = { "ProgressMonitorConsoleLongFormat", &ProgressMonitor::CLASS };

const Class *
ProgressMonitorConsoleLongFormat::getClass() const
{
    return &CLASS;
}
