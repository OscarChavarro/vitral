#ifndef __CONSTRUCT__
#define __CONSTRUCT__

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationSegment.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoid.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoidQueryNode.h"
const int T_X = 1;
const int T_Y = 2;
const int T_SINK = 3;

const int SEGMENT_SIZE = 200;
const int QSIZE = 8 * SEGMENT_SIZE;
const int TRAPEZOID_TABLE_SIZE = 4 * SEGMENT_SIZE;

const int SEGMENT_FIRST_ENDPOINT = 1;
const int SEGMENT_LAST_ENDPOINT = 2;

const int TRIANGULATION_INFINITY = 1 << 30;

const double TRIANGULATOR_EPSILON = 1.0e-7;

const int S_LEFT = 1;
const int S_RIGHT = 2;

const int ST_VALID = 1;
const int ST_INVALID = 2;

class _Construct {
  public:
    static int nextQueryNodeIndex;
    static int nextTrapezoidIndex;
    static ArrayList<_TriangulationTrapezoidQueryNode *> queryNodes;
    static ArrayList<_TriangulationTrapezoid *> trapezoids;
    static ArrayList<_TriangulationSegment> segments;
    static void resetTrapezoidQueryNodes();
    static void resetTrapezoids();
    static void prepareStorage(int nseg);

  private:
    static int max(Vector2Dd *yvalPoint, Vector2Dd *firstPoint,
                   Vector2Dd *secondPoint);
    static int min(Vector2Dd *yvalPoint, Vector2Dd *firstPoint,
                   Vector2Dd *secondPoint);
    static _TriangulationTrapezoidQueryNode *initQueryStructure(int segmentIndex);
    static int findNewRoots(int segmentIndex);

  public:
    static bool greaterThan(Vector2Dd *firstPoint, Vector2Dd *secondPoint);
    static bool equalTo(Vector2Dd *firstPoint, Vector2Dd *secondPoint);
    static bool greaterThanEqualTo(Vector2Dd *firstPoint,
                                   Vector2Dd *secondPoint);
    static bool lessThan(Vector2Dd *firstPoint, Vector2Dd *secondPoint);
    static double cross(const Vector2Dd &v0, const Vector2Dd &v1,
                        const Vector2Dd &v2);
    static double dot(const Vector2Dd &v0, const Vector2Dd &v1);
    static bool fpEqual(double firstValue, double secondValue);
    static int constructTrapezoids(int nseg);
    static _TriangulationSegment &segmentAt(int index);
    static _TriangulationTrapezoid *trapezoidAt(int index);
    static void setTrapezoidAt(int index, _TriangulationTrapezoid *trapezoid);
    static _TriangulationTrapezoidQueryNode *allocateQueryNode();
    static int allocateTrapezoidIndex();
};

#endif
