#ifndef __HEIGHT_FIELD_TRAVERSAL__
#define __HEIGHT_FIELD_TRAVERSAL__

#include "vsdk/toolkit/common/VSDK.h"

// Per-ray stepping state of the height field first-hit traversal: direction
// signs, the dominant axis and the slopes between ray coordinates.
class HeightFieldTraversal {
private:
    int isdx;
    int isdz;
    BOOLEAN xDom;
    double gdx;
    double gdy;
    double gdz;
    double myx;
    double mxz;
    double mzx;
    double myz;
    double depth;
public:
    HeightFieldTraversal(int isdxValue, int isdzValue, BOOLEAN xDomValue, double gdxValue,
        double gdyValue, double gdzValue, double myxValue, double mxzValue,
        double mzxValue, double myzValue) :
        isdx(isdxValue), isdz(isdzValue), xDom(xDomValue), gdx(gdxValue), gdy(gdyValue),
        gdz(gdzValue), myx(myxValue), mxz(mxzValue), mzx(mzxValue), myz(myzValue), depth(0) {}
    int getIsdx() const { return isdx; }
    int getIsdz() const { return isdz; }
    BOOLEAN getXDom() const { return xDom; }
    double getGdx() const { return gdx; }
    double getGdy() const { return gdy; }
    double getGdz() const { return gdz; }
    double getMyx() const { return myx; }
    double getMxz() const { return mxz; }
    double getMzx() const { return mzx; }
    double getMyz() const { return myz; }
    // Ray parameter of the hit found by the traversal.
    double getDepth() const { return depth; }
    void setDepth(double value) { depth = value; }
};
#endif
