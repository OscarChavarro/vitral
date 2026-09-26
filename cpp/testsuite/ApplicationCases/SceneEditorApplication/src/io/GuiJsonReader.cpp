#include <cctype>
#include <stdexcept>

#include "io/GuiJsonReader.h"

GuiNode GuiJsonReader::readMenuBar()
{
    at = 0;
    skipValueStart();
    GuiNode root = readNode();
    return root;
}

std::vector<GuiNode> GuiJsonReader::readPopups()
{
    const size_t popups = text.find("\"popups\"");
    if (popups == std::string::npos) return std::vector<GuiNode>();
    at = text.find('[', popups);
    if (at == std::string::npos) return std::vector<GuiNode>();
    return readNodes();
}

std::map<std::string, std::string> GuiJsonReader::readCommandLabels()
{
    return readCommandProperty("name");
}

std::map<std::string, std::string> GuiJsonReader::readCommandIcons()
{
    return readCommandProperty("icon");
}

std::map<std::string, std::string> GuiJsonReader::readCommandProperty(
    const std::string& property)
{
    const size_t commands = text.find("\"commands\"");
    if (commands == std::string::npos) return std::map<std::string, std::string>();
    at = text.find('[', commands);
    if (at == std::string::npos) return std::map<std::string, std::string>();
    return readCommandArray(property);
}

std::map<std::string, std::string> GuiJsonReader::readMessages()
{
    const size_t messages = text.find("\"messages\"");
    if (messages == std::string::npos) return std::map<std::string, std::string>();
    at = text.find('{', messages);
    if (at == std::string::npos) return std::map<std::string, std::string>();
    return readStringMap();
}

std::vector<std::string> GuiJsonReader::readButtonGroupCommands(
    const std::string& name)
{
    return readButtonGroup(name).commands;
}

std::vector<GuiCommand> GuiJsonReader::readCommands()
{
    std::vector<GuiCommand> result;
    const size_t commands = text.find("\"commands\"");
    if (commands == std::string::npos) return result;
    at = text.find('[', commands);
    if (at == std::string::npos) return result;
    consume('[');
    while (!consume(']')) {
        GuiCommand command;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            whitespace();
            if (key == "id") command.id = stringValue();
            else if (key == "help" && at < text.size() && text[at] == '[') command.help = readStrings();
            else if (key == "help") command.help.push_back(stringValue());
            else if (at < text.size() && text[at] == '\"') command.properties[key] = stringValue();
            else skipValue();
            consume(',');
        }
        if (!command.id.empty()) result.push_back(command);
        consume(',');
    }
    return result;
}

std::vector<GuiButtonGroup> GuiJsonReader::readButtonGroups()
{
    std::vector<GuiButtonGroup> result;
    const size_t groups = text.find("\"buttonGroups\"");
    if (groups == std::string::npos) return result;
    at = text.find('[', groups);
    if (at == std::string::npos) return result;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            if (key == "name") group.name = stringValue();
            else if (key == "title") group.title = stringValue();
            else if (key == "direction") group.horizontal = stringValue() == "horizontal";
            else if (key == "showTitle") group.showTitle = booleanValue();
            else if (key == "showIcons") group.showIcons = booleanValue();
            else if (key == "showText") group.showText = booleanValue();
            else if (key == "commands") group.commands = readStrings();
            else skipValue();
            consume(',');
        }
        group.found = true;
        result.push_back(group);
        consume(',');
    }
    return result;
}

GuiButtonGroup GuiJsonReader::readButtonGroup(const std::string& name)
{
    GuiButtonGroup notFound;
    const size_t groups = text.find("\"buttonGroups\"");
    if (groups == std::string::npos) return notFound;
    at = text.find('[', groups);
    if (at == std::string::npos) return notFound;
    consume('[');
    while (!consume(']')) {
        GuiButtonGroup group;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            if (key == "name") group.name = stringValue();
            else if (key == "title") group.title = stringValue();
            else if (key == "direction") group.horizontal = stringValue() == "horizontal";
            else if (key == "showTitle") group.showTitle = booleanValue();
            else if (key == "showIcons") group.showIcons = booleanValue();
            else if (key == "showText") group.showText = booleanValue();
            else if (key == "commands") group.commands = readStrings();
            else skipValue();
            consume(',');
        }
        if (group.name == name) {
            group.found = true;
            return group;
        }
        consume(',');
    }
    return notFound;
}

