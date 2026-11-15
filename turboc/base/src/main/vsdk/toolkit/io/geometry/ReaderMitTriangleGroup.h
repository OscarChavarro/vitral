#ifndef __READER_MIT_TRIANGLE_GROUP__
#define __READER_MIT_TRIANGLE_GROUP__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Triangle.h"
class SimpleMaterial;
class SimpleScene;
class SimpleBody;

// One MIT triangles block; material changes flush faces while keeping vertices.
class ReaderMitTriangleGroup {
private:
    ArrayList<Vector3Dd> vertices;
    ArrayList<Triangle> triangles;
public:
    ReaderMitTriangleGroup();
    ~ReaderMitTriangleGroup();
    void reset();
    BOOLEAN addVertex(const Vector3Dd& vertex);
    BOOLEAN addFace(const char* coordinates);
    // Adds the pending faces as one body and returns it, or null when none are pending.
    SimpleBody* flush(SimpleScene* scene, const SimpleMaterial* material);
};
#endif
