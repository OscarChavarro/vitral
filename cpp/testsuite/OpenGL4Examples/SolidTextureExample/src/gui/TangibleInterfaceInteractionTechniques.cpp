#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.h"
#include "gui/TangibleInterfaceInteractionTechniques.h"
#include "model/SolidTextureModel.h"

static const char* RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";
static const char* CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID = "cuttingPlane1";

TangibleInterfaceInteractionTechniques::TangibleInterfaceInteractionTechniques(
    SolidTextureModel* model, const std::function<void()>& repaintCallback)
    : model(model), repaintCallback(repaintCallback),
      toRayGizmoMapper(model != 0 ? model->getCamera() : 0),
      toInfinitePlaneGizmoMapper(model != 0 ? model->getCamera() : 0)
{
}

void TangibleInterfaceInteractionTechniques::tangibleInterfaceEventReceived(
    const TangibleInterfaceEvent& event)
{
    if ( model == 0 ) return;
    if ( event.getId().equals(RAY_CUBE_TANGIBLE_ELEMENT_ID) ) {
        toRayGizmoMapper.map(event, model->getRayGizmo());
    }
    else if ( event.getId().equals(CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID) ) {
        toInfinitePlaneGizmoMapper.map(event, model->getInfinitePlaneGizmo());
    }
    if ( repaintCallback ) repaintCallback();
}
