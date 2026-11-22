#include "java/util/ArrayList.txx"
#include "model/RenderTechnology.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"

const char* RenderTechnology::getCommand(Value technology)
{
    return technology == XLIB ?
        ViewportSetCommands::IDV_RENDER_TECHNOLOGY_AWT :
        ViewportSetCommands::IDV_RENDER_TECHNOLOGY_OPENGL4;
}

java::ArrayList<int> RenderTechnology::getRenderModes(Value technology)
{
    java::ArrayList<int> modes;
    if ( technology == XLIB ) {
        modes.add(Viewport::RENDER_MODE_HIDDEN_LINES);
        modes.add(Viewport::RENDER_MODE_HIDDEN_SURFACES);
        modes.add(Viewport::RENDER_MODE_RAYTRACING);
    }
    else {
        modes.add(Viewport::RENDER_MODE_Z_BUFFER);
    }
    return modes;
}

const char* RenderTechnology::getName(Value technology)
{
    if ( technology == XLIB ) {
        return "xlib";
    }
#ifdef VITRAL_SCENE_EDITOR_OPENGL1
    return "opengl1";
#else
    return "opengl4";
#endif
}

bool RenderTechnology::fromCommand(const java::String& command,
                                   Value& outTechnology)
{
    if ( command.equals(ViewportSetCommands::IDV_RENDER_TECHNOLOGY_OPENGL4) ) {
        outTechnology = OPENGL;
        return true;
    }
    if ( command.equals(ViewportSetCommands::IDV_RENDER_TECHNOLOGY_AWT) ) {
        outTechnology = XLIB;
        return true;
    }
    return false;
}

bool RenderTechnology::fromName(const java::String& name, Value& outTechnology)
{
    if ( name.equalsIgnoreCase("opengl") || name.equalsIgnoreCase("opengl4") ||
         name.equalsIgnoreCase("opengl1") || name.equalsIgnoreCase("gpu") ) {
        outTechnology = OPENGL;
        return true;
    }
    if ( name.equalsIgnoreCase("xlib") || name.equalsIgnoreCase("awt") ) {
        outTechnology = XLIB;
        return true;
    }
    return false;
}

bool RenderTechnology::forRenderMode(int renderMode, Value current,
                                     Value& outTechnology)
{
    const Value candidates[3] = { current, OPENGL, XLIB };
    for ( int i = 0; i < 3; i++ ) {
        java::ArrayList<int> modes = getRenderModes(candidates[i]);
        for ( long j = 0; j < modes.size(); j++ ) {
            if ( modes.get(j) == renderMode ) {
                outTechnology = candidates[i];
                return true;
            }
        }
    }
    return false;
}
