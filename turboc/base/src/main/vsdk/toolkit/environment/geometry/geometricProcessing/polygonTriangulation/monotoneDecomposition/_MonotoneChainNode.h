#ifndef __MONOTONE_CHAIN_NODE__
#define __MONOTONE_CHAIN_NODE__

#include "vsdk/toolkit/common/VSDK.h"

class _MonotoneChainNode {
  public:
    int vertexIndex;
    int nextNodeIndex;
    int previousNodeIndex;
    BOOLEAN isMarked;
};

#endif
