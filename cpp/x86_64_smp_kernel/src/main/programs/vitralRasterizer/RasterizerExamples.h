#ifndef __RASTERIZER_EXAMPLES__
#define __RASTERIZER_EXAMPLES__

class RGBImageUncompressed;

class RasterizerExamples {
  public:
    static constexpr int COUNT = 3;
    static void draw(int which, RGBImageUncompressed *img, int width, int height);

  private:
    static void drawLines(RGBImageUncompressed *img, int width, int height);
    static void drawPolygons(RGBImageUncompressed *img, int width, int height);
    static void drawSmoothPolygons(RGBImageUncompressed *img, int width, int height);
};

#endif
