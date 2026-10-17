#ifndef __VIEW__
#define __VIEW__

class View {
  public:
    View(int width, int height, double boxX, double boxY, double boxWidth, double boxHeight);
    double x(double canvasX) const;
    double y(double canvasY) const;

  private:
    double scaleX;
    double scaleY;
    double screenCenterX;
    double screenCenterY;
    double boxCenterX;
    double boxCenterY;
};

#endif
