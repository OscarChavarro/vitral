#ifndef __TRNGLTN_TRPZD_QRY_ND__
#define __TRNGLTN_TRPZD_QRY_ND__

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
class _TriangulationTrapezoidQueryNode {
  public:
    int queryNodeType;
    int segmentIndex;
    Vector2Dd splitPoint;
    int trapezoidIndex;
    _TriangulationTrapezoidQueryNode *parent;
    _TriangulationTrapezoidQueryNode *leftChild;
    _TriangulationTrapezoidQueryNode *rightChild;

    _TriangulationTrapezoidQueryNode()
        : queryNodeType(0), segmentIndex(0), splitPoint(), trapezoidIndex(0),
          parent(NULL), leftChild(NULL), rightChild(NULL) {}
    int locateEndpoint(Vector2Dd *queryPoint, Vector2Dd *otherPoint);

  private:
    static BOOLEAN isLeftOf(int segmentIndex, Vector2Dd *queryPoint);
};

#endif
