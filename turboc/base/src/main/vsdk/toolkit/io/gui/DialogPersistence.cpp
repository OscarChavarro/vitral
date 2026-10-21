#include <stdlib.h>
#include <map>
#include <set>
#include <stdexcept>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/io/gui/DialogPersistence.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include <string.h>


/**
JSON value, as read by `JsonReader`.
*/
struct JsonValue {
    enum Type { NUL, BOOLEAN, NUMBER, STRING, ARRAY, OBJECT };

    Type type;
    bool booleanValue;
    double numberValue;
    std::string stringValue;
    std::vector<JsonValue> items;
    std::map<std::string, JsonValue> members;

    JsonValue() : type(NUL), booleanValue(false), numberValue(0) {}

    /**
    @return the member with the name, or null if this is not an object or
    it has no such member
    */
    const JsonValue* get(const std::string& name) const
    {
        if ( type != OBJECT ) {
            return NULL;
        }
        std::map<std::string, JsonValue>::const_iterator member =
            members.find(name);
        return member != members.end() ? &member->second : NULL;
    }
};

/**
Recursive descent reader of a JSON document (RFC 8259).
*/
class JsonReader {
private:
    const std::string& text;
    size_t position;

    void fail(const char* problem) const
    {
        throw std::invalid_argument(std::string("dialogs.json: ") + problem +
            " at offset " + Cpp98Support::toString(position));
    }

    void skipBlanks()
    {
        while ( position < text.size() &&
                (text[position] == ' ' || text[position] == '\t' ||
                 text[position] == '\n' || text[position] == '\r') ) {
            position++;
        }
    }

    bool consume(char expected)
    {
        skipBlanks();
        if ( position < text.size() && text[position] == expected ) {
            position++;
            return true;
        }
        return false;
    }

    void expect(char expected)
    {
        if ( !consume(expected) ) {
            fail((std::string("expected '") + expected + "'").c_str());
        }
    }

    bool consumeWord(const char* word)
    {
        size_t length = strlen(word);
        if ( text.compare(position, length, word) == 0 ) {
            position += length;
            return true;
        }
        return false;
    }

    static void appendUtf8(std::string& out, unsigned long code)
    {
        if ( code < 0x80 ) {
            out += ((char)(code));
        }
        else if ( code < 0x800 ) {
            out += ((char)(0xC0 | (code >> 6)));
            out += ((char)(0x80 | (code & 0x3F)));
        }
        else if ( code < 0x10000 ) {
            out += ((char)(0xE0 | (code >> 12)));
            out += ((char)(0x80 | ((code >> 6) & 0x3F)));
            out += ((char)(0x80 | (code & 0x3F)));
        }
        else {
            out += ((char)(0xF0 | (code >> 18)));
            out += ((char)(0x80 | ((code >> 12) & 0x3F)));
            out += ((char)(0x80 | ((code >> 6) & 0x3F)));
            out += ((char)(0x80 | (code & 0x3F)));
        }
    }

    unsigned long readHex4()
    {
        if ( position + 4 > text.size() ) {
            fail("truncated unicode escape");
        }
        std::string digits = text.substr(position, 4);
        char* end = NULL;
        unsigned long code = strtoul(digits.c_str(), &end, 16);
        if ( end != digits.c_str() + 4 ) {
            fail("bad unicode escape");
        }
        position += 4;
        return code;
    }

    std::string readString()
    {
        expect('"');
        std::string out;
        while ( true ) {
            if ( position >= text.size() ) {
                fail("unterminated string");
            }
            char c = text[position++];
            if ( c == '"' ) {
                return out;
            }
            if ( c != '\\' ) {
                out += c;
                continue;
            }
            if ( position >= text.size() ) {
                fail("unterminated string");
            }
            char escape = text[position++];
            switch ( escape ) {
              case '"': out += '"'; break;
              case '\\': out += '\\'; break;
              case '/': out += '/'; break;
              case 'b': out += '\b'; break;
              case 'f': out += '\f'; break;
              case 'n': out += '\n'; break;
              case 'r': out += '\r'; break;
              case 't': out += '\t'; break;
              case 'u': {
                unsigned long code = readHex4();
                // Surrogate pair
                if ( code >= 0xD800 && code <= 0xDBFF &&
                     consumeWord("\\u") ) {
                    unsigned long low = readHex4();
                    code = 0x10000 + ((code - 0xD800) << 10) + (low - 0xDC00);
                }
                appendUtf8(out, code);
                break;
              }
              default:
                fail("bad escape");
            }
        }
    }

    JsonValue readValue()
    {
        skipBlanks();
        if ( position >= text.size() ) {
            fail("unexpected end");
        }
        JsonValue value;
        char c = text[position];
        if ( c == '{' ) {
            position++;
            value.type = JsonValue::OBJECT;
            if ( consume('}') ) {
                return value;
            }
            do {
                skipBlanks();
                std::string name = readString();
                expect(':');
                value.members[name] = readValue();
            } while ( consume(',') );
            expect('}');
        }
        else if ( c == '[' ) {
            position++;
            value.type = JsonValue::ARRAY;
            if ( consume(']') ) {
                return value;
            }
            do {
                value.items.push_back(readValue());
            } while ( consume(',') );
            expect(']');
        }
        else if ( c == '"' ) {
            value.type = JsonValue::STRING;
            value.stringValue = readString();
        }
        else if ( consumeWord("true") ) {
            value.type = JsonValue::BOOLEAN;
            value.booleanValue = true;
        }
        else if ( consumeWord("false") ) {
            value.type = JsonValue::BOOLEAN;
        }
        else if ( consumeWord("null") ) {
            value.type = JsonValue::NUL;
        }
        else {
            const char* start = text.c_str() + position;
            char* end = NULL;
            value.numberValue = strtod(start, &end);
            if ( end == start ) {
                fail("unexpected character");
            }
            value.type = JsonValue::NUMBER;
            position += ((size_t)(end - start));
        }
        return value;
    }

public:
    explicit JsonReader(const std::string& text) : text(text), position(0) {}

