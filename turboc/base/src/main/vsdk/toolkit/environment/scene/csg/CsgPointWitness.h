#ifndef __CSG_POINT_WITNESS__
#define __CSG_POINT_WITNESS__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
class CsgOperandView;
// Borrowed root-to-leaf path for a single operand proving a point result.
// Reversal records difference orientation, not a certified boundary hit.
class CsgPointWitness {
private:
    ArrayList<const CsgOperandView*> path;
    BOOLEAN reversed;
public:
    CsgPointWitness();
    CsgPointWitness(const CsgPointWitness& other);
    CsgPointWitness& operator=(const CsgPointWitness& other);
    ~CsgPointWitness();
    void clear();
    BOOLEAN prepend(const CsgOperandView* operand);
    void reverseOrientation();
    BOOLEAN isAvailable() const;
    BOOLEAN isReversed() const;
    long int getLength() const;
    const CsgOperandView* getOperand(long int index) const;
};
#endif
