#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgRayLeafBatch.h"
CsgRayLeafBatch::CsgRayLeafBatch() : pool(4), count(0) {}
CsgRayLeafBatch::CsgRayLeafBatch(const CsgRayLeafBatch& other) : pool(4), count(0) { *this=other; }
CsgRayLeafBatch& CsgRayLeafBatch::operator=(const CsgRayLeafBatch& other)
{
    if (this!=&other) {
        clear();
        for (long int i=0;i<other.count;++i) add(*other.pool[i]);
    }
    return *this;
}
CsgRayLeafBatch::~CsgRayLeafBatch() { for (long int i=0;i<pool.size();++i) delete pool[i]; }
void CsgRayLeafBatch::clear() { count=0; }
CsgRayLeaf* CsgRayLeafBatch::acquire()
{
    if (count==pool.size()) {
        CsgRayLeaf* leaf=new CsgRayLeaf();
        if (!pool.add(leaf)) { delete leaf; return NULL; }
    }
    CsgRayLeaf* leaf=pool[count++];
    leaf->reset();
    return leaf;
}
BOOLEAN CsgRayLeafBatch::add(const CsgRayLeaf& leaf)
{
    CsgRayLeaf* slot=acquire();
    if (!slot) return FALSE;
    *slot=leaf;
    return TRUE;
}
long int CsgRayLeafBatch::size() const { return count; }
CsgRayLeaf* CsgRayLeafBatch::get(long int index) { return index>=0 && index<count ? pool[index] : (CsgRayLeaf*)NULL; }
const CsgRayLeaf* CsgRayLeafBatch::get(long int index) const { return index>=0 && index<count ? pool[index] : (const CsgRayLeaf*)NULL; }
