#include <stdlib.h>

#include "java/lang/IllegalArgumentException.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.h"
#include "java/util/TreeSet.h"
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
    String stringValue;
    ArrayList<JsonValue> items;
    TreeMap<String, JsonValue> members;

    JsonValue() : type(NUL), booleanValue(false), numberValue(0) {}

    /**
    @return the member with the name, or null if this is not an object or
    it has no such member
    */
    const JsonValue* get(const String& name) const
    {
        if ( type != OBJECT ) {
            return NULL;
        }
        return members.get(name);
    }
};

/**
Recursive descent reader of a JSON document (RFC 8259).
*/
class JsonReader {
private:
    // The characters and the length of the text are cached, as
    // `String::size` counts them on each call
    const char* text;
    long length;
    long position;

    void fail(const char* problem) const
    {
        throw IllegalArgumentException(String("dialogs.json: ") + problem +
            " at offset " + Cpp98Support::toString(position));
    }

    void skipBlanks()
    {
        while ( position < length &&
                (text[position] == ' ' || text[position] == '\t' ||
                 text[position] == '\n' || text[position] == '\r') ) {
            position++;
        }
    }

    bool consume(char expected)
    {
        skipBlanks();
        if ( position < length && text[position] == expected ) {
            position++;
            return true;
        }
        return false;
    }

    void expect(char expected)
    {
        if ( !consume(expected) ) {
            StringBuilder problem;
            problem.append("expected '").append(expected).append("'");
            fail(problem.toString().c_str());
        }
    }

    bool consumeWord(const char* word)
    {
        long wordLength = (long)strlen(word);
        if ( position + wordLength <= length &&
             strncmp(text + position, word, (size_t)wordLength) == 0 ) {
            position += wordLength;
            return true;
        }
        return false;
    }

    static void appendUtf8(StringBuilder& out, unsigned long code)
    {
        if ( code < 0x80 ) {
            out.append(((char)(code)));
        }
        else if ( code < 0x800 ) {
            out.append(((char)(0xC0 | (code >> 6))));
            out.append(((char)(0x80 | (code & 0x3F))));
        }
        else if ( code < 0x10000 ) {
            out.append(((char)(0xE0 | (code >> 12))));
            out.append(((char)(0x80 | ((code >> 6) & 0x3F))));
            out.append(((char)(0x80 | (code & 0x3F))));
        }
        else {
            out.append(((char)(0xF0 | (code >> 18))));
            out.append(((char)(0x80 | ((code >> 12) & 0x3F))));
            out.append(((char)(0x80 | ((code >> 6) & 0x3F))));
            out.append(((char)(0x80 | (code & 0x3F))));
        }
    }

    unsigned long readHex4()
    {
        if ( position + 4 > length ) {
            fail("truncated unicode escape");
        }
        char digits[5];
        strncpy(digits, text + position, 4);
        digits[4] = '\0';
        char* end = NULL;
        unsigned long code = strtoul(digits, &end, 16);
        if ( end != digits + 4 ) {
            fail("bad unicode escape");
        }
        position += 4;
        return code;
    }

