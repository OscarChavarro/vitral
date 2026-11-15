#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgPointAttribution.h"
CsgPointAttribution::CsgPointAttribution() : owners(4), complete(FALSE) {}
CsgPointAttribution::CsgPointAttribution(const CsgPointAttribution& other)
    : owners(other.owners), complete(other.complete), versions(other.versions) {}
CsgPointAttribution& CsgPointAttribution::operator=(const CsgPointAttribution& other)
{ if (this!=&other) { owners=other.owners; complete=other.complete; versions=other.versions; } return *this; }
CsgVersionStamp& CsgPointAttribution::getVersions() { return versions; }
const CsgVersionStamp& CsgPointAttribution::getVersions() const { return versions; }
CsgPointAttribution::~CsgPointAttribution() {}
void CsgPointAttribution::clear() { owners.clear(); complete=FALSE; versions.clear(); }
void CsgPointAttribution::begin() { owners.clear(); complete=TRUE; versions.clear(); }
BOOLEAN CsgPointAttribution::add(const CsgPointWitness& owner)
{ if (!complete || !owner.isAvailable() || !owners.add(owner)) { clear(); return FALSE; } return TRUE; }
BOOLEAN CsgPointAttribution::append(const CsgPointAttribution& other)
{
    if (!complete || !other.isComplete()) { clear(); return FALSE; }
    for (long int i=0;i<other.owners.size();++i) if (!owners.add(other.owners[i])) { clear(); return FALSE; }
    return TRUE;
}
BOOLEAN CsgPointAttribution::prepend(const CsgOperandView* operand)
{
    if (!complete) return FALSE;
    for (long int i=0;i<owners.size();++i) if (!owners[i].prepend(operand)) { clear(); return FALSE; }
    return TRUE;
}
void CsgPointAttribution::reverseOrientation()
{ if (complete) for (long int i=0;i<owners.size();++i) owners[i].reverseOrientation(); }
BOOLEAN CsgPointAttribution::isComplete() const { return complete && owners.size()>0; }
long int CsgPointAttribution::getCount() const { return isComplete() ? owners.size() : 0; }
const CsgPointWitness* CsgPointAttribution::getOwner(long int index) const
{ return isComplete() && index>=0 && index<owners.size() ? &owners[index] : (const CsgPointWitness*)NULL; }
