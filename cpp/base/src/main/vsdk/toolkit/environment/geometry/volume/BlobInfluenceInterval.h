#ifndef __BLOB_INFLUENCE_INTERVAL__
#define __BLOB_INFLUENCE_INTERVAL__

// Start or end, along a ray, of the influence sphere of one blob component.
class BlobInfluenceInterval {
private:
    bool starting;
    long int component;
    double bound;
public:
    BlobInfluenceInterval() : starting(true), component(0), bound(0) {}
    BlobInfluenceInterval(bool isStart, long int componentIndex, double distance) :
        starting(isStart), component(componentIndex), bound(distance) {}
    bool isStart() const { return starting; }
    long int getComponent() const { return component; }
    double getBound() const { return bound; }
};
#endif
