#ifndef __RASTER_TILE_GENERATION_STRATEGY__
#define __RASTER_TILE_GENERATION_STRATEGY__

/**
C++98 replacement for a scoped enumeration: values are accessed as
`RasterTileGenerationStrategy::VALUE` and objects convert implicitly to the plain `Value` enum,
so they can be compared and used in `switch` statements.
*/
class RasterTileGenerationStrategy {
  public:
    enum Value {
        LINEAR,
        SERIAL
    };

    RasterTileGenerationStrategy() : value(LINEAR) {}
    RasterTileGenerationStrategy(Value inValue) : value(inValue) {}
    explicit RasterTileGenerationStrategy(int inValue) : value((Value)inValue) {}
    operator Value() const { return value; }

  private:
    Value value;
};

#endif
