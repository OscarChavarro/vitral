#ifndef __TANGIBLE_INTERFACE_EVENT__
#define __TANGIBLE_INTERFACE_EVENT__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/PresentationElement.h"

class TangibleInterfaceEvent : public PresentationElement {
  private:
    String id;
    Vector3Dd position;
    Quaterniond rotation;

  public:
    TangibleInterfaceEvent();
    TangibleInterfaceEvent(const String& id, const Vector3Dd& position, const Quaterniond& rotation);

    const String& getId() const;
    const Vector3Dd& getPosition() const;
    const Quaterniond& getRotation() const;
    String toString() const;
};

#endif
