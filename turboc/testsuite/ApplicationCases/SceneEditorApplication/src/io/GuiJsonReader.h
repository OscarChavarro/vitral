#ifndef __GUI_JSON_READER__
#define __GUI_JSON_READER__

#include "java/util/TreeMap.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

/**
Node of a menu tree of the GUI definition (menubar or popup).
*/
struct GuiNode {
    String type;
    String name;
    ArrayList<String> modifiers;
    ArrayList<GuiNode> children;
};

/**
Command of the GUI definition: its identifier, texts and images.
*/
struct GuiCommand {
    String id;
    /// String properties: name, brief, icon, iconTransparency...
    TreeMap<String, String> properties;
    /// Lines of its help (a string or an array of them in the JSON)
    ArrayList<String> help;

    /**
    @return the value of a string property, or an empty string
    */
    String get(const String& property) const
    {
        const String* value = properties.get(property);
        return value != NULL ? (*value) : String();
    }

    bool has(const String& property) const
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
    String name;
    String title;
    bool horizontal;
    bool showTitle;
    bool showIcons;
    bool showText;
    ArrayList<String> commands;

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
    explicit GuiJsonReader(const String& text);

    GuiNode readMenuBar();

    /**
    @return the popup menus of the definition (i.e. the standard
    `VIEWPORT_SET_...` ones), or none
    */
    ArrayList<GuiNode> readPopups();

    /**
    @return command id -> command name
    */
    TreeMap<String, String> readCommandLabels();

    /**
    @return command id -> path of its icon, for the commands with one
    (relative to the folder of the Java application)
    */
    TreeMap<String, String> readCommandIcons();

    /**
    @return the commands of the definition, in order
    */
    ArrayList<GuiCommand> readCommands();

    /**
    @return the button groups of the definition, in order
    */
    ArrayList<GuiButtonGroup> readButtonGroups();

    /**
    @return message id -> message text
    */
    TreeMap<String, String> readMessages();

    /**
    @param name name of a button group (i.e. `CREATION`)
    @return the ids of its commands, in order, or none
    */
    ArrayList<String> readButtonGroupCommands(const String& name);

    /**
    @param name name of a button group (i.e. `GLOBAL`)
    @return the group, not `found` if there is none with that name
    */
    GuiButtonGroup readButtonGroup(const String& name);

private:
    const String& text;
    // The characters and the length of the text are cached, as
    // `String::size` counts them on each call
    const char* data;
    long length;
    long position;

    /**
    @return the index of the first occurrence of the token, or -1
    */
    long indexOf(const char* token) const;

    /**
    @return the index of the first occurrence of the character at or after
    `from`, or -1
    */
    long indexOf(char token, long from) const;

    void whitespace();
    bool consume(char c);
    String stringValue();
    bool booleanValue();
    void skipValueStart();
    void skipValue();
    ArrayList<String> readStrings();
    ArrayList<GuiNode> readNodes();
    TreeMap<String, String> readCommandProperty(
        const String& property);
    TreeMap<String, String> readCommandArray(
        const String& property);
    TreeMap<String, String> readStringMap();
    GuiNode readNode();
};

#endif
