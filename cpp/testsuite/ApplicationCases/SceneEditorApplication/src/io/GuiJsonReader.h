#ifndef __GUI_JSON_READER__
#define __GUI_JSON_READER__

#include <map>
#include <string>
#include <vector>

/**
Node of a menu tree of the GUI definition (menubar or popup).
*/
struct GuiNode {
    std::string type;
    std::string name;
    std::vector<std::string> modifiers;
    std::vector<GuiNode> children;
};

/**
Command of the GUI definition: its identifier, texts and images.
*/
struct GuiCommand {
    std::string id;
    /// String properties: name, brief, icon, iconTransparency...
    std::map<std::string, std::string> properties;
    /// Lines of its help (a string or an array of them in the JSON)
    std::vector<std::string> help;

    /**
    @return the value of a string property, or an empty string
    */
    std::string get(const std::string& property) const
    {
        std::map<std::string, std::string>::const_iterator value =
            properties.find(property);
        return value != properties.end() ? value->second : std::string();
    }

    bool has(const std::string& property) const
    {
        return properties.find(property) != properties.end();
    }
};

/**
Group of command buttons of the GUI definition (i.e. the ones of a tab of
the side panel).
*/
struct GuiButtonGroup {
    bool found;
    std::string name;
    std::string title;
    bool horizontal;
    bool showTitle;
    bool showIcons;
    bool showText;
    std::vector<std::string> commands;

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
    explicit GuiJsonReader(const std::string& text) : text(text), at(0) {}

    GuiNode readMenuBar();

    /**
    @return the popup menus of the definition (i.e. the standard
    `VIEWPORT_SET_...` ones), or none
    */
    std::vector<GuiNode> readPopups();

    /**
    @return command id -> command name
    */
    std::map<std::string, std::string> readCommandLabels();

    /**
    @return command id -> path of its icon, for the commands with one
    (relative to the folder of the Java application)
    */
    std::map<std::string, std::string> readCommandIcons();

    /**
    @return the commands of the definition, in order
    */
    std::vector<GuiCommand> readCommands();

    /**
    @return the button groups of the definition, in order
    */
    std::vector<GuiButtonGroup> readButtonGroups();

    /**
    @return message id -> message text
    */
    std::map<std::string, std::string> readMessages();

    /**
    @param name name of a button group (i.e. `CREATION`)
    @return the ids of its commands, in order, or none
    */
    std::vector<std::string> readButtonGroupCommands(const std::string& name);

    /**
    @param name name of a button group (i.e. `GLOBAL`)
    @return the group, not `found` if there is none with that name
    */
    GuiButtonGroup readButtonGroup(const std::string& name);

private:
    const std::string& text;
    size_t at;

    void whitespace();
    bool consume(char c);
    std::string stringValue();
    bool booleanValue();
    void skipValueStart();
    void skipValue();
    std::vector<std::string> readStrings();
    std::vector<GuiNode> readNodes();
    std::map<std::string, std::string> readCommandProperty(
        const std::string& property);
    std::map<std::string, std::string> readCommandArray(
        const std::string& property);
    std::map<std::string, std::string> readStringMap();
    GuiNode readNode();
};

#endif
