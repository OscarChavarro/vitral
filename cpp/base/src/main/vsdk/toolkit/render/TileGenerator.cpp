#include "vsdk/toolkit/render/TileGenerator.h"
#include "vsdk/toolkit/render/Tile.h"
#include "vsdk/toolkit/media/Image.h"
#include "java/util/ArrayList.txx"
#include <stdexcept>

TileGenerator::TileGenerator(
    TileGenerationStrategy strategyIn,
    Image* imageIn,
    int widthIn,
    int heightIn,
    int numberOfThreadsIn)
    : TileGenerator(strategyIn, imageIn, 0, 0, widthIn, heightIn, numberOfThreadsIn)
{
}

TileGenerator::TileGenerator(
    TileGenerationStrategy strategyIn,
    Image* imageIn,
    int x0In,
    int y0In,
    int widthIn,
    int heightIn,
    int numberOfThreadsIn)
    : strategy(strategyIn), image(imageIn), x0(x0In), y0(y0In),
      width(widthIn), height(heightIn), numberOfThreads(numberOfThreadsIn)
{
    if ( image == 0 ) throw std::invalid_argument("image can not be null");
    if ( x0 < 0 || y0 < 0 ) throw std::invalid_argument("origin must be >= 0");
    if ( width <= 0 || height <= 0 ) throw std::invalid_argument("width/height must be > 0");
    if ( x0 + width > image->getXSize() || y0 + height > image->getYSize() ) {
        throw std::invalid_argument("requested tile area must be inside image");
    }
    if ( numberOfThreads <= 0 ) throw std::invalid_argument("numberOfThreads must be > 0");
    tiles = generateTiles();
}

const java::ArrayList<Tile>& TileGenerator::getTiles() const { return tiles; }

java::ArrayList<Tile> TileGenerator::generateTiles() const
{
    if ( strategy == TileGenerationStrategy::LINEAR ) return generateLinearTiles();
    return generateSerialTile();
}

java::ArrayList<Tile> TileGenerator::generateLinearTiles() const
{
    java::ArrayList<Tile> out;
    int workerBands = numberOfThreads;
    if ( workerBands > height ) workerBands = height;
    int baseBandHeight = height / workerBands;
    int extraRows = height % workerBands;
    int y = 0;

    out.reserve((long int)workerBands);
    for ( int i = 0; i < workerBands; i++ ) {
        int currentBandHeight = baseBandHeight + (i < extraRows ? 1 : 0);
        out.add(Tile(image, x0, y0 + y, width, currentBandHeight));
        y += currentBandHeight;
    }
    return out;
}

java::ArrayList<Tile> TileGenerator::generateSerialTile() const
{
    java::ArrayList<Tile> out;
    out.reserve(1);
    out.add(Tile(image, x0, y0, width, height));
    return out;
}
