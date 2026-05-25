#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include <cmath>

Cone::Cone(double inR1, double inR2, double inH) : r1(inR1), r2(inR2), h(inH) {}

double Cone::sq(double v){ return v*v; }
bool Cone::approxEq(double a, double b){ return std::abs(a-b) <= VSDK::EPSILON; }

double Cone::getBaseRadius() const { return r1; }
double Cone::getTopRadius() const { return r2; }
double Cone::getHeight() const { return h; }
void Cone::setBaseRadius(double val) { r1 = val; }
void Cone::setTopRadius(double val) { r2 = val; }
void Cone::setHeight(double val) { h = val; }

Ray* Cone::doIntersectionCylinder(const Ray& inOutRay, double inR, double inH, RayHit* outInfo) {
    double ox=inOutRay.origin().x(), oy=inOutRay.origin().y(), oz=inOutRay.origin().z();
    double dx=inOutRay.direction().x(), dy=inOutRay.direction().y(), dz=inOutRay.direction().z();
    double A = sq(dx)+sq(dy); if (std::abs(A) <= VSDK::EPSILON) return nullptr;
    double B = 2*((dx*ox)+(dy*oy));
    double C = sq(ox)+sq(oy)-sq(inR);
    double disc = sq(B)-4*A*C; if (disc <= VSDK::EPSILON) return nullptr;
    double t0 = (-B-std::sqrt(disc))/(2*A);
    if (t0 > VSDK::EPSILON) {
        double pz = oz + dz*t0;
        if (pz > inH || pz < 0) return nullptr;
        if (outInfo != nullptr) {
            double px=ox+dx*t0, py=oy+dy*t0;
            outInfo->p = Vector3Dd(px,py,pz);
            outInfo->n = Vector3Dd(px,py,0).normalized();
        }
        Ray hRay = inOutRay.withT(t0);
        return new Ray(hRay);
    }
    return nullptr;
}

Ray* Cone::doIntersectionCone(const Ray& inOutRay, double inR, double inH, RayHit* outInfo) {
    double ox=inOutRay.origin().x(), oy=inOutRay.origin().y(), oz=inOutRay.origin().z();
    double dx=inOutRay.direction().x(), dy=inOutRay.direction().y(), dz=inOutRay.direction().z();
    if (inH <= VSDK::EPSILON) return nullptr;
    double shiftedOz = oz - inH;
    double ratio = inR / inH;
    double ratioSquared = sq(ratio);
    double A = sq(dx)+sq(dy)-sq(dz*ratio); if (std::abs(A)<=VSDK::EPSILON) return nullptr;
    double B = 2*((dx*ox)+(dy*oy)-(dz*shiftedOz*ratioSquared));
    double C = sq(ox)+sq(oy)-sq(shiftedOz*ratio);
    double disc = sq(B)-4*A*C; if (disc <= VSDK::EPSILON) return nullptr;
    double t0 = (-B-std::sqrt(disc))/(2*A);
    if (t0 > VSDK::EPSILON) {
        double shiftedPz = shiftedOz + dz*t0;
        if (shiftedPz > 0 || shiftedPz < -inH) return nullptr;
        if (outInfo != nullptr) {
            double px=ox+dx*t0, py=oy+dy*t0;
            outInfo->p = Vector3Dd(px, py, shiftedPz + inH);
            outInfo->n = Vector3Dd(px, py, -shiftedPz * ratioSquared).normalized();
        }
        Ray hRay = inOutRay.withT(t0);
        return new Ray(hRay);
    }
    return nullptr;
}

Ray* Cone::doIntersectionTap(const Ray& inOutRay, double inR, double inH, RayHit* outInfo) {
    double dx=inOutRay.direction().x(), dy=inOutRay.direction().y(), dz=inOutRay.direction().z();
    double ox=inOutRay.origin().x(), oy=inOutRay.origin().y(), oz=inOutRay.origin().z();
    if (std::abs(dz) > VSDK::EPSILON) {
        double t=(inH-oz)/dz;
        if (t > VSDK::EPSILON) {
            double px=ox+dx*t, py=oy+dy*t;
            if (sq(px)+sq(py) < sq(inR)) {
                if (outInfo != nullptr) {
                    outInfo->n = Vector3Dd(0,0,1);
                    outInfo->p = Vector3Dd(px,py,inH);
                }
                Ray hRay = inOutRay.withT(t);
                return new Ray(hRay);
            }
        }
    }
    return nullptr;
}

