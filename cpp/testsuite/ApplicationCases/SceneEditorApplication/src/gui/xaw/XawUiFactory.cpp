#include <cstdlib>

#include "gui/xaw/XawFileDialog.h"
#include "gui/xaw/XawUiFactory.h"

XtUiFactory* createUiFactory()
{
    return new XawUiFactory();
}

XtWidgetSet* XawUiFactory::getWidgetSet()
{
    return &widgetSet;
}

const char* XawUiFactory::getResourceFile()
{
    const char* path = std::getenv("SCENE_EDITOR_XRESOURCES");
    return path != nullptr ? path : "XResources";
}

bool XawUiFactory::showFileDialog(XtApplicationHost* host,
                                  const std::string& title,
                                  const std::string& folder,
                                  const std::vector<FileSuffixFilter>& filters,
                                  std::string& outPath)
{
    return XawFileDialog::showDialog(host, title, folder, filters, outPath);
}
