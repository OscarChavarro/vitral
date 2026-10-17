#include "View.h"

View::View(int width, int height, double boxX, double boxY, double boxWidth, double boxHeight) {
    scaleX = width / 640.0;
    scaleY = height / 480.0;
    double fitX = 0.9 * 640.0 / boxWidth;
    double fitY = 0.9 * 480.0 / boxHeight;
    double k = fitX < fitY ? fitX : fitY;
    scaleX *= k;
    scaleY *= k;
    screenCenterX = width / 2.0;
    screenCenterY = height / 2.0;
    boxCenterX = boxX + boxWidth / 2.0;
    boxCenterY = boxY + boxHeight / 2.0;
}

double
View::x(double canvasX) const { return screenCenterX + (canvasX - boxCenterX) * scaleX; }
double
View::y(double canvasY) const { return screenCenterY + (canvasY - boxCenterY) * scaleY; }
