#ifndef ___MONOTONECHAINNODE__
#define ___MONOTONECHAINNODE__

class _MonotoneChainNode {
  public:
    int vertexIndex;
    int nextNodeIndex;
    int previousNodeIndex;
    bool isMarked;
};

#endif
