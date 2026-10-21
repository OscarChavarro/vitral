#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/render/raytracing/RasterTileArea.h"
#include "vsdk/toolkit/render/raytracing/RasterTileGenerator.h"

RasterTileGenerator::RasterTileGenerator(
    RasterTileGenerationStrategy strategyIn,
    Image* imageIn,
    int widthIn,
    int heightIn,
    int numberOfThreadsIn)
    : strategy(strategyIn), image(imageIn), startX(0), startY(0),
      width(widthIn), height(heightIn), numberOfThreads(numberOfThreadsIn)
{
    initialize();
}

RasterTileGenerator::RasterTileGenerator(
    RasterTileGenerationStrategy strategyIn,
    Image* imageIn,
    int startXIn,
    int startYIn,
    int widthIn,
    int heightIn,
    int numberOfThreadsIn)
    : strategy(strategyIn), image(imageIn), startX(startXIn), startY(startYIn),
      width(widthIn), height(heightIn), numberOfThreads(numberOfThreadsIn)
{
    initialize();
}

void RasterTileGenerator::initialize()
{
    if ( image == 0 ) {
        Logger::reportMessage("RasterTileGenerator", Logger::ERROR, "RasterTileGenerator", "image can not be null");
        throw VSDKFatalException("image can not be null");
    }
    if ( startX < 0 || startY < 0 ) {
        Logger::reportMessage("RasterTileGenerator", Logger::ERROR, "RasterTileGenerator", "origin must be >= 0");
        throw VSDKFatalException("origin must be >= 0");
    }
    if ( width <= 0 || height <= 0 ) {
        Logger::reportMessage("RasterTileGenerator", Logger::ERROR, "RasterTileGenerator", "width/height must be > 0");
        throw VSDKFatalException("width/height must be > 0");
    }
    if ( startX + width > image->getXSize() || startY + height > image->getYSize() ) {
        Logger::reportMessage("RasterTileGenerator", Logger::ERROR, "RasterTileGenerator", "requested tile area must be inside image");
        throw VSDKFatalException("requested tile area must be inside image");
    }
    if ( numberOfThreads <= 0 ) {
        Logger::reportMessage("RasterTileGenerator", Logger::ERROR, "RasterTileGenerator", "numberOfThreads must be > 0");
        throw VSDKFatalException("numberOfThreads must be > 0");
    }
    tiles = generateTiles();
}

const ArrayList<RasterTileArea>& RasterTileGenerator::getTiles() const {
    return tiles;
}

ArrayList<RasterTileArea> RasterTileGenerator::generateTiles() const
{
    if ( strategy == RasterTileGenerationStrategy::LINEAR ) return generateLinearTiles();
    return generateSerialTile();
}

ArrayList<RasterTileArea> RasterTileGenerator::generateLinearTiles() const
{
    ArrayList<RasterTileArea> out;
    int workerBands = numberOfThreads;
    if ( workerBands > height ) workerBands = height;
    int baseBandHeight = height / workerBands;
    int extraRows = height % workerBands;
    int y = 0;

    out.reserve((long int)workerBands);
    for ( int i = 0; i < workerBands; i++ ) {
        int currentBandHeight = baseBandHeight + (i < extraRows ? 1 : 0);
        out.add(RasterTileArea(image, startX, startY + y, width, currentBandHeight));
        y += currentBandHeight;
    }
    return out;
}

ArrayList<RasterTileArea> RasterTileGenerator::generateSerialTile() const
{
    ArrayList<RasterTileArea> out;
    out.reserve(1);
    out.add(RasterTileArea(image, startX, startY, width, height));
    return out;
}
