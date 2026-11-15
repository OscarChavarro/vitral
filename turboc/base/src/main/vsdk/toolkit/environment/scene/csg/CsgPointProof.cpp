#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgPointProof.h"
CsgPointProof::CsgPointProof() : evidence(4), available(FALSE) {}
void CsgPointProof::clear() { evidence.clear(); available=FALSE; versions.clear(); }
void CsgPointProof::begin() { evidence.clear(); available=TRUE; versions.clear(); }
CsgVersionStamp& CsgPointProof::getVersions() { return versions; }
const CsgVersionStamp& CsgPointProof::getVersions() const { return versions; }
BOOLEAN CsgPointProof::add(const CsgPointEvidence& value)
{ if (!available || !evidence.add(value)) { clear(); return FALSE; } return TRUE; }
BOOLEAN CsgPointProof::append(const CsgPointProof& other)
{
    if (!available || !other.available) { clear(); return FALSE; }
    for (long int i=0;i<other.evidence.size();++i) if (!evidence.add(other.evidence[i])) { clear(); return FALSE; }
    return TRUE;
}
BOOLEAN CsgPointProof::prepend(const CsgOperandView* operand)
{
    if (!available) return FALSE;
    for (long int i=0;i<evidence.size();++i)
        if (!evidence[i].prepend(operand)) { clear(); return FALSE; }
    return TRUE;
}
void CsgPointProof::reverseOrientation()
{ if (available) for (long int i=0;i<evidence.size();++i) evidence[i].reverseOrientation(); }
BOOLEAN CsgPointProof::isAvailable() const { return available && evidence.size()>0; }
long int CsgPointProof::getCount() const { return isAvailable() ? evidence.size() : 0; }
const CsgPointEvidence* CsgPointProof::getEvidence(long int index) const
{ return isAvailable() && index>=0 && index<evidence.size() ? &evidence[index] : (const CsgPointEvidence*)NULL; }
