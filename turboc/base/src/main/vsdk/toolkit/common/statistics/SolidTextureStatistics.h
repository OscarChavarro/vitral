#ifndef __SOLID_TEXTURE_STATISTICS__
#define __SOLID_TEXTURE_STATISTICS__

#include "java/util/ArrayList.h"

class SolidTextureStatistics {
  public:
    long callsToNoise;
    long callsToDNoise;

    SolidTextureStatistics() : callsToNoise(0L), callsToDNoise(0L) {}

    // Supports multi-thread rendering: each RenderTask binds its own
    // ProceduralNoise to its own SolidTextureStatistics instance (so noise()/
    // differentialNoise() call counters never race across threads), then
    // this reduces every task's instance into one total once all tasks have
    // joined. Mirrors Statistics(ArrayList<Statistics*>*).
    explicit SolidTextureStatistics(
        ArrayList<SolidTextureStatistics *> *partsPerThread);

    void reset() { callsToNoise = 0L; callsToDNoise = 0L; }
};

#endif
