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
    const size_t commands = text.find("\"commands\"");
    if (commands == std::string::npos) return std::map<std::string, std::string>();
    at = text.find('[', commands);
    if (at == std::string::npos) return std::map<std::string, std::string>();
    return readCommandArray();
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
    std::vector<std::string> commands;
    const size_t groups = text.find("\"buttonGroups\"");
    if (groups == std::string::npos) return commands;
    at = text.find('[', groups);
    if (at == std::string::npos) return commands;
    consume('[');
    while (!consume(']')) {
        std::string groupName;
        std::vector<std::string> groupCommands;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            if (key == "name") groupName = stringValue();
            else if (key == "commands") groupCommands = readStrings();
            else skipValue();
            consume(',');
        }
        if (groupName == name) return groupCommands;
        consume(',');
    }
    return commands;
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

std::map<std::string, std::string> GuiJsonReader::readCommandArray()
{
    std::map<std::string, std::string> result;
    consume('[');
    while (!consume(']')) {
        std::string id;
        std::string name;
        consume('{');
        while (!consume('}')) {
            const std::string key = stringValue(); consume(':');
            if (key == "id") id = stringValue();
            else if (key == "name") name = stringValue();
            else skipValue();
            consume(',');
        }
        if (!id.empty()) result[id] = name;
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
