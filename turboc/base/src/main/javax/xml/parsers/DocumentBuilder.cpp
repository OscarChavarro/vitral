#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "javax/xml/parsers/DocumentBuilder.h"
#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"



class XmlTextParser {
private:
const String& text;
int position;
int length;
BOOLEAN failed;

BOOLEAN atEnd() const
{
    return position >= length;
}

char current() const
{
    return atEnd() ? '\0' : text.charAt(position);
}

BOOLEAN startsWithAt(const char* token) const
{
    int i;
    for ( i = 0; token[i] != '\0'; i++ ) {
        if ( position + i >= length || text.charAt(position + i) != token[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

static BOOLEAN isSpace(char c)
{
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

static BOOLEAN isNameChar(char c)
{
    return !isSpace(c) && c != '\0' && c != '=' && c != '>' &&
        c != '/' && c != '<' && c != '"' && c != '\'';
}

void skipSpaces()
{
    while ( !atEnd() && isSpace(current()) ) {
        position++;
    }
}

BOOLEAN skipPast(const char* token)
{
    while ( !atEnd() ) {
        if ( startsWithAt(token) ) {
            int i;
            for ( i = 0; token[i] != '\0'; i++ ) {
                position++;
            }
            return TRUE;
        }
        position++;
    }
    failed = TRUE;
    return FALSE;
}

void skipDoctype()
{
    // Skips "<!DOCTYPE ... >", including an optional internal subset.
    int depth = 0;
    while ( !atEnd() ) {
        char c = current();
        position++;
        if ( c == '[' ) {
            depth++;
        }
        else if ( c == ']' ) {
            depth--;
        }
        else if ( c == '>' && depth <= 0 ) {
            return;
        }
    }
    failed = TRUE;
}

String readName()
{
    String name;
    char buffer[2] = {'\0', '\0'};
    while ( !atEnd() && isNameChar(current()) ) {
        buffer[0] = current();
        name += buffer;
        position++;
    }
    return name;
}

static void appendCodePoint(String& out, unsigned long cp)
{
    char buffer[5] = {'\0', '\0', '\0', '\0', '\0'};
    if ( cp < 0x80 ) {
        buffer[0] = (char)cp;
    }
    else if ( cp < 0x800 ) {
        buffer[0] = (char)(0xC0 | (cp >> 6));
        buffer[1] = (char)(0x80 | (cp & 0x3F));
    }
    else if ( cp < 0x10000 ) {
        buffer[0] = (char)(0xE0 | (cp >> 12));
        buffer[1] = (char)(0x80 | ((cp >> 6) & 0x3F));
        buffer[2] = (char)(0x80 | (cp & 0x3F));
    }
    else {
        buffer[0] = (char)(0xF0 | (cp >> 18));
        buffer[1] = (char)(0x80 | ((cp >> 12) & 0x3F));
        buffer[2] = (char)(0x80 | ((cp >> 6) & 0x3F));
        buffer[3] = (char)(0x80 | (cp & 0x3F));
    }
    out += buffer;
}

static String decodeEntities(const String& raw)
{
    String out;
    char buffer[2] = {'\0', '\0'};
    int i = 0;
    int n = raw.length();
    while ( i < n ) {
        char c = raw.charAt(i);
        if ( c != '&' ) {
            buffer[0] = c;
            out += buffer;
            i++;
            continue;
        }
        int end = raw.indexOf(';', i);
        if ( end < 0 ) {
            buffer[0] = c;
            out += buffer;
            i++;
            continue;
        }
        String entity = raw.substring(i + 1, end);
        if ( entity.equals("lt") ) {
            out += "<";
        }
        else if ( entity.equals("gt") ) {
            out += ">";
        }
        else if ( entity.equals("amp") ) {
            out += "&";
        }
        else if ( entity.equals("quot") ) {
            out += "\"";
        }
        else if ( entity.equals("apos") ) {
            out += "'";
        }
        else if ( entity.length() > 1 && entity.charAt(0) == '#' ) {
            unsigned long cp;
            if ( entity.charAt(1) == 'x' || entity.charAt(1) == 'X' ) {
                cp = strtoul(entity.c_str() + 2, NULL, 16);
            }
            else {
                cp = strtoul(entity.c_str() + 1, NULL, 10);
            }
            appendCodePoint(out, cp);
        }
        else {
            out += raw.substring(i, end + 1);
        }
        i = end + 1;
    }
    return out;
}

static BOOLEAN isWhitespaceOnly(const String& s)
{
    int i;
    for ( i = 0; i < s.length(); i++ ) {
        if ( !isSpace(s.charAt(i)) ) {
            return FALSE;
        }
    }
    return TRUE;
}

void addText(Node* parent, const String& raw,
             BOOLEAN decode)
{
    if ( parent == NULL || isWhitespaceOnly(raw) ) {
        return;
    }
    Node* textNode = new Node(
        Node::TEXT_NODE, "#text",
        decode ? decodeEntities(raw) : raw);
    parent->appendChild(textNode);
}

BOOLEAN parseAttributes(Element* element, BOOLEAN* selfClosing)
{
    while ( TRUE ) {
        skipSpaces();
        if ( atEnd() ) {
            failed = TRUE;
            return FALSE;
        }
        if ( startsWithAt("/>") ) {
            position += 2;
            *selfClosing = TRUE;
            return TRUE;
        }
        if ( current() == '>' ) {
            position++;
            *selfClosing = FALSE;
            return TRUE;
        }
        String name = readName();
        if ( name.isEmpty() ) {
            failed = TRUE;
            return FALSE;
        }
        skipSpaces();
        if ( current() != '=' ) {
            failed = TRUE;
            return FALSE;
        }
        position++;
        skipSpaces();
        char quote = current();
        if ( quote != '"' && quote != '\'' ) {
            failed = TRUE;
            return FALSE;
        }
        position++;
        int start = position;
        while ( !atEnd() && current() != quote ) {
            position++;
        }
        if ( atEnd() ) {
            failed = TRUE;
            return FALSE;
        }
        element->setAttribute(name,
            decodeEntities(text.substring(start, position)));
        position++;
    }
}

public:
explicit XmlTextParser(const String& text)
    : text(text), position(0), length(text.length()), failed(FALSE)
{
}

Document* parse()
{
    Document* document = new Document();
    ArrayList<Node*> stack;
    Node* parent = document;

    while ( !atEnd() && !failed ) {
        if ( startsWithAt("<?") ) {
            skipPast("?>");
        }
        else if ( startsWithAt("<!--") ) {
            skipPast("-->");
        }
        else if ( startsWithAt("<![CDATA[") ) {
            position += 9;
            int start = position;
            if ( skipPast("]]>") ) {
                addText(parent, text.substring(start, position - 3),
                        FALSE);
            }
        }
        else if ( startsWithAt("<!") ) {
            skipDoctype();
        }
        else if ( startsWithAt("</") ) {
            position += 2;
            String name = readName();
            skipSpaces();
            if ( current() != '>' || stack.size() == 0 ||
                 !stack.get(stack.size() - 1)->getNodeName().equals(name) ) {
                failed = TRUE;
                break;
            }
            position++;
            stack.remove((long)(stack.size() - 1));
            parent = stack.size() == 0 ?
                ((Node*)(document)) :
                stack.get(stack.size() - 1);
        }
        else if ( current() == '<' ) {
            position++;
            String name = readName();
            if ( name.isEmpty() ) {
                failed = TRUE;
                break;
            }
            Element* element =
                new Element(name);
            parent->appendChild(element);
            BOOLEAN selfClosing = FALSE;
            if ( !parseAttributes(element, &selfClosing) ) {
                break;
            }
            if ( !selfClosing ) {
                stack.add(element);
                parent = element;
            }
        }
        else {
            int start = position;
            while ( !atEnd() && current() != '<' ) {
                position++;
            }
            if ( parent != document ) {
                addText(parent, text.substring(start, position), TRUE);
            }
        }
    }

    if ( failed || stack.size() != 0 ||
         document->getDocumentElement() == NULL ) {
        delete document;
        return NULL;
    }
    return document;
}
};


Document* DocumentBuilder::newDocument()
{
return new Document();
}

Document* DocumentBuilder::parseText(const String& text)
{
XmlTextParser parser(text);
return parser.parse();
}

Document* DocumentBuilder::parse(const File& file)
{
FILE* fd = fopen(file.getPath().c_str(), "rb");
if ( fd == NULL ) {
    return NULL;
}
ArrayList<char> bytes;
char buffer[4096];
size_t n;
while ( (n = fread(buffer, 1, sizeof(buffer), fd)) > 0 ) {
    size_t i;
    for ( i = 0; i < n; i++ ) {
        bytes.add(buffer[i]);
    }
}
fclose(fd);
bytes.add('\0');
return parseText(String(bytes.data()));
}

