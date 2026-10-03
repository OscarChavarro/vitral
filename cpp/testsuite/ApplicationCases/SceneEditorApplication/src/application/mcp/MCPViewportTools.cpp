#include <stdexcept>
#include <string>

#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/MCPViewportTools.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/InteractionMode.h"

namespace {

const char* shadingTypeName(ShadingType type)
{
    switch ( type ) {
      case SHADING_NOLIGHT: return "NOLIGHT";
      case SHADING_FLAT: return "FLAT";
      case SHADING_GOURAUD: return "GOURAUD";
      case SHADING_PHONG: return "PHONG";
      default: return "COOK_TERRANCE";
    }
}

/**
As Java `ShadingType.valueOf`.
@throws std::invalid_argument if there is no type with that name
*/
ShadingType shadingTypeValueOf(const java::String& name)
{
    const ShadingType types[] = {
        SHADING_NOLIGHT, SHADING_FLAT, SHADING_GOURAUD, SHADING_PHONG,
        SHADING_COOK_TERRANCE
    };
    for ( int i = 0; i < 5; i++ ) {
        if ( name.equals(shadingTypeName(types[i])) ) {
            return types[i];
        }
    }
    throw std::invalid_argument((java::String(
        "No enum constant vsdk.toolkit.environment.material.ShadingType.") +
        name).c_str());
}

}

MCPViewportTools::MCPViewportTools(ApplicationModel* model)
    : model(model)
{
}

java::String MCPViewportTools::setInteractionMode(const java::String& request)
{
    java::String mode = MCPJson::stringProperty(request, "mode", "");
    InteractionMode value;

    if ( mode.equals("camera") ) value = InteractionMode::CAMERA;
    else if ( mode.equals("select") ) value = InteractionMode::SELECT;
    else if ( mode.equals("translate") ) value = InteractionMode::TRANSLATE;
    else if ( mode.equals("rotate") ) value = InteractionMode::ROTATE;
    else if ( mode.equals("scale") ) value = InteractionMode::SCALE;
    else {
        throw std::invalid_argument((java::String("Unknown mode \"") + mode +
            "\". Use camera, select, translate, rotate or scale").c_str());
    }
    model->getDrawingArea()->setInteractionMode(value);
    return java::String("{\"ok\":true,\"mode\":\"") + mode + "\"}";
}

java::ArrayList<Viewport*> MCPViewportTools::selectedViewports(
    const java::String& request)
{
    ViewportSet* set = model->getActiveViewportSet();
    java::ArrayList<Viewport*> out;
    double index = MCPJson::numberProperty(request, "viewport", -1);

    if ( index < 0 ) {
        for ( int i = 0; i < set->getViewportCount(); i++ ) {
            out.add(set->getViewport(i));
        }
    }
    else if ( index < set->getViewportCount() ) {
        out.add(set->getViewport((int)index));
    }
    else {
        throw std::invalid_argument(
            "Viewport index out of range: " + std::to_string((int)index));
    }
    return out;
}

void MCPViewportTools::setRendererConfiguration(const java::String& request)
{
    java::ArrayList<Viewport*> viewports = selectedViewports(request);

    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        RendererConfiguration* q = viewport->getRendererConfiguration();
        bool value;

        if ( MCPJson::booleanProperty(request, "points", value) ) q->setPoints(value);
        if ( MCPJson::booleanProperty(request, "wires", value) ) q->setWires(value);
        if ( MCPJson::booleanProperty(request, "surfaces", value) ) q->setSurfaces(value);
        if ( MCPJson::booleanProperty(request, "texture", value) ) q->setTexture(value);
        if ( MCPJson::booleanProperty(request, "bumpMap", value) ) q->setBumpMap(value);
        if ( MCPJson::booleanProperty(request, "boundingVolume", value) ) q->setBoundingVolume(value);
        if ( MCPJson::booleanProperty(request, "normals", value) ) q->setNormals(value);
        if ( MCPJson::booleanProperty(request, "trianglesNormals", value) ) q->setTrianglesNormals(value);
        if ( MCPJson::booleanProperty(request, "selectionCorners", value) ) q->setSelectionCorners(value);

        java::String shading = MCPJson::stringProperty(request, "shading", "");
        if ( !shading.isEmpty() ) {
            q->setShadingType(shadingTypeValueOf(shading.toUpperCase()));
        }

        if ( MCPJson::booleanProperty(request, "grid", value) ) viewport->setShowGrid(value);
        java::String renderMode = MCPJson::stringProperty(request, "renderMode", "");
        if ( renderMode.equalsIgnoreCase("gpu") ) {
            viewport->setRenderMode(Viewport::RENDER_MODE_Z_BUFFER);
        }
        else if ( renderMode.equalsIgnoreCase("cpu") ) {
            viewport->setRenderMode(Viewport::RENDER_MODE_RAYTRACING);
        }
        else if ( !renderMode.isEmpty() ) {
            throw std::invalid_argument("renderMode must be gpu or cpu");
        }
    }
}

java::String MCPViewportTools::describeRendererConfigurations(
    const java::String& request)
{
    java::StringBuilder sb;
    ViewportSet* set = model->getActiveViewportSet();
    java::ArrayList<Viewport*> viewports = selectedViewports(request);
    bool first = true;

    sb.append("{\"viewports\":[");
    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        RendererConfiguration* q = viewport->getRendererConfiguration();
        int index = -1;

        for ( int j = 0; j < set->getViewportCount(); j++ ) {
            if ( set->getViewport(j) == viewport ) {
                index = j;
                break;
            }
        }
        if ( !first ) {
            sb.append(',');
        }
        first = false;
        sb.append("{\"index\":").append(index)
            .append(",\"title\":\"").append(MCPJson::escape(viewport->getTitle())).append('"')
            .append(",\"points\":").append(q->isPointsSet())
            .append(",\"wires\":").append(q->isWiresSet())
            .append(",\"surfaces\":").append(q->isSurfacesSet())
            .append(",\"texture\":").append(q->isTextureSet())
            .append(",\"bumpMap\":").append(q->isBumpMapSet())
            .append(",\"boundingVolume\":").append(q->isBoundingVolumeSet())
            .append(",\"normals\":").append(q->isNormalsSet())
            .append(",\"trianglesNormals\":").append(q->isTrianglesNormalsSet())
            .append(",\"selectionCorners\":").append(q->isSelectionCornersSet())
            .append(",\"shading\":\"").append(shadingTypeName(q->getShadingTypeEnum())).append('"')
            .append(",\"grid\":").append(viewport->isShowGrid())
            .append(",\"renderMode\":\"")
            .append(viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING ? "cpu" : "gpu")
            .append("\"}");
    }
    sb.append("]}");
    return sb.toString();
}
