#ifndef ___SEGMENTTABLEBUILDER__
#define ___SEGMENTTABLEBUILDER__

class _SegmentTableBuilder {
  public:
    static int prepareSegments(double *vertices, int numberOfVertices,
                               int *contourSizes, int numberOfContours);
};

#endif
