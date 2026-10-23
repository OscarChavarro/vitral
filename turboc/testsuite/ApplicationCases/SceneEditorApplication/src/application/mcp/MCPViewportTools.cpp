#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

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
#include "java/lang/IllegalArgumentException.h"


static const char* shadingTypeName(ShadingType type)
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
@throws IllegalArgumentException if there is no type with that name
*/
static ShadingType shadingTypeValueOf(const String& name)
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
    throw IllegalArgumentException((String(
        "No enum constant vsdk.toolkit.environment.material.ShadingType.") +
        name).c_str());
}


MCPViewportTools::MCPViewportTools(ApplicationModel* model, BOOLEAN supportsXlib)
    : model(model), supportsXlib(supportsXlib)
{
}

String MCPViewportTools::setInteractionMode(const String& request)
{
    String mode = MCPJson::stringProperty(request, "mode", "");
    InteractionMode value;

    if ( mode.equals("camera") ) value = InteractionMode::CAMERA;
    else if ( mode.equals("select") ) value = InteractionMode::SELECT;
    else if ( mode.equals("translate") ) value = InteractionMode::TRANSLATE;
    else if ( mode.equals("rotate") ) value = InteractionMode::ROTATE;
    else if ( mode.equals("scale") ) value = InteractionMode::SCALE;
    else {
        throw IllegalArgumentException((String("Unknown mode \"") + mode +
            "\". Use camera, select, translate, rotate or scale").c_str());
    }
    model->getDrawingArea()->setInteractionMode(value);
    return String("{\"ok\":true,\"mode\":\"") + mode + "\"}";
}

ArrayList<Viewport*> MCPViewportTools::selectedViewports(
    const String& request)
{
    ViewportSet* set = model->getActiveViewportSet();
    ArrayList<Viewport*> out;
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
        throw IllegalArgumentException(
            "Viewport index out of range: " + Cpp98Support::toString((int)index));
    }
    return out;
}

void MCPViewportTools::setRendererConfiguration(const String& request)
{
    ArrayList<Viewport*> viewports = selectedViewports(request);
    ViewportSet* set = model->getActiveViewportSet();
    String renderMode = MCPJson::stringProperty(request, "renderMode", "");
    int mode = 0;
    if ( !renderMode.isEmpty() ) {
        mode = renderMode.equalsIgnoreCase("gpu") ? Viewport::RENDER_MODE_Z_BUFFER :
            renderMode.equalsIgnoreCase("cpu") ? Viewport::RENDER_MODE_RAYTRACING :
            renderMode.equalsIgnoreCase("hidden_lines") ? Viewport::RENDER_MODE_HIDDEN_LINES : 0;
        if ( mode == 0 )
            throw IllegalArgumentException("renderMode must be gpu, cpu or hidden_lines");
        if ( supportsXlib && (mode == Viewport::RENDER_MODE_HIDDEN_LINES ||
                              mode == Viewport::RENDER_MODE_Z_BUFFER) ) {
            ArrayList<int> modes;
            modes.add(mode);
            modes.add(Viewport::RENDER_MODE_RAYTRACING);
            set->setAvailableRenderModes(modes);
        }
        if ( !set->isRenderModeAvailable(mode) )
            throw IllegalArgumentException("renderMode is not available with this rendering technology");
    }

    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        RendererConfiguration* q = viewport->getRendererConfiguration();
        BOOLEAN value;

        if ( MCPJson::booleanProperty(request, "points", value) ) q->setPoints(value);
        if ( MCPJson::booleanProperty(request, "wires", value) ) q->setWires(value);
        if ( MCPJson::booleanProperty(request, "surfaces", value) ) q->setSurfaces(value);
        if ( MCPJson::booleanProperty(request, "texture", value) ) q->setTexture(value);
        if ( MCPJson::booleanProperty(request, "bumpMap", value) ) q->setBumpMap(value);
        if ( MCPJson::booleanProperty(request, "boundingVolume", value) ) q->setBoundingVolume(value);
        if ( MCPJson::booleanProperty(request, "normals", value) ) q->setNormals(value);
        if ( MCPJson::booleanProperty(request, "trianglesNormals", value) ) q->setTrianglesNormals(value);
        if ( MCPJson::booleanProperty(request, "selectionCorners", value) ) q->setSelectionCorners(value);

        String shading = MCPJson::stringProperty(request, "shading", "");
        if ( !shading.isEmpty() ) {
            q->setShadingType(shadingTypeValueOf(shading.toUpperCase()));
        }

        if ( MCPJson::booleanProperty(request, "grid", value) ) viewport->setShowGrid(value);
        if ( mode != 0 ) {
            viewport->setRenderMode(mode);
        }
    }
}

String MCPViewportTools::describeRendererConfigurations(
    const String& request)
{
    StringBuilder sb;
    ViewportSet* set = model->getActiveViewportSet();
    ArrayList<Viewport*> viewports = selectedViewports(request);
    BOOLEAN first = TRUE;

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
        first = FALSE;
        sb.append("{\"index\":").append(index)
            .append(",\"title\":\"").append(MCPJson::escape(viewport->getTitle())).append('"')
            .append(",\"points\":").appendBoolean(q->isPointsSet())
            .append(",\"wires\":").appendBoolean(q->isWiresSet())
            .append(",\"surfaces\":").appendBoolean(q->isSurfacesSet())
            .append(",\"texture\":").appendBoolean(q->isTextureSet())
            .append(",\"bumpMap\":").appendBoolean(q->isBumpMapSet())
            .append(",\"boundingVolume\":").appendBoolean(q->isBoundingVolumeSet())
            .append(",\"normals\":").appendBoolean(q->isNormalsSet())
            .append(",\"trianglesNormals\":").appendBoolean(q->isTrianglesNormalsSet())
            .append(",\"selectionCorners\":").appendBoolean(q->isSelectionCornersSet())
            .append(",\"shading\":\"").append(shadingTypeName(q->getShadingTypeEnum())).append('"')
            .append(",\"grid\":").appendBoolean(viewport->isShowGrid())
            .append(",\"renderMode\":\"")
            .append(viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING ? "cpu" :
                    viewport->getRenderMode() == Viewport::RENDER_MODE_HIDDEN_LINES ? "hidden_lines" : "gpu")
            .append("\"}");
    }
    sb.append("]}");
    return sb.toString();
}
