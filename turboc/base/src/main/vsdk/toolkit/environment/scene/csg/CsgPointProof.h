#ifndef __CSG_POINT_PROOF__
#define __CSG_POINT_PROOF__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPointEvidence.h"
#include "vsdk/toolkit/environment/scene/csg/CsgVersionStamp.h"
class CsgOperandView;
// Sufficient leaf facts for a definite result, interpreted with the borrowed
// expression tree. Empty/unavailable means the result has no collected proof.
class CsgPointProof {
private:
    ArrayList<CsgPointEvidence> evidence;
    BOOLEAN available;
    CsgVersionStamp versions;
public:
    CsgPointProof();
    void clear();
    void begin();
    BOOLEAN add(const CsgPointEvidence& value);
    BOOLEAN append(const CsgPointProof& other);
    BOOLEAN prepend(const CsgOperandView* operand);
    void reverseOrientation();
    BOOLEAN isAvailable() const;
    long int getCount() const;
    const CsgPointEvidence* getEvidence(long int index) const;
    // Versions of the borrowed expression, captured by the top-level query.
    CsgVersionStamp& getVersions();
    const CsgVersionStamp& getVersions() const;
};
#endif