Ray* Cone::doIntersection(const Ray& inOutRay) {
    RayHit hit;
    if (doIntersection(inOutRay, &hit) && hit.ray() != nullptr) {
        return new Ray(*hit.ray());
    }
    return nullptr;
}

bool Cone::doIntersection(const Ray& inOutRay, RayHit* outHit) {
    RayHit infoTap1, infoTap2, infoBody;
    Ray* bodyHit = nullptr;
    Ray* tap1Hit = nullptr;
    Ray* tap2Hit = nullptr;
    Ray* winner = nullptr;
    RayHit* winnerInfo = nullptr;

    if (r2 < VSDK::EPSILON && r1 > VSDK::EPSILON) {
        bodyHit = doIntersectionCone(inOutRay, r1, h, &infoBody);
        tap1Hit = doIntersectionTap(inOutRay, r1, 0, &infoTap1);
        if ((tap1Hit != nullptr && bodyHit == nullptr) ||
            (tap1Hit != nullptr && bodyHit != nullptr && tap1Hit->t() < bodyHit->t())) {
            infoTap1.n = infoTap1.n.multiply(-1);
            winner = new Ray(inOutRay.withT(tap1Hit->t()));
            winnerInfo = &infoTap1;
        }
        else if (bodyHit != nullptr) {
            winner = new Ray(inOutRay.withT(bodyHit->t()));
            winnerInfo = &infoBody;
        }
    }
    else if (approxEq(r1, r2)) {
        int nearest = -1;
        bodyHit = doIntersectionCylinder(inOutRay, r1, h, &infoBody);
        tap1Hit = doIntersectionTap(inOutRay, r1, 0, &infoTap1);
        tap2Hit = doIntersectionTap(inOutRay, r1, h, &infoTap2);

        if (bodyHit != nullptr &&
            ((tap1Hit != nullptr && bodyHit->t() < tap1Hit->t()) || tap1Hit == nullptr) &&
            ((tap2Hit != nullptr && bodyHit->t() < tap2Hit->t()) || tap2Hit == nullptr)) nearest = 1;
        else if (tap1Hit != nullptr &&
            ((bodyHit != nullptr && tap1Hit->t() < bodyHit->t()) || bodyHit == nullptr) &&
            ((tap2Hit != nullptr && tap1Hit->t() < tap2Hit->t()) || tap2Hit == nullptr)) nearest = 3;
        else if (tap2Hit != nullptr) nearest = 2;

        if (nearest == 1) { winner = new Ray(inOutRay.withT(bodyHit->t())); winnerInfo = &infoBody; }
        else if (nearest == 2) { winner = new Ray(inOutRay.withT(tap2Hit->t())); winnerInfo = &infoTap2; }
        else if (nearest == 3) { winner = new Ray(inOutRay.withT(tap1Hit->t())); winnerInfo = &infoTap1; }
    }

    if (bodyHit) delete bodyHit;
    if (tap1Hit) delete tap1Hit;
    if (tap2Hit) delete tap2Hit;

    if (winner == nullptr) return false;

    if (outHit != nullptr) {
        outHit->setRay(*winner);
        outHit->p = Vector3Dd(winnerInfo->p);
        outHit->n = Vector3Dd(winnerInfo->n).normalized();
        outHit->t = Vector3Dd(winnerInfo->t);
        outHit->u = winnerInfo->u;
        outHit->v = winnerInfo->v;
        outHit->material = winnerInfo->material;
        outHit->texture = winnerInfo->texture;
        outHit->normalMap = winnerInfo->normalMap;
    }
    delete winner;
    return true;
}

void Cone::doExtraInformation(const Ray& inRay, double, RayHit* outData) {
    if (outData == nullptr) return;
    RayHit hit;
    if (doIntersection(inRay.withT(1e308), &hit)) {
        outData->clone(hit);
    }
}

double* Cone::getMinMax() {
    double* m = new double[6];
    double r = (r1 > r2) ? r1 : r2;
    m[0]=-r; m[1]=-r; m[2]=0; m[3]=r; m[4]=r; m[5]=h;
    return m;
}
