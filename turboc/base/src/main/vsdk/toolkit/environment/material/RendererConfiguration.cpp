#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
RendererConfiguration::RendererConfiguration()
    : shadingType(SHADING_TYPE_GOURAUD), surfaces(TRUE), wires(FALSE), boundingVolume(FALSE), selectionCorners(FALSE),
      texture(TRUE), bumpMap(FALSE), points(FALSE), normals(FALSE), trianglesNormals(FALSE), useVertexColors(FALSE),
      vertexNormalSmoothingThresholdDegrees(15.0),
      wireColor(1, 1, 1), boundingVolumeColor(1, 1, 0), lodHint(0)
{
}

int RendererConfiguration::compareTo(const RendererConfiguration& other) const
{
    long valThis = 0x00;
    if ( surfaces ) valThis += 0x0001;
    if ( wires ) valThis += 0x0002;
    if ( boundingVolume ) valThis += 0x0004;
    if ( selectionCorners ) valThis += 0x0008;
    if ( texture ) valThis += 0x0010;
    if ( bumpMap ) valThis += 0x0020;
    if ( points ) valThis += 0x0040;
    if ( normals ) valThis += 0x0080;
    if ( trianglesNormals ) valThis += 0x0100;
    valThis += shadingType * 0x1000;
    valThis += Cpp98Support::lround(vertexNormalSmoothingThresholdDegrees) * 0x100000;

    long valOther = 0x00;
    if ( other.surfaces ) valOther += 0x0001;
    if ( other.wires ) valOther += 0x0002;
    if ( other.boundingVolume ) valOther += 0x0004;
    if ( other.selectionCorners ) valOther += 0x0008;
    if ( other.texture ) valOther += 0x0010;
    if ( other.bumpMap ) valOther += 0x0020;
    if ( other.points ) valOther += 0x0040;
    if ( other.normals ) valOther += 0x0080;
    if ( other.trianglesNormals ) valOther += 0x0100;
    valOther += other.shadingType * 0x1000;
    valOther += Cpp98Support::lround(other.vertexNormalSmoothingThresholdDegrees) * 0x100000;

    if ( valThis > valOther ) return 1;
    if ( valThis < valOther ) return -1;
    return 0;
}

void RendererConfiguration::cloneFrom(const RendererConfiguration& other)
{
    shadingType = other.shadingType;
    surfaces = other.surfaces;
    wires = other.wires;
    boundingVolume = other.boundingVolume;
    selectionCorners = other.selectionCorners;
    texture = other.texture;
    bumpMap = other.bumpMap;
    points = other.points;
    normals = other.normals;
    trianglesNormals = other.trianglesNormals;
    wireColor = ColorRgb(other.wireColor);
    boundingVolumeColor = ColorRgb(other.boundingVolumeColor);
    useVertexColors = other.useVertexColors;
    vertexNormalSmoothingThresholdDegrees = other.vertexNormalSmoothingThresholdDegrees;
    lodHint = other.lodHint;
}

RendererConfiguration RendererConfiguration::clone() const
{
    RendererConfiguration copy;
    copy.cloneFrom(*this);
    return copy;
}

void RendererConfiguration::setLodHint(int l) { lodHint = l; }
int RendererConfiguration::getLodHint() const { return lodHint; }

void RendererConfiguration::setWireColor(const ColorRgb& c) { wireColor = ColorRgb(c); }
void RendererConfiguration::setWireColor(double r, double g, double b) { wireColor = ColorRgb(r, g, b); }
ColorRgb RendererConfiguration::getWireColor() const { return wireColor; }

void RendererConfiguration::setSurfaces(BOOLEAN b) { surfaces = b; }
void RendererConfiguration::setWires(BOOLEAN b) { wires = b; }
void RendererConfiguration::setBoundingVolume(BOOLEAN b) { boundingVolume = b; }
void RendererConfiguration::setSelectionCorners(BOOLEAN c) { selectionCorners = c; }
void RendererConfiguration::setTexture(BOOLEAN b) { texture = b; }
void RendererConfiguration::setBumpMap(BOOLEAN b) { bumpMap = b; }
void RendererConfiguration::setPoints(BOOLEAN b) { points = b; }
void RendererConfiguration::setNormals(BOOLEAN b) { normals = b; }
void RendererConfiguration::setTrianglesNormals(BOOLEAN b) { trianglesNormals = b; }
void RendererConfiguration::setShadingType(int type) { shadingType = type; }
void RendererConfiguration::setShadingType(ShadingType type) { shadingType = ((int)(type)); }

