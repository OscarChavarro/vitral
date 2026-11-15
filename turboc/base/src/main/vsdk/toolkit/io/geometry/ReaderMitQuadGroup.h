#ifndef __READER_MIT_QUAD_GROUP__
#define __READER_MIT_QUAD_GROUP__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class SimpleBody;
class SimpleMaterial;
class SimpleScene;

// One MIT quads block: `v x y z` vertices and `q i0 i1 i2 i3` faces, built as a
// QuadMesh; material changes flush faces while keeping vertices.
class ReaderMitQuadGroup {
private:
    ArrayList<Vector3Dd> vertices;
    ArrayList<int> quadIndices;
public:
    ReaderMitQuadGroup();
    ~ReaderMitQuadGroup();
    void reset();
    BOOLEAN addVertex(const Vector3Dd& vertex);
    // Reads four vertex indices; false when malformed or out of range.
    BOOLEAN addQuad(const char* indices);
    // Adds the pending faces as one body and returns it, or null when none are pending.
    SimpleBody* flush(SimpleScene* scene, const SimpleMaterial* material);
};
#endif
