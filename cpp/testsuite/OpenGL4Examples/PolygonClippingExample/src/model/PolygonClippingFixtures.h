#ifndef POLYGONCLIPPING_FIXTURES_H
#define POLYGONCLIPPING_FIXTURES_H

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
