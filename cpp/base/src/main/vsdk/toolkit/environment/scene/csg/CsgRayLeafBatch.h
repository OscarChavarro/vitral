#ifndef __CSG_RAY_LEAF_BATCH__
#define __CSG_RAY_LEAF_BATCH__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayLeaf.h"
// Borrowed leaf identities; owns snapshots of the classifications and paths.
// Leaves are pooled: clear keeps them, and acquire reuses their storage, so a
// batch reused across queries stops allocating once it has reached its size.
class CsgRayLeafBatch {
private:
    java::ArrayList<CsgRayLeaf*> pool;
    long int count;
public:
    CsgRayLeafBatch();
    CsgRayLeafBatch(const CsgRayLeafBatch& other);
    CsgRayLeafBatch& operator=(const CsgRayLeafBatch& other);
    ~CsgRayLeafBatch();
    void clear();
    bool add(const CsgRayLeaf& leaf);
    // Appends an empty leaf, or returns nullptr when storage is exhausted.
    CsgRayLeaf* acquire();
    long int size() const;
    CsgRayLeaf* get(long int index);
    const CsgRayLeaf* get(long int index) const;
};
#endif
