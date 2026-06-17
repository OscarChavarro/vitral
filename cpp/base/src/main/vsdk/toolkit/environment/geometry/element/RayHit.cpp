#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"

static const Vector3Dd ZERO_VECTOR;

RayHit::RayHit() : RayHit(DETAIL_ALL, true) {}

RayHit::RayHit(int requiredDetailMask) : RayHit(requiredDetailMask, true) {}

RayHit::RayHit(int requiredDetailMask, bool storeRay)
    : p(ZERO_VECTOR), n(ZERO_VECTOR), t(ZERO_VECTOR),
      u(0), v(0),
      material(nullptr), texture(nullptr), normalMap(nullptr),
      rayValue_(),
      hasRay_(false),
      requiredDetailMask_(requiredDetailMask),
      storeRay_(storeRay),
      hitDistance_(0),
      hasHitDistance_(false)
{
    clear();
    RaytraceStatistics::recordRayHitInstance();
}

RayHit::RayHit(const RayHit& other)
    : p(ZERO_VECTOR), n(ZERO_VECTOR), t(ZERO_VECTOR),
      u(0), v(0),
      material(nullptr), texture(nullptr), normalMap(nullptr),
      rayValue_(),
      hasRay_(false),
      requiredDetailMask_(other.requiredDetailMask_),
      storeRay_(other.storeRay_),
      hitDistance_(0),
      hasHitDistance_(false)
{
    clone(other);
}

RayHit::~RayHit()
{
}

void RayHit::clear()
{
    p = ZERO_VECTOR;
    n = ZERO_VECTOR;
    t = ZERO_VECTOR;
    u = 0;
    v = 0;
    material = nullptr;
    texture = nullptr;
    normalMap = nullptr;
    hasRay_ = false;
    hitDistance_ = 0;
    hasHitDistance_ = false;
}

void RayHit::reset(int newRequiredDetailMask)
{
    requiredDetailMask_ = newRequiredDetailMask;
    clear();
}

void RayHit::resetForDistanceOnly()
{
    requiredDetailMask_ = DETAIL_NONE;
    hasRay_ = false;
    hitDistance_ = 0;
    hasHitDistance_ = false;
}

void RayHit::clone(const RayHit& other)
{
    RaytraceStatistics::recordHitInfoClone();
    requiredDetailMask_ = other.requiredDetailMask_;
    storeRay_ = other.storeRay_;
    hitDistance_ = other.hitDistance_;
    hasHitDistance_ = other.hasHitDistance_;
    p = other.p;
    n = other.n;
    t = other.t;
    u = other.u;
    v = other.v;
    material = other.material;
    texture = other.texture;
    normalMap = other.normalMap;

    hasRay_ = other.hasRay_;
    if ( hasRay_ ) {
        rayValue_ = other.rayValue_;
    }
}

int RayHit::requiredDetailMask() const
{
    return requiredDetailMask_;
}

void RayHit::setRequiredDetailMask(int requiredDetailMask)
{
    requiredDetailMask_ = requiredDetailMask;
}

bool RayHit::shouldStoreRay() const
{
    return storeRay_;
}

void RayHit::setStoreRay(bool storeRay)
{
    storeRay_ = storeRay;
}

bool RayHit::needsPoint() const
{
    return (requiredDetailMask_ & DETAIL_POINT) != 0;
}

bool RayHit::needsNormal() const
{
    return (requiredDetailMask_ & DETAIL_NORMAL) != 0;
}

bool RayHit::needsTextureCoordinates() const
{
    return (requiredDetailMask_ & DETAIL_UV) != 0;
}

bool RayHit::needsTangent() const
{
    return (requiredDetailMask_ & DETAIL_TANGENT) != 0;
}

bool RayHit::needsAnySurfaceData() const
{
    return requiredDetailMask_ != DETAIL_NONE;
}

const Ray* RayHit::ray() const
{
    return hasRay_ ? &rayValue_ : nullptr;
}

void RayHit::setRay(const Ray& ray)
{
    rayValue_ = ray;
    hasRay_ = true;
    hitDistance_ = ray.getT();
    hasHitDistance_ = true;
}

bool RayHit::hasHitDistance() const
{
    return hasHitDistance_;
}

double RayHit::hitDistance() const
{
    if ( hasHitDistance_ ) {
        return hitDistance_;
    }
    if ( hasRay_ ) {
        return rayValue_.getT();
    }
    return 0;
}

void RayHit::setHitDistance(double hitDistance)
{
    hitDistance_ = hitDistance;
    hasHitDistance_ = true;
}
