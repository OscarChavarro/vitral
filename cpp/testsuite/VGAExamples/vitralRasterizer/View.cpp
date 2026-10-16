#include "View.h"

View::View(int width, int height, double boxX, double boxY, double boxWidth, double boxHeight) {
    sx = width / 640.0;
    sy = height / 480.0;
    double fitX = 0.9 * 640.0 / boxWidth;
    double fitY = 0.9 * 480.0 / boxHeight;
    double k = fitX < fitY ? fitX : fitY;
    sx *= k;
    sy *= k;
    ox = width / 2.0;
    oy = height / 2.0;
    bx = boxX + boxWidth / 2.0;
    by = boxY + boxHeight / 2.0;
}

double
View::x(double canvasX) const { return ox + (canvasX - bx) * sx; }
double
View::y(double canvasY) const { return oy + (canvasY - by) * sy; }
