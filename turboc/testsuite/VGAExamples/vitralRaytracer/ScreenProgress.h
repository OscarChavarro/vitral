#ifndef __SCREEN_PROGRESS__
#define __SCREEN_PROGRESS__
#include <stdio.h>
#include <vga.h>
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"

class ScreenProgress : public ProgressMonitor {
    int width;
    int row;
    int color;
    int filled;
    int text;
    double percent;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    ScreenProgress(int width, int height, int color, int text)
        : width(width), row(height - 1), color(color), filled(0), text(text), percent(0) {}
    virtual void begin() {
        filled = 0;
        percent = 0;
    }
    virtual void end() {
        percent = 100.0;
        advance(width);
    }
    virtual void update(double minValue, double maxValue, double currentValue) {
        if (maxValue - minValue < 1e-9) {
            return;
        }
        percent = 100.0 * (currentValue - minValue) / (maxValue - minValue);
        advance((int)(percent * width / 100.0));
    }
    virtual double getCurrentPercent() { return percent; }

private:
    void advance(int to) {
        if (to > width) {
            to = width;
        }
        if (to <= filled) {
            return;
        }
        if (text) {
            int from = filled * (TEXT_COLUMNS - 1) / width;
            int until = to * (TEXT_COLUMNS - 1) / width;
            for (int column = from; column < until; column++) {
                printf("\033[%d;%dH#", TEXT_ROWS, column + 1);
            }
            fflush(stdout);
            filled = to;
            return;
        }
        vga_setcolor(color);
        for (; filled < to; filled++) {
            vga_drawpixel(filled, row);
        }
    }
};

#endif
