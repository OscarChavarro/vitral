#ifndef __TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__
#define __TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__

#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.h"
class MeshModel;
class Runnable;

class TangibleInterfaceInteractionTechniques : public TangibleInterfaceListener {
private:
    static const char* RAY_CUBE_TANGIBLE_ELEMENT_ID;

    MeshModel* model;
    Runnable* repaintCallback;
    TangibleInterfaceEvent2RayGizmoMapper mapper;

public:
    /**
    @param repaintCallback called from the connection thread after each
    event (not owned)
    */
    TangibleInterfaceInteractionTechniques(MeshModel* model, Runnable* repaintCallback);

    virtual void tangibleInterfaceEventReceived(const TangibleInterfaceEvent& event);
};

#endif
