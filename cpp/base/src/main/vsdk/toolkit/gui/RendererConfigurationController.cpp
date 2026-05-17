#include "RendererConfigurationController.h"

#include "KeyEvent.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"

namespace vsdk { namespace toolkit { namespace gui {

RendererConfigurationController::RendererConfigurationController()
    : qualitySelection(0)
{
}

RendererConfigurationController::RendererConfigurationController(RendererConfiguration* qualitySelection)
    : qualitySelection(qualitySelection)
{
}

void RendererConfigurationController::setRendererConfiguration(RendererConfiguration* q)
{
    qualitySelection = q;
}

bool RendererConfigurationController::processKeyPressedEvent(const KeyEvent& keyEvent)
{
    if (qualitySelection == 0) return false;

    bool updated = false;
    int st;

    switch (keyEvent.keycode) {
        case KeyEvent::KEY_F1:
            qualitySelection->changePoints();
            updated = true;
            break;
        case KeyEvent::KEY_F2:
            qualitySelection->changeWires();
            updated = true;
            break;
        case KeyEvent::KEY_F3:
            qualitySelection->changeSurfaces();
            updated = true;
            break;
        case KeyEvent::KEY_F4:
            qualitySelection->changeBoundingVolume();
            updated = true;
            break;
        case KeyEvent::KEY_F5:
            qualitySelection->changeNormals();
            updated = true;
            break;
        case KeyEvent::KEY_F6:
            qualitySelection->changeTrianglesNormals();
            updated = true;
            break;
        case KeyEvent::KEY_F7:
            st = qualitySelection->getShadingType();
            if (st == RendererConfiguration::SHADING_TYPE_FLAT) {
                st = RendererConfiguration::SHADING_TYPE_GOURAUD;
                qualitySelection->setBumpMap(false);
            }
            else if (st == RendererConfiguration::SHADING_TYPE_GOURAUD) {
                st = RendererConfiguration::SHADING_TYPE_PHONG;
                qualitySelection->setBumpMap(false);
            }
            else if (st == RendererConfiguration::SHADING_TYPE_PHONG) {
                if (qualitySelection->isBumpMapSet()) {
                    st = RendererConfiguration::SHADING_TYPE_COOK_TERRANCE;
                    qualitySelection->setBumpMap(false);
                }
                else {
                    st = RendererConfiguration::SHADING_TYPE_PHONG;
                    qualitySelection->setBumpMap(true);
                }
            }
            else {
                st = RendererConfiguration::SHADING_TYPE_FLAT;
                qualitySelection->setBumpMap(false);
            }
            qualitySelection->setShadingType(st);
            updated = true;
            break;
        case KeyEvent::KEY_F8:
            qualitySelection->changeTexture();
            updated = true;
            break;
        case KeyEvent::KEY_F9:
            qualitySelection->changeBumpMap();
            updated = true;
            break;
        default:
            break;
    }

    return updated;
}

bool RendererConfigurationController::processKeyReleasedEvent(const KeyEvent&)
{
    return false;
}

}}}
