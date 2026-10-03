#ifndef __MCP_JSON__
#define __MCP_JSON__

#include "java/lang/String.h"

class ColorRgb;
class Vector3Dd;

/**
Minimal JSON support of the automation service of the editor: reads the
properties of a request (by key, wherever they are in the request) and builds
the JSON-RPC answers. It is not a general JSON parser.
*/
class MCPJson {
private:
    MCPJson();

public:
    //= Request properties ================================================

    /**
    @param json request
    @param key name of the property
    @return true if the request has the property, whatever its value
    */
    static bool hasProperty(const java::String& json, const java::String& key);

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such string property
    @return value of the string property, still escaped
    */
    static java::String stringProperty(const java::String& json,
                                       const java::String& key,
                                       const java::String& defaultValue);

    /**
    @param json request
    @return the JSON-RPC id of the request, as written in it, or "null"
    */
    static java::String idProperty(const java::String& json);

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such number property
    @return value of the number property
    */
    static double numberProperty(const java::String& json,
                                 const java::String& key,
                                 double defaultValue);

    /**
    @param json request
    @param key name of the property
    @param outValue receives the value of the boolean property, if present
    @return false if the request has no such property (Java returns null)
    */
    static bool booleanProperty(const java::String& json,
                                const java::String& key, bool& outValue);

    //= Answers ===========================================================

    /**
    @param in text
    @return the text escaped to be written inside a JSON string
    */
    static java::String escape(const java::String& in);

    /**
    @param v vector
    @return JSON object with the coordinates of the vector
    */
    static java::String vector(const Vector3Dd& v);

    /**
    @param c color
    @return JSON object with the components of the color
    */
    static java::String color(const ColorRgb& c);

    /**
    @param path absolute path of a written file
    @return the answer of the tools that write a file
    */
    static java::String writtenFile(const java::String& path);

    /**
    @param name name of a tool
    @param description description of the tool
    @return the description of the tool for `tools/list`
    */
    static java::String tool(const char* name, const char* description);

    /**
    @param json answer of a tool
    @return the answer wrapped as the text content of a `tools/call` result
    */
    static java::String content(const java::String& json);

    /**
    @param id JSON-RPC id of the request
    @param json result
    @return JSON-RPC result answer
    */
    static java::String result(const java::String& id,
                               const java::String& json);

    /**
    @param id JSON-RPC id of the request
    @param code JSON-RPC error code
    @param message error message
    @return JSON-RPC error answer
    */
    static java::String error(const java::String& id, int code,
                              const java::String& message);
};

#endif
