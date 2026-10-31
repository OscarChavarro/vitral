#ifndef __GUI_JSON_READER__
#define __GUI_JSON_READER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/TreeMap.h"

/**
Node of a menu tree of the GUI definition (menubar or popup).
*/
struct GuiNode {
    java::String type;
    java::String name;
    java::ArrayList<java::String> modifiers;
    java::ArrayList<GuiNode> children;
};

/**
Command of the GUI definition: its identifier, texts and images.
*/
struct GuiCommand {
    java::String id;
    /// String properties: name, brief, icon, iconTransparency...
    java::TreeMap<java::String, java::String> properties;
    /// Lines of its help (a string or an array of them in the JSON)
    java::ArrayList<java::String> help;

    /**
    @return the value of a string property, or an empty string
    */
    java::String get(const java::String& property) const
    {
        java::String const* value = properties.get(property);
        return value != nullptr ? (*value) : java::String();
    }

    bool has(const java::String& property) const
    {
        return properties.containsKey(property);
    }
};

/**
Group of command buttons of the GUI definition (i.e. the ones of a tab of
the side panel).
*/
struct GuiButtonGroup {
    bool found;
    java::String name;
    java::String title;
    bool horizontal;
    bool showTitle;
    bool showIcons;
    bool showText;
    java::ArrayList<java::String> commands;

    GuiButtonGroup()
        : found(false), horizontal(false), showTitle(false),
          showIcons(false), showText(false) {}
};

/**
The Java application stores its GUI definition (menus, popups, commands and
messages) as a small JSON tree. This deliberately tiny reader keeps the native
application independent of another JSON library while importing that same
model at run time. It does not depend on any GUI technology.
*/
class GuiJsonReader {
public:
    /**
    @param text JSON document, referenced (it must outlive the reader)
    */
    explicit GuiJsonReader(const java::String& text) : text(text), position(0) {}

    GuiNode readMenuBar();

    /**
    @return the popup menus of the definition (i.e. the standard
    `VIEWPORT_SET_...` ones), or none
    */
    java::ArrayList<GuiNode> readPopups();

    /**
    @return command id -> command name
    */
    java::TreeMap<java::String, java::String> readCommandLabels();

    /**
    @return command id -> path of its icon, for the commands with one
    (relative to the folder of the Java application)
    */
    java::TreeMap<java::String, java::String> readCommandIcons();

    /**
    @return the commands of the definition, in order
    */
    java::ArrayList<GuiCommand> readCommands();

    /**
    @return the button groups of the definition, in order
    */
    java::ArrayList<GuiButtonGroup> readButtonGroups();

    /**
    @return message id -> message text
    */
    java::TreeMap<java::String, java::String> readMessages();

    /**
    @param name name of a button group (i.e. `CREATION`)
    @return the ids of its commands, in order, or none
    */
    java::ArrayList<java::String> readButtonGroupCommands(const java::String& name);

    /**
    @param name name of a button group (i.e. `GLOBAL`)
    @return the group, not `found` if there is none with that name
    */
    GuiButtonGroup readButtonGroup(const java::String& name);

private:
    const java::String& text;
    int position;

    void whitespace();
    bool consume(char c);
    java::String stringValue();
    bool booleanValue();
    void skipValueStart();
    void skipValue();
    java::ArrayList<java::String> readStrings();
    java::ArrayList<GuiNode> readNodes();
    java::TreeMap<java::String, java::String> readCommandProperty(
        const java::String& property);
    java::TreeMap<java::String, java::String> readCommandArray(
        const java::String& property);
    java::TreeMap<java::String, java::String> readStringMap();
    GuiNode readNode();
};

#endif
