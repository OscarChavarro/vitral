#ifndef __XAW_UI_FACTORY__
#define __XAW_UI_FACTORY__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XawWidgetSet.h"

/**
The Athena (Xaw) GUI of the editor: the `XawWidgetSet`, the black and white
look of the `XResources` file and the `XawFileDialog`.
*/
class XawUiFactory : public XtUiFactory {
private:
    XawWidgetSet widgetSet;

public:
    virtual XtWidgetSet* getWidgetSet() override;

    /**
    @return the file named by the `SCENE_EDITOR_XRESOURCES` environment
    variable, or "XResources" (in the working directory)
    */
    virtual const char* getResourceFile() override;

    virtual bool showFileDialog(XtApplicationHost* host,
                                const java::String& title,
                                const java::String& folder,
                                const java::ArrayList<FileSuffixFilter>& filters,
                                java::String& outPath) override;
};

#endif
