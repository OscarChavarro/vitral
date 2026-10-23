#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "gui/xaw/XawFileDialog.h"
#include "gui/xaw/XawUiFactory.h"
#include "java/util/ArrayList.txx"

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
    const char* path = getenv("SCENE_EDITOR_XRESOURCES");
    return path != NULL ? path : "XResources";
}

BOOLEAN XawUiFactory::showFileDialog(XtApplicationHost* host,
                                  const String& title,
                                  const String& folder,
                                  const ArrayList<FileSuffixFilter>& filters,
                                  String& outPath)
{
    return XawFileDialog::showDialog(host, title, folder, filters, outPath);
}
