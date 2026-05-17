#include "RayHit.h"
#include "Ray.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"

static const Vector3Dd ZERO_VECTOR;

RayHit::RayHit() : RayHit(DETAIL_ALL, true) {}

RayHit::RayHit(int requiredDetailMask) : RayHit(requiredDetailMask, true) {}

RayHit::RayHit(int requiredDetailMask, bool storeRay)
    : p(ZERO_VECTOR), n(ZERO_VECTOR), t(ZERO_VECTOR),
      u(0), v(0),
      material(nullptr), texture(nullptr), normalMap(nullptr),
      ray_(nullptr),
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
      ray_(nullptr),
      requiredDetailMask_(other.requiredDetailMask_),
      storeRay_(other.storeRay_),
      hitDistance_(0),
      hasHitDistance_(false)
{
    clone(other);
}

RayHit::~RayHit()
{
    if ( ray_ != nullptr ) {
        delete ray_;
        ray_ = nullptr;
    }
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
    if ( ray_ != nullptr ) {
        delete ray_;
        ray_ = nullptr;
    }
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
    if ( ray_ != nullptr ) {
        delete ray_;
        ray_ = nullptr;
    }
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

    if ( ray_ != nullptr ) {
        delete ray_;
        ray_ = nullptr;
    }
    if ( other.ray_ != nullptr ) {
        ray_ = new Ray(*other.ray_);
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
    return ray_;
}

void RayHit::setRay(const Ray& ray)
{
    if ( ray_ != nullptr ) {
        delete ray_;
    }
    ray_ = new Ray(ray);
    hitDistance_ = ray.t();
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
    if ( ray_ != nullptr ) {
        return ray_->t();
    }
    return 0;
}

void RayHit::setHitDistance(double hitDistance)
{
    hitDistance_ = hitDistance;
    hasHitDistance_ = true;
}
