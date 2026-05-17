#include "NormalMap.h"
#include "../common/linealAlgebra/Vector3Dd.h"
#include <cmath>

static int positiveMod(int value, int modulus) {
    int result = value % modulus;
    if (result < 0) {
        result += modulus;
    }
    return result;
}

NormalMap::NormalMap() : xSize(0), ySize(0), bumpMapScale(nullptr) {
    bumpMapScale = new Vector3Dd(1.0, 1.0, 1.0);
}

NormalMap::~NormalMap() {
    for (size_t i = 0; i < data.size(); i++) {
        if (data[i] != nullptr) {
            delete data[i];
            data[i] = nullptr;
        }
    }
    data.clear();

    if (bumpMapScale != nullptr) {
        delete bumpMapScale;
        bumpMapScale = nullptr;
    }
}

bool NormalMap::init(int width, int height) {
    try {
        for (size_t i = 0; i < data.size(); i++) {
            if (data[i] != nullptr) {
                delete data[i];
            }
        }
        data.clear();

        for (int i = 0; i < width * height; i++) {
            data.push_back(new Vector3Dd());
        }
    } catch (...) {
        return false;
    }
    xSize = width;
    ySize = height;
    return true;
}

int NormalMap::getXSize() const {
    return xSize;
}

int NormalMap::getYSize() const {
    return ySize;
}

Vector3Dd* NormalMap::getBumpMapScale() const {
    if (bumpMapScale != nullptr) {
        return new Vector3Dd(*bumpMapScale);
    }
    return nullptr;
}

void NormalMap::setBumpMapScale(const Vector3Dd& scale) {
    if (bumpMapScale != nullptr) {
        delete bumpMapScale;
    }
    bumpMapScale = new Vector3Dd(scale);
}

void NormalMap::putNormal(int i, int j, const Vector3Dd& n) {
    if (i < 0 || j < 0 || i >= xSize || j >= ySize) {
        return;
    }
    int index = j * xSize + i;
    if (data[index] != nullptr) {
        delete data[index];
    }
    data[index] = new Vector3Dd(n);
}

Vector3Dd* NormalMap::getNormal(int u, int v) const {
    if (u < 0 || v < 0 || u >= xSize || v >= ySize) {
        return nullptr;
    }
    int index = v * xSize + u;
    if (data[index] != nullptr) {
        return new Vector3Dd(*data[index]);
    }
    return nullptr;
}

Vector3Dd* NormalMap::getNormalBiLinear(double x, double y) const {
    double u = x - std::floor(x);
    double v = y - std::floor(y);
    int width = getXSize();
    int height = getYSize();
    double U = u * ((double)width);
    double V = v * ((double)height);
    int i0 = positiveMod((int)std::floor(U), width);
    int j0 = positiveMod((int)std::floor(V), height);
    int i1 = positiveMod(i0 + 1, width);
    int j1 = positiveMod(j0 + 1, height);
    double du = U - (double)i0;
    double dv = V - (double)j0;

    Vector3Dd* F00 = getNormal(i0, j0);
    Vector3Dd* F01 = getNormal(i0, j1);
    Vector3Dd* F10 = getNormal(i1, j0);
    Vector3Dd* F11 = getNormal(i1, j1);

    Vector3Dd* FU0 = new Vector3Dd(F00->add(F10->subtract(*F00).multiply(du)));
    Vector3Dd* FU1 = new Vector3Dd(F01->add(F11->subtract(*F01).multiply(du)));
    Vector3Dd* FVAL = new Vector3Dd(FU0->add(FU1->subtract(*FU0).multiply(dv)));

    delete F00;
    delete F01;
    delete F10;
    delete F11;
    delete FU0;
    delete FU1;

    return FVAL;
}
