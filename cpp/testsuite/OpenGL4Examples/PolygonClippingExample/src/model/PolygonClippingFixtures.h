#ifndef __POLYGONCLIPPINGFIXTURES__
#define __POLYGONCLIPPINGFIXTURES__

struct PolygonClippingTestCase {
    const char* name;
    const char* clipLoops;
    const char* subjectLoops;
};

class PolygonClippingFixtures {
public:
    static const PolygonClippingTestCase CASES[];
    static const int COUNT;
};

#endif