    String readString()
    {
        expect('"');
        StringBuilder out;
        while ( true ) {
            if ( position >= length ) {
                fail("unterminated string");
            }
            char c = text[position++];
            if ( c == '"' ) {
                return out.toString();
            }
            if ( c != '\\' ) {
                out.append(c);
                continue;
            }
            if ( position >= length ) {
                fail("unterminated string");
            }
            char escape = text[position++];
            switch ( escape ) {
              case '"': out.append('"'); break;
              case '\\': out.append('\\'); break;
              case '/': out.append('/'); break;
              case 'b': out.append('\b'); break;
              case 'f': out.append('\f'); break;
              case 'n': out.append('\n'); break;
              case 'r': out.append('\r'); break;
              case 't': out.append('\t'); break;
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
        if ( position >= length ) {
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
                String name = readString();
                expect(':');
                value.members.put(name, readValue());
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
                value.items.add(readValue());
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
            const char* start = text + position;
            char* end = NULL;
            value.numberValue = strtod(start, &end);
            if ( end == start ) {
                fail("unexpected character");
            }
            value.type = JsonValue::NUMBER;
            position += ((long)(end - start));
        }
        return value;
    }

public:
    explicit JsonReader(const String& source) :
        text(source.c_str()), length(source.length()), position(0) {}

    JsonValue readDocument()
    {
        JsonValue value = readValue();
        skipBlanks();
        if ( position != length ) {
            fail("unexpected content after the document");
        }
        return value;
    }
};

static void invalid(const String& message)
{
    throw IllegalArgumentException(message);
}

/**
@return the text of a non blank string member, or an empty string (null in
Java)
*/
static bool isBlank(const String& text)
{
    for ( const char* c = text.c_str(); *c != '\0'; c++ ) {
        if ( strchr(" \t\r\n", *c) == NULL ) {
            return false;
        }
    }
    return true;
}

static String optional(const JsonValue& node, const char* field)
{
    const JsonValue* value = node.get(field);
    if ( value == NULL || value->type != JsonValue::STRING ||
         isBlank(value->stringValue) ) {
        return "";
    }
    return value->stringValue;
}

static String required(const JsonValue& node, const char* field,
                     const String& path)
{
    String value = optional(node, field);
    if ( value.isEmpty() ) {
        invalid(path + ": missing " + field);
    }
    return value;
}

static bool isOneOf(const String& kind, const char* const* kinds)
{
    for ( int i = 0; kinds[i] != NULL; i++ ) {
        if ( kind == kinds[i] ) {
            return true;
        }
    }
    return false;
}

static WidgetDialogElement element(const JsonValue& node, const String& path,
                            TreeSet<String>& ids)
{
    static const char* const KINDS[] = { "column", "row", "textInput",
        "button", "tree", "action", "fileChooser", "fileFilter", NULL };
    static const char* const BOUND_KINDS[] = { "textInput", "tree",
        "fileFilter", NULL };
    static const char* const ACTION_KINDS[] = { "button", "action", NULL };

    String kind = required(node, "kind", path);
    if ( !isOneOf(kind, KINDS) ) {
        invalid(path + ": unsupported kind " + kind);
    }
    String id = optional(node, "id");
    if ( !id.isEmpty() && !ids.add(id) ) {
        invalid(path + ": duplicate element id " + id);
    }
    String binding = optional(node, "binding");
    String action = optional(node, "action");
    if ( isOneOf(kind, BOUND_KINDS) && binding.isEmpty() ) {
        invalid(path + ": missing binding");
    }
    if ( isOneOf(kind, ACTION_KINDS) && action.isEmpty() ) {
        invalid(path + ": missing action");
    }
    ArrayList<WidgetDialogElement> children;
    const JsonValue* childNodes = node.get("children");
    if ( childNodes != NULL && childNodes->type == JsonValue::ARRAY ) {
        for ( long i = 0; i < childNodes->items.size(); i++ ) {
            children.add(element(childNodes->items[i],
                path + ".children[" + Cpp98Support::toString(i) + "]", ids));
        }
    }
    return WidgetDialogElement(kind, id, optional(node, "labelKey"),
                               binding, action, children);
}


void DialogPersistence::importDialogs(const String& source,
                                      Widget* widget)
{
    JsonValue root = JsonReader(source).readDocument();
    const JsonValue* version = root.get("schemaVersion");
    if ( version == NULL || version->type != JsonValue::NUMBER ||
         version->numberValue != 1 ) {
        invalid("dialogs.json: unsupported schemaVersion");
    }
    const JsonValue* dialogs = root.get("dialogs");
    if ( dialogs == NULL || dialogs->type != JsonValue::ARRAY ) {
        invalid("dialogs.json: dialogs must be an array");
    }

    TreeSet<String> ids;
    for ( long i = 0; i < dialogs->items.size(); i++ ) {
        String path = "dialogs[" + Cpp98Support::toString(i) + "]";
        const JsonValue& node = dialogs->items[i];
        String id = required(node, "id", path);
        if ( !ids.add(id) ||
             widget->getDialogById(id) != NULL ) {
            invalid(path + ": duplicate dialog id " + id);
        }
        String titleKey = required(node, "titleKey", path);
        const JsonValue* modal = node.get("modal");
        const JsonValue* content = node.get("content");
        if ( content == NULL || content->type != JsonValue::ARRAY ) {
            invalid(path + ".content: expected array");
        }
        TreeSet<String> elementIds;
        ArrayList<WidgetDialogElement> elements;
        for ( long j = 0; j < content->items.size(); j++ ) {
            elements.add(element(content->items[j],
                path + ".content[" + Cpp98Support::toString(j) + "]", elementIds));
        }

        WidgetDialog* dialog = new WidgetDialog();
        dialog->setId(id);
        dialog->setTitleKey(titleKey);
        dialog->setModal(modal != NULL &&
                         modal->type == JsonValue::BOOLEAN &&
                         modal->booleanValue);
        for ( long j = 0; j < elements.size(); j++ ) {
            dialog->addElement(elements[j]);
        }
        widget->addDialog(dialog);
    }
}