    JsonValue readDocument()
    {
        JsonValue value = readValue();
        skipBlanks();
        if ( position != text.size() ) {
            fail("unexpected content after the document");
        }
        return value;
    }
};

static void invalid(const std::string& message)
{
    throw std::invalid_argument(message);
}

/**
@return the text of a non blank string member, or an empty string (null in
Java)
*/
static std::string optional(const JsonValue& node, const char* field)
{
    const JsonValue* value = node.get(field);
    if ( value == NULL || value->type != JsonValue::STRING ||
         value->stringValue.find_first_not_of(" \t\r\n") == std::string::npos ) {
        return "";
    }
    return value->stringValue;
}

static std::string required(const JsonValue& node, const char* field,
                     const std::string& path)
{
    std::string value = optional(node, field);
    if ( value.empty() ) {
        invalid(path + ": missing " + field);
    }
    return value;
}

static bool isOneOf(const std::string& kind, const char* const* kinds)
{
    for ( int i = 0; kinds[i] != NULL; i++ ) {
        if ( kind == kinds[i] ) {
            return true;
        }
    }
    return false;
}

static WidgetDialogElement element(const JsonValue& node, const std::string& path,
                            std::set<std::string>& ids)
{
    static const char* const KINDS[] = { "column", "row", "textInput",
        "button", "tree", "action", "fileChooser", "fileFilter", NULL };
    static const char* const BOUND_KINDS[] = { "textInput", "tree",
        "fileFilter", NULL };
    static const char* const ACTION_KINDS[] = { "button", "action", NULL };

    std::string kind = required(node, "kind", path);
    if ( !isOneOf(kind, KINDS) ) {
        invalid(path + ": unsupported kind " + kind);
    }
    std::string id = optional(node, "id");
    if ( !id.empty() && !ids.insert(id).second ) {
        invalid(path + ": duplicate element id " + id);
    }
    std::string binding = optional(node, "binding");
    std::string action = optional(node, "action");
    if ( isOneOf(kind, BOUND_KINDS) && binding.empty() ) {
        invalid(path + ": missing binding");
    }
    if ( isOneOf(kind, ACTION_KINDS) && action.empty() ) {
        invalid(path + ": missing action");
    }
    std::vector<WidgetDialogElement> children;
    const JsonValue* childNodes = node.get("children");
    if ( childNodes != NULL && childNodes->type == JsonValue::ARRAY ) {
        for ( size_t i = 0; i < childNodes->items.size(); i++ ) {
            children.push_back(element(childNodes->items[i],
                path + ".children[" + Cpp98Support::toString(i) + "]", ids));
        }
    }
    return WidgetDialogElement(kind.c_str(), id.c_str(),
                               optional(node, "labelKey").c_str(),
                               binding.c_str(), action.c_str(), children);
}


void DialogPersistence::importDialogs(const String& source,
                                      Widget* widget)
{
    std::string text(source.c_str());
    JsonValue root = JsonReader(text).readDocument();
    const JsonValue* version = root.get("schemaVersion");
    if ( version == NULL || version->type != JsonValue::NUMBER ||
         version->numberValue != 1 ) {
        invalid("dialogs.json: unsupported schemaVersion");
    }
    const JsonValue* dialogs = root.get("dialogs");
    if ( dialogs == NULL || dialogs->type != JsonValue::ARRAY ) {
        invalid("dialogs.json: dialogs must be an array");
    }

    std::set<std::string> ids;
    for ( size_t i = 0; i < dialogs->items.size(); i++ ) {
        std::string path = "dialogs[" + Cpp98Support::toString(i) + "]";
        const JsonValue& node = dialogs->items[i];
        std::string id = required(node, "id", path);
        if ( !ids.insert(id).second ||
             widget->getDialogById(id.c_str()) != NULL ) {
            invalid(path + ": duplicate dialog id " + id);
        }
        std::string titleKey = required(node, "titleKey", path);
        const JsonValue* modal = node.get("modal");
        const JsonValue* content = node.get("content");
        if ( content == NULL || content->type != JsonValue::ARRAY ) {
            invalid(path + ".content: expected array");
        }
        std::set<std::string> elementIds;
        std::vector<WidgetDialogElement> elements;
        for ( size_t j = 0; j < content->items.size(); j++ ) {
            elements.push_back(element(content->items[j],
                path + ".content[" + Cpp98Support::toString(j) + "]", elementIds));
        }

        WidgetDialog* dialog = new WidgetDialog();
        dialog->setId(id.c_str());
        dialog->setTitleKey(titleKey.c_str());
        dialog->setModal(modal != NULL &&
                         modal->type == JsonValue::BOOLEAN &&
                         modal->booleanValue);
        for ( size_t j = 0; j < elements.size(); j++ ) {
            dialog->addElement(elements[j]);
        }
        widget->addDialog(dialog);
    }
}
