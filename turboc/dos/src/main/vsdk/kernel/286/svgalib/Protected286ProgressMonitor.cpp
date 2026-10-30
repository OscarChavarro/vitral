#include "svgalib/Protected286ProgressMonitor.h"
#include "svgalib/Protected286VgaText.h"

Protected286ProgressMonitor::Protected286ProgressMonitor()
    : lastPercent(0), active(FALSE)
{
}

void
Protected286ProgressMonitor::draw(Protected286Word percent, const char* status)
{
    Protected286VgaText::writeStatusAt(0, 21, "                                                                                ");
    Protected286VgaText::writeStatusAt(2, 21, status);
    Protected286VgaText::drawProgressBar(2, 23, 60, percent);
}

void
Protected286ProgressMonitor::begin()
{
    active = TRUE;
    // Preserve a fixed status area below the scrolling text console.
    Protected286VgaText::setConsoleRows(20);
    lastPercent = 0;
    draw(0, "Rendering scene");
}

void
Protected286ProgressMonitor::end()
{
    if (!active) {
        return;
    }
    lastPercent = 100;
    draw(100, "Render complete");
    active = FALSE;
}

void
Protected286ProgressMonitor::update(double minValue, double maxValue,
                                    double currentValue)
{
    Protected286Word percent;
    if (!active || maxValue <= minValue) {
        return;
    }
    if (currentValue <= minValue) {
        percent = 0;
    }
    else if (currentValue >= maxValue) {
        percent = 100;
    }
    else {
        percent = (Protected286Word)(100.0 * (currentValue - minValue) /
                                     (maxValue - minValue));
    }
    if (percent != lastPercent) {
        lastPercent = percent;
        draw(percent, "Rendering scene");
    }
}

double
Protected286ProgressMonitor::getCurrentPercent()
{
    return (double)lastPercent;
}

const Class Protected286ProgressMonitor::CLASS = {
    "Protected286ProgressMonitor", &ProgressMonitor::CLASS
};

const Class*
Protected286ProgressMonitor::getClass() const
{
    return &CLASS;
}
