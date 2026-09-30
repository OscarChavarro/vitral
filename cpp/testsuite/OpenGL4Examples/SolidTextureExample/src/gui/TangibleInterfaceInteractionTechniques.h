#ifndef __SOLID_TEXTURE_TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__
#define __SOLID_TEXTURE_TANGIBLE_INTERFACE_INTERACTION_TECHNIQUES__

#include <functional>

#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2InfinitePlaneGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.h"

class SolidTextureModel;

class TangibleInterfaceInteractionTechniques : public TangibleInterfaceListener {
private:
    SolidTextureModel* model;
    std::function<void()> repaintCallback;
    TangibleInterfaceEvent2RayGizmoMapper toRayGizmoMapper;
    TangibleInterfaceEvent2InfinitePlaneGizmoMapper toInfinitePlaneGizmoMapper;

public:
    TangibleInterfaceInteractionTechniques(
        SolidTextureModel* model, const std::function<void()>& repaintCallback);
    virtual void tangibleInterfaceEventReceived(const TangibleInterfaceEvent& event);
};

#endif
