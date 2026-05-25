#include "vsdk/toolkit/environment/material/RendererConfiguration.h"

RendererConfiguration::RendererConfiguration()
    : shadingType(SHADING_TYPE_GOURAUD), surfaces(true), wires(false), boundingVolume(false), selectionCorners(false),
      texture(true), bumpMap(false), points(false), normals(false), trianglesNormals(false), useVertexColors(false),
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

void RendererConfiguration::setSurfaces(bool b) { surfaces = b; }
void RendererConfiguration::setWires(bool b) { wires = b; }
void RendererConfiguration::setBoundingVolume(bool b) { boundingVolume = b; }
void RendererConfiguration::setSelectionCorners(bool c) { selectionCorners = c; }
void RendererConfiguration::setTexture(bool b) { texture = b; }
void RendererConfiguration::setBumpMap(bool b) { bumpMap = b; }
void RendererConfiguration::setPoints(bool b) { points = b; }
void RendererConfiguration::setNormals(bool b) { normals = b; }
void RendererConfiguration::setTrianglesNormals(bool b) { trianglesNormals = b; }
void RendererConfiguration::setShadingType(int type) { shadingType = type; }
void RendererConfiguration::setShadingType(ShadingType type) { shadingType = static_cast<int>(type); }

bool RendererConfiguration::isSurfacesSet() const { return surfaces; }
bool RendererConfiguration::isWiresSet() const { return wires; }
bool RendererConfiguration::isBoundingVolumeSet() const { return boundingVolume; }
bool RendererConfiguration::isSelectionCornersSet() const { return selectionCorners; }
bool RendererConfiguration::isTextureSet() const { return texture; }
bool RendererConfiguration::isBumpMapSet() const { return bumpMap; }
bool RendererConfiguration::isPointsSet() const { return points; }
bool RendererConfiguration::isNormalsSet() const { return normals; }
bool RendererConfiguration::isTrianglesNormalsSet() const { return trianglesNormals; }

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

bool RendererConfiguration::getUseVertexColors() const { return useVertexColors; }
void RendererConfiguration::setUseVertexColors(bool value) { useVertexColors = value; }
ColorRgb RendererConfiguration::getBoundingVolumeColor() const { return boundingVolumeColor; }
void RendererConfiguration::setBoundingVolumeColor(const ColorRgb& c) { boundingVolumeColor = c; }

std::string RendererConfiguration::toString() const
{
    std::string msg = "<RendererConfiguration>:\n";
    msg += "  - Shading type: ";
    switch ( shadingType ) {
        case SHADING_TYPE_NOLIGHT: msg += "LIGHTING DISABLED (ONLY AMBIENT COLOR)\n"; break;
        case SHADING_TYPE_FLAT: msg += "FLAT\n"; break;
        case SHADING_TYPE_GOURAUD: msg += "GOURAUD\n"; break;
        case SHADING_TYPE_PHONG: msg += "PHONG\n"; break;
        case SHADING_TYPE_COOK_TERRANCE: msg += "COOK-TERRANCE\n"; break;
        default: msg += "INVALID!\n"; break;
    }
    msg += "  - Draw points: " + std::string(points ? "ON" : "OFF") + "\n";
    msg += "  - Draw wires: " + std::string(wires ? "ON" : "OFF") + "\n";
    msg += "  - Draw surfaces: " + std::string(surfaces ? "ON" : "OFF") + "\n";
    msg += "  - Draw bounding volume: " + std::string(boundingVolume ? "ON" : "OFF") + "\n";
    msg += "  - Draw selection corners: " + std::string(selectionCorners ? "ON" : "OFF") + "\n";
    msg += "  - Draw normals: " + std::string(normals ? "ON" : "OFF") + "\n";
    msg += "  - Draw triangles normals: " + std::string(trianglesNormals ? "ON" : "OFF") + "\n";
    msg += "  - With texture: " + std::string(texture ? "ON" : "OFF") + "\n";
    msg += "  - With bump map: " + std::string(bumpMap ? "ON" : "OFF") + "\n";
    return msg;
}