bool GuiJsonReader::booleanValue()
{
    whitespace();
    if (text.compare(at, 4, "true") == 0) { at += 4; return true; }
    if (text.compare(at, 5, "false") == 0) { at += 5; return false; }
    skipValue();
    return false;
}

void GuiJsonReader::whitespace()
{
    while (at < text.size() && std::isspace(static_cast<unsigned char>(text[at]))) ++at;
}

bool GuiJsonReader::consume(char c)
{
    whitespace();
    if (at < text.size() && text[at] == c) { ++at; return true; }
    return false;
}

std::string GuiJsonReader::stringValue()
{
    whitespace();
    if (at >= text.size() || text[at++] != '\"') throw std::runtime_error("Invalid GUI JSON string");
    std::string result;
    while (at < text.size() && text[at] != '\"') {
        char c = text[at++];
        if (c == '\\' && at < text.size()) {
            c = text[at++];
            if (c == 'n') result += '\n';
            else if (c == 't') result += '\t';
            else result += c;
        }
        else result += c;
    }
    if (at >= text.size()) throw std::runtime_error("Unterminated GUI JSON string");
    ++at;
    return result;
}

void GuiJsonReader::skipValueStart()
{
    whitespace();
    if (!consume('{')) throw std::runtime_error("Invalid GUI JSON document");
    while (at < text.size()) {
        std::string key = stringValue();
        consume(':');
        if (key == "menubar") return;
        skipValue();
        if (!consume(',')) break;
    }
    throw std::runtime_error("GUI JSON has no menubar");
}

void GuiJsonReader::skipValue()
{
    whitespace();
    if (at >= text.size()) return;
    if (text[at] == '\"') { stringValue(); return; }
    char open = text[at];
    if (open != '{' && open != '[') { while (at < text.size() && text[at] != ',' && text[at] != '}' && text[at] != ']') ++at; return; }
    char close = open == '{' ? '}' : ']'; ++at;
    while (at < text.size()) { whitespace(); if (text[at] == close) { ++at; return; } skipValue(); whitespace(); if (at < text.size() && text[at] == ',') ++at; }
}

std::vector<std::string> GuiJsonReader::readStrings()
{
    std::vector<std::string> result;
    consume('[');
    while (!consume(']')) { result.push_back(stringValue()); consume(','); }
    return result;
}

std::vector<GuiNode> GuiJsonReader::readNodes()
{
    std::vector<GuiNode> result;
    consume('[');
    while (!consume(']')) { result.push_back(readNode()); consume(','); }
    return result;
}

std::map<std::string, std::string> GuiJsonReader::readCommandArray(
    const std::string& property)
{
    std::map<std::string, std::string> result;
    consume('[');
    while (!consume(']')) {
        std::string id;
        std::string value;
        bool found = false;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            if (key == "id") id = stringValue();
            else if (key == property) { value = stringValue(); found = true; }
            else skipValue();
            consume(',');
        }
        if (!id.empty() && found) result[id] = value;
        consume(',');
    }
    return result;
}

std::map<std::string, std::string> GuiJsonReader::readStringMap()
{
    std::map<std::string, std::string> result;
    consume('{');
    while (!consume('}')) {
        const std::string key = stringValue(); consume(':');
        result[key] = stringValue();
        consume(',');
    }
    return result;
}

GuiNode GuiJsonReader::readNode()
{
    GuiNode node;
    consume('{');
    while (!consume('}')) {
        std::string key = stringValue(); consume(':');
        if (key == "type") node.type = stringValue();
        else if (key == "name") node.name = stringValue();
        else if (key == "modifiers") node.modifiers = readStrings();
        else if (key == "children") node.children = readNodes();
        else skipValue();
        consume(',');
    }
    return node;
}
