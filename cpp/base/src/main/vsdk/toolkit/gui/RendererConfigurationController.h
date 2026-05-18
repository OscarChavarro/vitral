#ifndef __VSDK_TOOLKIT_GUI_RENDERERCONFIGURATIONCONTROLLER_H__
#define __VSDK_TOOLKIT_GUI_RENDERERCONFIGURATIONCONTROLLER_H__

#include "Controller.h"
#include "KeyEvent.h"

class RendererConfiguration;

class RendererConfigurationController : public Controller {
private:
    RendererConfiguration* qualitySelection;

public:
    RendererConfigurationController();
    explicit RendererConfigurationController(RendererConfiguration* qualitySelection);
    virtual ~RendererConfigurationController() {}

    void setRendererConfiguration(RendererConfiguration* q);

    bool processKeyPressedEvent(const KeyEvent& keyEvent);
    bool processKeyReleasedEvent(const KeyEvent& keyEvent);
};


#endif
