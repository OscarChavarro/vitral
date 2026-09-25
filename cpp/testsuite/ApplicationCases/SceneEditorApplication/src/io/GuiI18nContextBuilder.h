#ifndef __GUI_I18N_CONTEXT_BUILDER__
#define __GUI_I18N_CONTEXT_BUILDER__

#include <string>

class Widget;

/**
Builds the vitral I18N context (a `Widget`) from the JSON GUI definition
shared with the Java application: its popup menus (i.e. the standard
`VIEWPORT_SET_...` ones, that name the viewports and fill their menus), its
commands and its messages. The menubar is presented directly by the GUI
technology, so it is not imported.

WARNING: the X Toolkit also names a type `Widget`, so this header must not
be included in translation units using Xt.
*/
class GuiI18nContextBuilder {
public:
    /**
    @param json JSON GUI definition
    @return a new context, owned by the caller
    */
    static Widget* build(const std::string& json);

private:
    GuiI18nContextBuilder() {}
};

#endif