void RendererConfiguration::setVertexNormalSmoothingThresholdDegrees(double thresholdDegrees)
{
    if ( Cpp98Support::isNaN(thresholdDegrees) ) {
        vertexNormalSmoothingThresholdDegrees = 15.0;
        return;
    }
    if ( thresholdDegrees < 0.0 ) {
        thresholdDegrees = 0.0;
    }
    if ( thresholdDegrees > 180.0 ) {
        thresholdDegrees = 180.0;
    }
    vertexNormalSmoothingThresholdDegrees = thresholdDegrees;
}

double RendererConfiguration::getVertexNormalSmoothingThresholdDegrees() const
{
    return vertexNormalSmoothingThresholdDegrees;
}

BOOLEAN RendererConfiguration::isSurfacesSet() const { return surfaces; }
BOOLEAN RendererConfiguration::isWiresSet() const { return wires; }
BOOLEAN RendererConfiguration::isBoundingVolumeSet() const { return boundingVolume; }
BOOLEAN RendererConfiguration::isSelectionCornersSet() const { return selectionCorners; }
BOOLEAN RendererConfiguration::isTextureSet() const { return texture; }
BOOLEAN RendererConfiguration::isBumpMapSet() const { return bumpMap; }
BOOLEAN RendererConfiguration::isPointsSet() const { return points; }
BOOLEAN RendererConfiguration::isNormalsSet() const { return normals; }
BOOLEAN RendererConfiguration::isTrianglesNormalsSet() const { return trianglesNormals; }

int RendererConfiguration::getShadingType() const { return shadingType; }
ShadingType RendererConfiguration::getShadingTypeEnum() const { return ShadingTypeUtil::fromCode(shadingType); }

void RendererConfiguration::changeSurfaces() { surfaces = !surfaces; }
void RendererConfiguration::changeWires() { wires = !wires; }
void RendererConfiguration::changeBoundingVolume() { boundingVolume = !boundingVolume; }
void RendererConfiguration::changeSelectionCorners() { selectionCorners = !selectionCorners; }
void RendererConfiguration::changeTexture() { texture = !texture; }
void RendererConfiguration::changeBumpMap() { bumpMap = !bumpMap; }
void RendererConfiguration::changePoints() { points = !points; }
void RendererConfiguration::changeNormals() { normals = !normals; }
void RendererConfiguration::changeTrianglesNormals() { trianglesNormals = !trianglesNormals; }
void RendererConfiguration::changeShadingType() { setShadingType(ShadingTypeUtil::next(getShadingTypeEnum())); }

BOOLEAN RendererConfiguration::getUseVertexColors() const { return useVertexColors; }
void RendererConfiguration::setUseVertexColors(BOOLEAN value) { useVertexColors = value; }
ColorRgb RendererConfiguration::getBoundingVolumeColor() const { return boundingVolumeColor; }
void RendererConfiguration::setBoundingVolumeColor(const ColorRgb& c) { boundingVolumeColor = c; }

String RendererConfiguration::toString() const
{
    String msg = "<RendererConfiguration>:\n";
    msg += "  - Shading type: ";
    switch ( shadingType ) {
        case SHADING_TYPE_NOLIGHT: msg += "LIGHTING DISABLED (ONLY AMBIENT COLOR)\n"; break;
        case SHADING_TYPE_FLAT: msg += "FLAT\n"; break;
        case SHADING_TYPE_GOURAUD: msg += "GOURAUD\n"; break;
        case SHADING_TYPE_PHONG: msg += "PHONG\n"; break;
        case SHADING_TYPE_COOK_TERRANCE: msg += "COOK-TERRANCE\n"; break;
        default: msg += "INVALID!\n"; break;
    }
    msg += "  - Draw points: " + String(points ? "ON" : "OFF") + "\n";
    msg += "  - Draw wires: " + String(wires ? "ON" : "OFF") + "\n";
    msg += "  - Draw surfaces: " + String(surfaces ? "ON" : "OFF") + "\n";
    msg += "  - Draw bounding volume: " + String(boundingVolume ? "ON" : "OFF") + "\n";
    msg += "  - Draw selection corners: " + String(selectionCorners ? "ON" : "OFF") + "\n";
    msg += "  - Draw normals: " + String(normals ? "ON" : "OFF") + "\n";
    msg += "  - Draw triangles normals: " + String(trianglesNormals ? "ON" : "OFF") + "\n";
    msg += "  - With texture: " + String(texture ? "ON" : "OFF") + "\n";
    msg += "  - With bump map: " + String(bumpMap ? "ON" : "OFF") + "\n";
    msg += "  - Vertex normal smoothing threshold: " +
        String::valueOf(vertexNormalSmoothingThresholdDegrees) + " deg\n";
    return msg;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
