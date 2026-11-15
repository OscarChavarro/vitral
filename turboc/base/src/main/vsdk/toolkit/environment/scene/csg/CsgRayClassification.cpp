#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgRayClassification.h"
CsgRayClassification::CsgRayClassification() : owners(8), ownerStarts(8) {}
CsgRayClassification::CsgRayClassification(const CsgRayClassification& other) : owners(8), ownerStarts(8)
{ *this=other; }
CsgRayClassification& CsgRayClassification::operator=(const CsgRayClassification& other)
{
    if (this!=&other) { long int i;
        leaves=other.leaves; solid=other.solid; versions=other.versions;
        owners.clear(); ownerStarts.clear();
        for (i =0;i<other.owners.size();++i) owners.add(other.owners[i]);
        for (i =0;i<other.ownerStarts.size();++i) ownerStarts.add(other.ownerStarts[i]);
    }
    return *this;
}
CsgRayClassification::~CsgRayClassification() {}
void CsgRayClassification::clear() { leaves.clear(); solid.clear(); owners.clear(); ownerStarts.clear(); versions.clear(); }
CsgRayLeafBatch& CsgRayClassification::getLeaves() { return leaves; }
const CsgRayLeafBatch& CsgRayClassification::getLeaves() const { return leaves; }
RaySolidClassification& CsgRayClassification::getSolid() { return solid; }
const RaySolidClassification& CsgRayClassification::getSolid() const { return solid; }
CsgVersionStamp& CsgRayClassification::getVersions() { return versions; }
const CsgVersionStamp& CsgRayClassification::getVersions() const { return versions; }
BOOLEAN CsgRayClassification::beginOwners() { return ownerStarts.add(owners.size()); }
BOOLEAN CsgRayClassification::addOwner(const CsgRayEventReference& reference)
{ return ownerStarts.size()>0 && owners.add(reference); }
long int CsgRayClassification::getOwnerGroupCount() const { return ownerStarts.size(); }
long int CsgRayClassification::getOwnerCount(long int boundaryIndex) const
{
    if (boundaryIndex<0 || boundaryIndex>=ownerStarts.size()) return 0;
    const long long end=boundaryIndex+1<ownerStarts.size() ? ownerStarts[boundaryIndex+1] : owners.size();
    return ((long int)(end-ownerStarts[boundaryIndex]));
}
const CsgRayEventReference* CsgRayClassification::getOwner(long int boundaryIndex, long int ownerIndex) const
{
    if (ownerIndex<0 || ownerIndex>=getOwnerCount(boundaryIndex)) return NULL;
    return &owners[((long int)(ownerStarts[boundaryIndex]))+ownerIndex];
}
