#include <stddef.h>
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
static const Vector3Dd ZERO_VECTOR;

RayHit::RayHit()
    : point(ZERO_VECTOR), normal(ZERO_VECTOR), tangent(ZERO_VECTOR),
      u(0), v(0),
      material(NULL), texture(NULL), normalMap(NULL),
      ray(),
      hasRay(false),
      requiredDetailMask(DETAIL_ALL),
      storeRay(true),
      hitDistance(0),
      hitDistanceKnown(false)
{
    clear();
    RaytraceStatistics::recordRayHitInstance();
}

RayHit::RayHit(int requiredDetailMask)
    : point(ZERO_VECTOR), normal(ZERO_VECTOR), tangent(ZERO_VECTOR),
      u(0), v(0),
      material(NULL), texture(NULL), normalMap(NULL),
      ray(),
      hasRay(false),
      requiredDetailMask(requiredDetailMask),
      storeRay(true),
      hitDistance(0),
      hitDistanceKnown(false)
{
    clear();
    RaytraceStatistics::recordRayHitInstance();
}

RayHit::RayHit(int requiredDetailMask, bool storeRay)
    : point(ZERO_VECTOR), normal(ZERO_VECTOR), tangent(ZERO_VECTOR),
      u(0), v(0),
      material(NULL), texture(NULL), normalMap(NULL),
      ray(),
      hasRay(false),
      requiredDetailMask(requiredDetailMask),
      storeRay(storeRay),
      hitDistance(0),
      hitDistanceKnown(false)
{
    clear();
    RaytraceStatistics::recordRayHitInstance();
}

RayHit::RayHit(const RayHit& other)
    : point(ZERO_VECTOR), normal(ZERO_VECTOR), tangent(ZERO_VECTOR),
      u(0), v(0),
      material(NULL), texture(NULL), normalMap(NULL),
      ray(),
      hasRay(false),
      requiredDetailMask(other.requiredDetailMask),
      storeRay(other.storeRay),
      hitDistance(0),
      hitDistanceKnown(false)
{
    clone(other);
}

RayHit::~RayHit()
{
}

void RayHit::clear()
{
    point = ZERO_VECTOR;
    normal = ZERO_VECTOR;
    tangent = ZERO_VECTOR;
    u = 0;
    v = 0;
    material = NULL;
    texture = NULL;
    normalMap = NULL;
    hasRay = false;
    hitDistance = 0;
    hitDistanceKnown = false;
}

void RayHit::reset(int newRequiredDetailMask)
{
    requiredDetailMask = newRequiredDetailMask;
    clear();
}

void RayHit::resetForDistanceOnly()
{
    requiredDetailMask = DETAIL_NONE;
    hasRay = false;
    hitDistance = 0;
    hitDistanceKnown = false;
}

void RayHit::clone(const RayHit& other)
{
    RaytraceStatistics::recordHitInfoClone();
    requiredDetailMask = other.requiredDetailMask;
    storeRay = other.storeRay;
    hitDistance = other.hitDistance;
    hitDistanceKnown = other.hitDistanceKnown;
    point = other.point;
    normal = other.normal;
    tangent = other.tangent;
    u = other.u;
    v = other.v;
    material = other.material;
    texture = other.texture;
    normalMap = other.normalMap;

    hasRay = other.hasRay;
    if ( hasRay ) {
        ray = other.ray;
    }
}

int RayHit::getRequiredDetailMask() const
{
    return requiredDetailMask;
}

void RayHit::setRequiredDetailMask(int value)
{
    requiredDetailMask = value;
}

bool RayHit::shouldStoreRay() const
{
    return storeRay;
}

void RayHit::setStoreRay(bool value)
{
    storeRay = value;
}

bool RayHit::needsPoint() const
{
    return (requiredDetailMask & DETAIL_POINT) != 0;
}

bool RayHit::needsNormal() const
{
    return (requiredDetailMask & DETAIL_NORMAL) != 0;
}

bool RayHit::needsTextureCoordinates() const
{
    return (requiredDetailMask & DETAIL_UV) != 0;
}

bool RayHit::needsTangent() const
{
    return (requiredDetailMask & DETAIL_TANGENT) != 0;
}

bool RayHit::needsAnySurfaceData() const
{
    return requiredDetailMask != DETAIL_NONE;
}

const Ray* RayHit::getRay() const
{
    return hasRay ? &ray : NULL;
}

void RayHit::setRay(const Ray& value)
{
    ray = value;
    hasRay = true;
    hitDistance = value.getT();
    hitDistanceKnown = true;
}

bool RayHit::hasHitDistance() const
{
    return hitDistanceKnown;
}

double RayHit::getHitDistance() const
{
    if ( hitDistanceKnown ) {
        return hitDistance;
    }
    if ( hasRay ) {
        return ray.getT();
    }
    return 0;
}

void RayHit::setHitDistance(double value)
{
    hitDistance = value;
    hitDistanceKnown = true;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int RayHit::DETAIL_ALL;

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int RayHit::DETAIL_NONE;
const int RayHit::DETAIL_POINT;
const int RayHit::DETAIL_NORMAL;
const int RayHit::DETAIL_UV;
const int RayHit::DETAIL_TANGENT;
