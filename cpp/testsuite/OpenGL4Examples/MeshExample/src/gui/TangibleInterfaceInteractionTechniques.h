#ifndef __TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__
#define __TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__

#include <functional>

#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.h"
class MeshModel;

class TangibleInterfaceInteractionTechniques : public TangibleInterfaceListener {
private:
    static const char* RAY_CUBE_TANGIBLE_ELEMENT_ID;

    MeshModel* model;
    std::function<void()> repaintCallback;
    TangibleInterfaceEvent2RayGizmoMapper mapper;

public:
    TangibleInterfaceInteractionTechniques(MeshModel* model, std::function<void()> repaintCallback);

    virtual void tangibleInterfaceEventReceived(const TangibleInterfaceEvent& event) override;
};

#endif
