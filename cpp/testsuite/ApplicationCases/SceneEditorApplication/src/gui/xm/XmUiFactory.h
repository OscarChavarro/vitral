#ifndef __XM_UI_FACTORY__
#define __XM_UI_FACTORY__

#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XmWidgetSet.h"

/**
The Motif GUI of the editor: the `XmWidgetSet`, with the own look of Motif
(no X resources file: the `XResources` of the application is the look of
its Athena GUI) and the `XmFileDialog`.
*/
class XmUiFactory : public XtUiFactory {
private:
    XmWidgetSet widgetSet;

public:
    virtual XtWidgetSet* getWidgetSet() override;

    /**
    @return null: Motif keeps its own look
    */
    virtual const char* getResourceFile() override;

    virtual bool showFileDialog(XtApplicationHost* host,
                                const std::string& title,
                                const std::string& folder,
                                const std::vector<FileSuffixFilter>& filters,
                                std::string& outPath) override;
};

#endif
