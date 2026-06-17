#include <cmath>
#include <cstdio>

#include "java/io/FileInputStream.h"
#include "java/util/ArrayList.txx"
#include "io/MarkerGroupPersistence.hpp"
#include "jackson/databind/ObjectMapper.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
Quaterniond MarkerGroupPersistence::eulerDegToQuaternion(double yawDeg, double pitchDeg, double rollDeg) const {
    const double toRad = 3.14159265358979323846 / 180.0;
    Matrix4x4d r = Matrix4x4d().eulerAnglesRotation(yawDeg * toRad, pitchDeg * toRad, rollDeg * toRad);
    return r.exportToQuaternion().normalized();
}

bool MarkerGroupPersistence::readFromJsonFile(const java::String& filePath, MarkerGroup* outGroup) const {
    if (outGroup == nullptr) return false;

    java::FileInputStream fis(filePath.c_str());

    char buffer[65536];
    int pos = 0;
    for (;;) {
        int c = fis.read();
        if (c < 0 || pos >= 65535) break;
        buffer[pos++] = static_cast<char>(c);
    }
    if (pos <= 0) return false;
    buffer[pos] = '\0';

    jackson::databind::ObjectMapper mapper;
    jackson::databind::JsonNode root = mapper.readTree(java::String(buffer));
    if (!root.isObject()) return false;

    const jackson::databind::JsonNode* labelNode = root.get("label");
    const jackson::databind::JsonNode* colorNode = root.get("color");
    const jackson::databind::JsonNode* markersNode = root.get("markers");
    if (labelNode == nullptr || colorNode == nullptr || markersNode == nullptr) return false;
    if (!labelNode->isString() || !colorNode->isArray() || !markersNode->isArray()) return false;
    if (colorNode->size() < 3) return false;

    outGroup->label = labelNode->asText();
    outGroup->color = ColorRgb(colorNode->get(0)->asDouble(), colorNode->get(1)->asDouble(), colorNode->get(2)->asDouble());

    const jackson::databind::JsonNode* sideNode = root.get("physicalSideLength");
    if (sideNode != nullptr && sideNode->isNumber()) {
        outGroup->physicalSideLength = sideNode->asDouble();
    }

    outGroup->markers.clear();

    for (long i = 0; i < markersNode->size(); ++i) {
        const jackson::databind::JsonNode* markerNode = markersNode->get(i);
        if (markerNode == nullptr || !markerNode->isObject()) continue;

        const jackson::databind::JsonNode* idNode = markerNode->get("id");
        const jackson::databind::JsonNode* posNode = markerNode->get("position");
        const jackson::databind::JsonNode* yprNode = markerNode->get("yawPitchRoll");
        if (idNode == nullptr || posNode == nullptr || yprNode == nullptr) continue;
        if (!idNode->isNumber() || !posNode->isArray() || !yprNode->isArray()) continue;
        if (posNode->size() < 3 || yprNode->size() < 3) continue;

        Marker marker;
        marker.id = idNode->asInt(-1);
        marker.position = Vector3Dd(posNode->get(0)->asDouble(), posNode->get(1)->asDouble(), posNode->get(2)->asDouble());
        marker.rotation = eulerDegToQuaternion(yprNode->get(0)->asDouble(), yprNode->get(1)->asDouble(), yprNode->get(2)->asDouble());
        marker.physicalSideLength = outGroup->physicalSideLength;
        outGroup->markers.add(marker);
    }

    return outGroup->markers.size() > 0;
}
