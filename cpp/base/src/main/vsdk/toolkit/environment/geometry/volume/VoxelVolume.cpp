#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/VSDK.h"
#include <cmath>

VoxelVolume::VoxelVolume() : data(nullptr), xSize(0), ySize(0), zSize(0) {}

VoxelVolume::~VoxelVolume() {
    if (data != nullptr) {
        for (int z = 0; z < zSize; z++) {
            delete data[z];
        }
        delete[] data;
    }
}

int VoxelVolume::getXSize() const { return xSize; }
int VoxelVolume::getYSize() const { return ySize; }
int VoxelVolume::getZSize() const { return zSize; }

bool VoxelVolume::init(int xs, int ys, int zs) {
    if (xs <= 0 || ys <= 0 || zs <= 0) return false;

    if (data != nullptr) {
        for (int z = 0; z < zSize; z++) delete data[z];
        delete[] data;
        data = nullptr;
    }

    data = new IndexedColorImageUncompressed*[zs];
    xSize = xs; ySize = ys; zSize = zs;

    for (int z = 0; z < zSize; z++) {
        data[z] = new IndexedColorImageUncompressed();
        if (!data[z]->init(xSize, ySize)) {
            for (int i = 0; i <= z; i++) delete data[i];
            delete[] data;
            data = nullptr;
            xSize = ySize = zSize = 0;
            return false;
        }
    }
    return true;
}

void VoxelVolume::putVoxel(int x, int y, int z, char val) {
    if (data == nullptr) return;
    if (x < 0 || x >= xSize || y < 0 || y >= ySize || z < 0 || z >= zSize) return;
    data[z]->putPixel(x, y, val);
}

int VoxelVolume::getVoxel(int x, int y, int z) const {
    if (data == nullptr) return 0;
    if (x < 0 || x >= xSize || y < 0 || y >= ySize || z < 0 || z >= zSize) return 0;
    return (unsigned char)data[z]->getPixel(x, y);
}

Vector3Dd VoxelVolume::getVoxelPosition(int x, int y, int z) const {
    return Vector3Dd(
        (((double)x + 0.5) / (double)xSize) * 2 - 1,
        (((double)y + 0.5) / (double)ySize) * 2 - 1,
        (((double)z + 0.5) / (double)zSize) * 2 - 1);
}

int VoxelVolume::getNearestIFromX(double x) const { return (int)(((x + 1)/2) * ((double)xSize) - 0.5); }
int VoxelVolume::getNearestJFromY(double y) const { return (int)(((y + 1)/2) * ((double)ySize) - 0.5); }
int VoxelVolume::getNearestKFromZ(double z) const { return (int)(((z + 1)/2) * ((double)zSize) - 0.5); }

int VoxelVolume::getVoxelAtPosition(double x, double y, double z) const {
    if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) return 0;
    return getVoxel(getNearestIFromX(x), getNearestJFromY(y), getNearestKFromZ(z));
}

int VoxelVolume::getVoxelAtPosition(const Vector3Dd& p) const {
    return getVoxelAtPosition(p.x(), p.y(), p.z());
}

void VoxelVolume::putVoxelAtPosition(double x, double y, double z, char val) {
    if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) return;
    putVoxel(getNearestIFromX(x), getNearestJFromY(y), getNearestKFromZ(z), val);
}

void VoxelVolume::putVoxelAtPosition(const Vector3Dd& p, char val) {
    putVoxelAtPosition(p.x(), p.y(), p.z(), val);
}

double* VoxelVolume::getMinMax() {
    double* m = new double[6];
    m[0]=-1; m[1]=-1; m[2]=-1; m[3]=1; m[4]=1; m[5]=1;
    return m;
}

Ray* VoxelVolume::doIntersection(const Ray& inOutRay) {
    RayHit hit;
    if (doIntersection(inOutRay, &hit) && hit.ray() != nullptr) {
        return new Ray(*hit.ray());
    }
    return nullptr;
}

bool VoxelVolume::doIntersection(const Ray& inRay, RayHit* outHit) {
    double tmin = -1e308, tmax = 1e308;
    const double bounds[2] = {-1.0, 1.0};

    for (int axis = 0; axis < 3; axis++) {
        double o = axis == 0 ? inRay.getOrigin().x() : (axis == 1 ? inRay.getOrigin().y() : inRay.getOrigin().z());
        double d = axis == 0 ? inRay.getDirection().x() : (axis == 1 ? inRay.getDirection().y() : inRay.getDirection().z());

        if (std::abs(d) <= VSDK::EPSILON) {
            if (o < bounds[0] || o > bounds[1]) return false;
            continue;
        }

        double t1 = (bounds[0] - o) / d;
        double t2 = (bounds[1] - o) / d;
        if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
        if (t1 > tmin) tmin = t1;
        if (t2 < tmax) tmax = t2;
        if (tmin > tmax) return false;
    }

    double hitT = tmin > VSDK::EPSILON ? tmin : tmax;
    if (hitT <= VSDK::EPSILON) return false;

    if (outHit != nullptr) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) outHit->setRay(inRay.withT(hitT));
        else outHit->setHitDistance(hitT);
        doExtraInformation(inRay, hitT, outHit);
    }
    return true;
}

void VoxelVolume::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) {
    if (outData == nullptr) return;
    if (outData->needsPoint()) {
        outData->p = inRay.getOrigin().add(inRay.getDirection().multiply(inT));
    }
}

Matrix4x4d VoxelVolume::getTransformFromVoxelFrameToMinMax(const double minmax[6]) {
    double sx = minmax[3]-minmax[0], sy = minmax[4]-minmax[1], sz = minmax[5]-minmax[2];
    double greaterScale = sx;
    if (sy > greaterScale) greaterScale = sy;
    if (sz > greaterScale) greaterScale = sz;

    Matrix4x4d S, T1, T2;
    S = S.scale(greaterScale/2, greaterScale/2, greaterScale/2);
    T1 = T1.translation(1, 1, 1);
    T2 = T2.translation(minmax[0]-(greaterScale-sx)/2, minmax[1]-(greaterScale-sy)/2, minmax[2]-(greaterScale-sz)/2);
    return T2.multiply(S.multiply(T1));
}

Vector3Dd VoxelVolume::doCenterOfMass() {
    double cmx=0,cmy=0,cmz=0,mi,M=0;
    for (int x=0; x<getXSize(); x++) {
        for (int y=0; y<getYSize(); y++) {
            for (int z=0; z<getZSize(); z++) {
                mi = ((double)getVoxel(x,y,z))/255.0;
                M += mi;
                Vector3Dd p = getVoxelPosition(x,y,z);
                cmx += mi*p.x(); cmy += mi*p.y(); cmz += mi*p.z();
            }
        }
    }
    if (std::abs(M) < VSDK::EPSILON) return Vector3Dd(0,0,0);
    return Vector3Dd(cmx/M, cmy/M, cmz/M);
}
