#ifndef __RASTER_TILE_GENERATOR__
#define __RASTER_TILE_GENERATOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/render/raytracing/RasterTileGenerationStrategy.h"

class Image;
class RasterTileArea;

class RasterTileGenerator {
  private:
    RasterTileGenerationStrategy strategy;
    Image* image;
    int startX;
    int startY;
    int width;
    int height;
    int numberOfThreads;
    ArrayList<RasterTileArea> tiles;

    void initialize();
    ArrayList<RasterTileArea> generateTiles() const;
    ArrayList<RasterTileArea> generateLinearTiles() const;
    ArrayList<RasterTileArea> generateSerialTile() const;

  public:
    RasterTileGenerator(
        RasterTileGenerationStrategy strategy,
        Image* image,
        int width,
        int height,
        int numberOfThreads);

    RasterTileGenerator(
        RasterTileGenerationStrategy strategy,
        Image* image,
        int startX,
        int startY,
        int width,
        int height,
        int numberOfThreads);

    const ArrayList<RasterTileArea>& getTiles() const;
};

#endif
