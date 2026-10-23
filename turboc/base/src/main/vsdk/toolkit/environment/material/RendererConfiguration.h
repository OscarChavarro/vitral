#ifndef __RENDERER_CONFIGURATION__
#define __RENDERER_CONFIGURATION__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/material/ShadingType.h"
class RendererConfiguration {
public:
    enum {
        SHADING_TYPE_NOLIGHT = 0,
        SHADING_TYPE_FLAT = 1,
        SHADING_TYPE_GOURAUD = 2,
        SHADING_TYPE_PHONG = 3,
        SHADING_TYPE_COOK_TERRANCE = 4
    };

private:
    int shadingType;
    BOOLEAN surfaces;
    BOOLEAN wires;
    BOOLEAN boundingVolume;
    BOOLEAN selectionCorners;
    BOOLEAN texture;
    BOOLEAN bumpMap;
    BOOLEAN points;
    BOOLEAN normals;
    BOOLEAN trianglesNormals;
    BOOLEAN useVertexColors;
    double vertexNormalSmoothingThresholdDegrees;
    ColorRgb wireColor;
    ColorRgb boundingVolumeColor;
    int lodHint;

public:
    RendererConfiguration();
    virtual ~RendererConfiguration() {}

    int compareTo(const RendererConfiguration& other) const;
    void cloneFrom(const RendererConfiguration& other);
    RendererConfiguration clone() const;

    void setLodHint(int l);
    int getLodHint() const;

    void setWireColor(const ColorRgb& c);
    void setWireColor(double r, double g, double b);
    ColorRgb getWireColor() const;

    void setSurfaces(BOOLEAN b);
    void setWires(BOOLEAN b);
    void setBoundingVolume(BOOLEAN b);
    void setSelectionCorners(BOOLEAN c);
    void setTexture(BOOLEAN b);
    void setBumpMap(BOOLEAN b);
    void setPoints(BOOLEAN b);
    void setNormals(BOOLEAN b);
    void setTrianglesNormals(BOOLEAN b);
    void setShadingType(int type);
    void setShadingType(ShadingType type);
    void setVertexNormalSmoothingThresholdDegrees(double thresholdDegrees);

    BOOLEAN isSurfacesSet() const;
    BOOLEAN isWiresSet() const;
    BOOLEAN isBoundingVolumeSet() const;
    BOOLEAN isSelectionCornersSet() const;
    BOOLEAN isTextureSet() const;
    BOOLEAN isBumpMapSet() const;
    BOOLEAN isPointsSet() const;
    BOOLEAN isNormalsSet() const;
    BOOLEAN isTrianglesNormalsSet() const;

    int getShadingType() const;
    ShadingType getShadingTypeEnum() const;
    double getVertexNormalSmoothingThresholdDegrees() const;

    void changeSurfaces();
    void changeWires();
    void changeBoundingVolume();
    void changeSelectionCorners();
    void changeTexture();
    void changeBumpMap();
    void changePoints();
    void changeNormals();
    void changeTrianglesNormals();
    void changeShadingType();

    BOOLEAN getUseVertexColors() const;
    void setUseVertexColors(BOOLEAN useVertexColors);
    ColorRgb getBoundingVolumeColor() const;
    void setBoundingVolumeColor(const ColorRgb& boundingVolumeColor);

    String toString() const;
};

#endif
