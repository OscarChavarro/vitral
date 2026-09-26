#include "gui/xm/XmFileDialog.h"
#include "gui/xm/XmUiFactory.h"

XtUiFactory* createUiFactory()
{
    return new XmUiFactory();
}

XtWidgetSet* XmUiFactory::getWidgetSet()
{
    return &widgetSet;
}

const char* XmUiFactory::getResourceFile()
{
    return nullptr;
}

bool XmUiFactory::showFileDialog(XtApplicationHost* host,
                                 const std::string& title,
                                 const std::string& folder,
                                 const std::vector<FileSuffixFilter>& filters,
                                 std::string& outPath)
{
    return XmFileDialog::showDialog(host, title, folder, filters, outPath);
}
