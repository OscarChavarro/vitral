#include "gui/xm/XmFileDialog.h"
#include "gui/xm/XmUiFactory.h"
#include "java/util/ArrayList.txx"

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
    return NULL;
}

bool XmUiFactory::showFileDialog(XtApplicationHost* host,
                                 const String& title,
                                 const String& folder,
                                 const ArrayList<FileSuffixFilter>& filters,
                                 String& outPath)
{
    return XmFileDialog::showDialog(host, title, folder, filters, outPath);
}
