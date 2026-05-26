#ifndef __VSDK_TOOLKIT_RENDER_TILEGENERATOR_H__
#define __VSDK_TOOLKIT_RENDER_TILEGENERATOR_H__

#include "vsdk/toolkit/render/TileGenerationStrategy.h"
#include "java/util/ArrayList.h"

class Image;
class Tile;

class TileGenerator {
private:
    TileGenerationStrategy strategy;
    Image* image;
    int x0;
    int y0;
    int width;
    int height;
    int numberOfThreads;
    java::ArrayList<Tile> tiles;

    java::ArrayList<Tile> generateTiles() const;
    java::ArrayList<Tile> generateLinearTiles() const;
    java::ArrayList<Tile> generateSerialTile() const;

public:
    TileGenerator(
        TileGenerationStrategy strategy,
        Image* image,
        int width,
        int height,
        int numberOfThreads);

    TileGenerator(
        TileGenerationStrategy strategy,
        Image* image,
        int x0,
        int y0,
        int width,
        int height,
        int numberOfThreads);

    const java::ArrayList<Tile>& getTiles() const;
};

#endif
