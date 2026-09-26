#ifndef __GUI_I18N_CONTEXT_BUILDER__
#define __GUI_I18N_CONTEXT_BUILDER__

#include <string>

class Widget;

/**
Builds the vitral I18N context (a `Widget`) from the JSON GUI definition
shared with the Java application, as `GuiPersistence.importAquynzaGui` does
in the Java toolkit: its commands (with their icons), menubar, popup menus
(the submenus of both are registered as popups too), button groups and
messages. Variables and dialogs are not used by the editor and are not
imported.

WARNING: the X Toolkit also names a type `Widget`, so this header must not
be included in translation units using Xt.
*/
class GuiI18nContextBuilder {
public:
    /**
    @param json JSON GUI definition
    @param globalDataPath folder the paths of the icons are relative to
    @return a new context, owned by the caller
    */
    static Widget* build(const std::string& json,
                         const std::string& globalDataPath);

private:
    GuiI18nContextBuilder() {}
};

#endif
