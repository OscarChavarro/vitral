#ifndef __INCREMENTAL_SEGMENT_INSERTER__
#define __INCREMENTAL_SEGMENT_INSERTER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationSegment.h"
class _IncrementalSegmentInserter {
  public:
    static int addSegment(int segmentIndex);

  private:
    static void normalizeSegmentForInsertion(_TriangulationSegment &segment, BOOLEAN &isSwapped);
    static void updateLowerNeighbourLinksAfterSplit(int splitTrapIndex,
                                                    int originalUpperTrapIndex);
    static int splitTrapezoidAtEndpoint(int segmentIndex, _TriangulationSegment &segment,
                                        BOOLEAN useFirstEndpoint);
    static int locateOrInsertEndpointTrapezoid(int segmentIndex, _TriangulationSegment &segment,
                                               BOOLEAN useFirstEndpoint,
                                               BOOLEAN endpointAlreadyInserted,
                                               BOOLEAN &wasEndpointInserted);
    static BOOLEAN isLeftOf(int segmentIndex, Vector2Dd *queryPoint);
    static BOOLEAN inserted(int segmentIndex, int whichPoint);
    static int mergeTrapezoids(int segmentIndex, int tfirst, int tlast,
                               int side);
};

#endif
