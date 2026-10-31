#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
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
                                 const java::String& title,
                                 const java::String& folder,
                                 const java::ArrayList<FileSuffixFilter>& filters,
                                 java::String& outPath)
{
    return XmFileDialog::showDialog(host, title, folder, filters, outPath);
}
