#ifndef __CSG_RAY_EVENT_REFERENCE__
#define __CSG_RAY_EVENT_REFERENCE__
// Indexes into the input leaf batch and that leaf's boundary array.
class CsgRayEventReference {
public:
    long int leafIndex;
    long int boundaryIndex;
    CsgRayEventReference();
    CsgRayEventReference(long int leaf, long int boundary);
};
#endif
