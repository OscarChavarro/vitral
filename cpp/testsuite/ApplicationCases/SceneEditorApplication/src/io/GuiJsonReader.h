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
    @return message id -> message text
    */
    std::map<std::string, std::string> readMessages();

    /**
    @param name name of a button group (i.e. `CREATION`)
    @return the ids of its commands, in order, or none
    */
    std::vector<std::string> readButtonGroupCommands(const std::string& name);

private:
    const std::string& text;
    size_t at;

    void whitespace();
    bool consume(char c);
    std::string stringValue();
    void skipValueStart();
    void skipValue();
    std::vector<std::string> readStrings();
    std::vector<GuiNode> readNodes();
    std::map<std::string, std::string> readCommandArray();
    std::map<std::string, std::string> readStringMap();
    GuiNode readNode();
};

#endif
