#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
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
                                  const java::String& title,
                                  const java::String& folder,
                                  const java::ArrayList<FileSuffixFilter>& filters,
                                  java::String& outPath)
{
    return XawFileDialog::showDialog(host, title, folder, filters, outPath);
}
