#ifndef __VSDK_TOOLKIT_RENDER_TILEGENERATOR_H__
#define __VSDK_TOOLKIT_RENDER_TILEGENERATOR_H__

#include <vector>
#include "TileGenerationStrategy.h"

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
    std::vector<Tile> tiles;

    std::vector<Tile> generateTiles() const;
    std::vector<Tile> generateLinearTiles() const;
    std::vector<Tile> generateSerialTile() const;

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

    const std::vector<Tile>& getTiles() const;
};

#endif
