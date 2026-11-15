#ifndef __RNDRR_CNFGRTN_CNTRLLR__
#define __RNDRR_CNFGRTN_CNTRLLR__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/Controller.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
class RendererConfiguration;

class RendererConfigurationController : public Controller {
private:
    RendererConfiguration* qualitySelection;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    RendererConfigurationController();
    explicit RendererConfigurationController(RendererConfiguration* qualitySelection);
    virtual ~RendererConfigurationController() {}

    void setRendererConfiguration(RendererConfiguration* q);

    BOOLEAN processKeyPressedEvent(const KeyEvent& keyEvent);
    BOOLEAN processKeyReleasedEvent(const KeyEvent& keyEvent);
};


#endif
