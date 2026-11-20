#include "java/lang/IllegalArgumentException.h"
#include "java/lang/String.h"

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
#include "model/RenderTechnology.h"

namespace {

int renderModeFromName(const java::String& name)
{
    if ( name.equalsIgnoreCase("gpu") ) return Viewport::RENDER_MODE_Z_BUFFER;
    if ( name.equalsIgnoreCase("cpu") ) return Viewport::RENDER_MODE_RAYTRACING;
    if ( name.equalsIgnoreCase("hidden_lines") ) {
        return Viewport::RENDER_MODE_HIDDEN_LINES;
    }
    if ( name.equalsIgnoreCase("hidden_surfaces") ) {
        return Viewport::RENDER_MODE_HIDDEN_SURFACES;
    }
    throw java::IllegalArgumentException(
        "renderMode must be gpu, cpu, hidden_lines or hidden_surfaces");
}

const char* renderModeName(int mode)
{
    switch ( mode ) {
      case Viewport::RENDER_MODE_RAYTRACING: return "cpu";
      case Viewport::RENDER_MODE_HIDDEN_LINES: return "hidden_lines";
      case Viewport::RENDER_MODE_HIDDEN_SURFACES: return "hidden_surfaces";
      default: return "gpu";
    }
}

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
@throws java::IllegalArgumentException if there is no type with that name
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
    throw java::IllegalArgumentException((java::String(
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
        throw java::IllegalArgumentException((java::String("Unknown mode \"") + mode +
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
        throw java::IllegalArgumentException(
            "Viewport index out of range: " + java::String::valueOf((int)index));
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
        if ( MCPJson::booleanProperty(request, "withShadows", value) ) q->setWithShadows(value);
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
        if ( !renderMode.isEmpty() ) {
            int mode = renderModeFromName(renderMode);
            if ( !model->getActiveViewportSet()->isRenderModeAvailable(mode) ) {
                throw java::IllegalArgumentException((java::String("renderMode ") +
                    renderMode + " is not available with the render technology " +
                    RenderTechnology::getName(
                        model->getDrawingArea()->getRenderTechnology())).c_str());
            }
            viewport->setRenderMode(mode);
        }
    }
}

java::String MCPViewportTools::setSelectedRendering(const java::String& request)
{
    ViewportSet* set = model->getActiveViewportSet();
    int selected = -1;
    for ( int i = 0; i < set->getViewportCount(); i++ ) {
        if ( set->getViewport(i) == set->getSelectedViewport() ) {
            selected = i;
            break;
        }
    }
    int index = (int)MCPJson::numberProperty(request, "viewport", selected);
    if ( index < 0 || index >= set->getViewportCount() ) {
        throw java::IllegalArgumentException((java::String(
            "Viewport index out of range: ") + java::String::valueOf(index)).c_str());
    }
    java::String algorithm = MCPJson::stringProperty(request, "algorithm", "");
    if ( algorithm.isEmpty() ) {
        throw java::IllegalArgumentException(
            "algorithm must be gpu, hidden_lines, hidden_surfaces or raytracing");
    }
    int mode;
    if ( algorithm.equalsIgnoreCase("gpu") ) {
        mode = Viewport::RENDER_MODE_Z_BUFFER;
    }
    else if ( algorithm.equalsIgnoreCase("hidden_lines") ||
              algorithm.equalsIgnoreCase("hidden-line") ) {
        mode = Viewport::RENDER_MODE_HIDDEN_LINES;
    }
    else if ( algorithm.equalsIgnoreCase("hidden_surfaces") ||
              algorithm.equalsIgnoreCase("hidden-surface") ) {
        mode = Viewport::RENDER_MODE_HIDDEN_SURFACES;
    }
    else if ( algorithm.equalsIgnoreCase("raytracing") ||
              algorithm.equalsIgnoreCase("raytrace") ||
              algorithm.equalsIgnoreCase("cpu") ) {
        mode = Viewport::RENDER_MODE_RAYTRACING;
    }
    else {
        throw java::IllegalArgumentException(
            "algorithm must be gpu, hidden_lines, hidden_surfaces or raytracing");
    }
    if ( !set->isRenderModeAvailable(mode) ) {
        throw java::IllegalArgumentException((java::String("algorithm ") +
            algorithm + " is not available with " + RenderTechnology::getName(
                model->getDrawingArea()->getRenderTechnology())).c_str());
    }
    set->getViewport(index)->setRenderMode(mode);
    return describeRendererConfigurations(java::String("{\"viewport\":") +
        java::String::valueOf(index) + "}");
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
            .append(",\"withShadows\":").append(q->isWithShadowsSet())
            .append(",\"boundingVolume\":").append(q->isBoundingVolumeSet())
            .append(",\"normals\":").append(q->isNormalsSet())
            .append(",\"trianglesNormals\":").append(q->isTrianglesNormalsSet())
            .append(",\"selectionCorners\":").append(q->isSelectionCornersSet())
            .append(",\"shading\":\"").append(shadingTypeName(q->getShadingTypeEnum())).append('"')
            .append(",\"grid\":").append(viewport->isShowGrid())
            .append(",\"renderMode\":\"")
            .append(renderModeName(viewport->getRenderMode()))
            .append("\"}");
    }
    sb.append("],\"technology\":\"")
        .append(RenderTechnology::getName(
            model->getDrawingArea()->getRenderTechnology()))
        .append("\"}");
    return sb.toString();
}
