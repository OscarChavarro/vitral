#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
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

BOOLEAN RendererConfigurationController::processKeyPressedEvent(const KeyEvent& keyEvent)
{
    if (qualitySelection == 0) return FALSE;

    BOOLEAN updated = FALSE;
    int st;

    switch (keyEvent.keycode) {
        case KeyEvent::KEY_F1:
            qualitySelection->changePoints();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F2:
            qualitySelection->changeWires();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F3:
            qualitySelection->changeSurfaces();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F4:
            qualitySelection->changeBoundingVolume();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F5:
            qualitySelection->changeNormals();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F6:
            qualitySelection->changeTrianglesNormals();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F7:
            st = qualitySelection->getShadingType();
            if (st == RendererConfiguration::SHADING_TYPE_NOLIGHT) {
                st = RendererConfiguration::SHADING_TYPE_FLAT;
            }
            else if (st == RendererConfiguration::SHADING_TYPE_FLAT) {
                st = RendererConfiguration::SHADING_TYPE_GOURAUD;
            }
            else if (st == RendererConfiguration::SHADING_TYPE_GOURAUD) {
                st = RendererConfiguration::SHADING_TYPE_PHONG;
            }
            else if (st == RendererConfiguration::SHADING_TYPE_PHONG) {
                st = RendererConfiguration::SHADING_TYPE_COOK_TERRANCE;
            }
            else {
                st = RendererConfiguration::SHADING_TYPE_NOLIGHT;
            }
            qualitySelection->setShadingType(st);
            updated = TRUE;
            break;
        case KeyEvent::KEY_F8:
            qualitySelection->changeTexture();
            updated = TRUE;
            break;
        case KeyEvent::KEY_F9:
            qualitySelection->changeBumpMap();
            updated = TRUE;
            break;
        default:
            break;
    }

    return updated;
}

BOOLEAN RendererConfigurationController::processKeyReleasedEvent(const KeyEvent&)
{
    return FALSE;
}

const Class RendererConfigurationController::CLASS = { "RendererConfigurationController", &Controller::CLASS };

const Class *
RendererConfigurationController::getClass() const
{
    return &CLASS;
}
