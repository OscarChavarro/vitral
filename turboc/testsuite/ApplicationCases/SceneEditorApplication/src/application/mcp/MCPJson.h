#ifndef __MCP_JSON__
#define __MCP_JSON__

#include "vsdk/toolkit/common/VSDK.h"
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
    static BOOLEAN hasProperty(const String& json, const String& key);

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such string property
    @return value of the string property, still escaped
    */
    static String stringProperty(const String& json,
                                       const String& key,
                                       const String& defaultValue);

    /**
    @param json request
    @return the JSON-RPC id of the request, as written in it, or "null"
    */
    static String idProperty(const String& json);

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such number property
    @return value of the number property
    */
    static double numberProperty(const String& json,
                                 const String& key,
                                 double defaultValue);

    /**
    @param json request
    @param key name of the property
    @param outValue receives the value of the boolean property, if present
    @return false if the request has no such property (Java returns null)
    */
    static BOOLEAN booleanProperty(const String& json,
                                const String& key, BOOLEAN& outValue);

    //= Answers ===========================================================

    /**
    @param in text
    @return the text escaped to be written inside a JSON string
    */
    static String escape(const String& in);

    /**
    @param v vector
    @return JSON object with the coordinates of the vector
    */
    static String vector(const Vector3Dd& v);

    /**
    @param c color
    @return JSON object with the components of the color
    */
    static String color(const ColorRgb& c);

    /**
    @param path absolute path of a written file
    @return the answer of the tools that write a file
    */
    static String writtenFile(const String& path);

    /**
    @param name name of a tool
    @param description description of the tool
    @return the description of the tool for `tools/list`
    */
    static String tool(const char* name, const char* description);

    /**
    @param json answer of a tool
    @return the answer wrapped as the text content of a `tools/call` result
    */
    static String content(const String& json);

    /**
    @param id JSON-RPC id of the request
    @param json result
    @return JSON-RPC result answer
    */
    static String result(const String& id,
                               const String& json);

    /**
    @param id JSON-RPC id of the request
    @param code JSON-RPC error code
    @param message error message
    @return JSON-RPC error answer
    */
    static String error(const String& id, int code,
                              const String& message);
};

#endif
