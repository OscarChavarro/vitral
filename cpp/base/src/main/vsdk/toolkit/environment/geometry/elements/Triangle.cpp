#include "Triangle.h"

Triangle::Triangle() : p0(0), p1(0), p2(0), normal(0, 0, 0) {}
Triangle::Triangle(int inP0, int inP1, int inP2)
    : p0(inP0), p1(inP1), p2(inP2), normal(0, 0, 0) {}

int Triangle::getPoint0() const { return p0; }
int Triangle::getPoint1() const { return p1; }
int Triangle::getPoint2() const { return p2; }

void Triangle::setPoint0(int inP0) { p0 = inP0; }
void Triangle::setPoint1(int inP1) { p1 = inP1; }
void Triangle::setPoint2(int inP2) { p2 = inP2; }

std::string Triangle::toString() const
{
    return "f < " + std::to_string(p0) + ", " +
        std::to_string(p1) + ", " +
        std::to_string(p2) + " >";
}
