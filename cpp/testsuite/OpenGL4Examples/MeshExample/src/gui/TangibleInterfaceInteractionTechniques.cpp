#include "java/lang/Runnable.h"
#include "gui/TangibleInterfaceInteractionTechniques.h"
#include "model/MeshModel.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.h"

const char* TangibleInterfaceInteractionTechniques::RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";

TangibleInterfaceInteractionTechniques::TangibleInterfaceInteractionTechniques(
    MeshModel* model, java::Runnable repaintCallback)
    : model(model),
      repaintCallback(repaintCallback),
      mapper(model->getCamera())
{
}

void TangibleInterfaceInteractionTechniques::tangibleInterfaceEventReceived(const TangibleInterfaceEvent& event)
{
    if ( event.getId() != RAY_CUBE_TANGIBLE_ELEMENT_ID ) {
        return;
    }

    mapper.map(event, model->getRayGizmo());
    repaintCallback.run();
}
