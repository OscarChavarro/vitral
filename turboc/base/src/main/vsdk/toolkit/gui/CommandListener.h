#ifndef __COMMAND_LISTENER__
#define __COMMAND_LISTENER__

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/PresentationElement.h"

/**
This class represents the concept of event language as explained in section
[FOLE1992.10.6] and figure [FOLE1992.10.24].
*/
class CommandListener : public PresentationElement {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~CommandListener() {}
    virtual bool executeCommand(const String& commandId) = 0;
};

#endif
