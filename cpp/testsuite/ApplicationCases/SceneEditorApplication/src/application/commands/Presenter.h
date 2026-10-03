#ifndef __PRESENTER__
#define __PRESENTER__

#include "java/lang/String.h"

/**
What the GUI technology presents for the `GuiEventExecutor`.
*/
class Presenter {
public:
    virtual ~Presenter() {}

    /**
    @param message text to show in the status bar
    */
    virtual void showStatusMessage(const java::String& message) = 0;
};

#endif
