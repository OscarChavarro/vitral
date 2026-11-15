#ifndef __CSG_POINT_ATTRIBUTION__
#define __CSG_POINT_ATTRIBUTION__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPointWitness.h"
#include "vsdk/toolkit/environment/scene/csg/CsgVersionStamp.h"
class CsgOperandView;
// Complete set of certified boundary owners. Paths borrow scene identities;
// the version stamp tells whether they are still current.
class CsgPointAttribution {
private:
    ArrayList<CsgPointWitness> owners;
    BOOLEAN complete;
    CsgVersionStamp versions;
public:
    CsgPointAttribution();
    CsgPointAttribution(const CsgPointAttribution& other);
    CsgPointAttribution& operator=(const CsgPointAttribution& other);
    ~CsgPointAttribution();
    void clear();
    void begin();
    BOOLEAN add(const CsgPointWitness& owner);
    BOOLEAN append(const CsgPointAttribution& other);
    BOOLEAN prepend(const CsgOperandView* operand);
    void reverseOrientation();
    BOOLEAN isComplete() const;
    long int getCount() const;
    const CsgPointWitness* getOwner(long int index) const;
    CsgVersionStamp& getVersions();
    const CsgVersionStamp& getVersions() const;
};
#endif
