#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/statistics/SolidTextureStatistics.h"

SolidTextureStatistics::SolidTextureStatistics(
    ArrayList<SolidTextureStatistics *> *partsPerThread)
{
    reset();

    if (partsPerThread == NULL) {
        return;
    }

    for (long i = 0; i < partsPerThread->size(); ++i) {
        const SolidTextureStatistics *part = partsPerThread->get(i);
        if (part == NULL) {
            continue;
        }
        callsToNoise += part->callsToNoise;
        callsToDNoise += part->callsToDNoise;
    }
}
