package application.mcp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Minimal JSON support of the automation service of the editor: reads the
properties of a request (by key, wherever they are in the request) and builds
the JSON-RPC answers. It is not a general JSON parser.
*/
final class MCPJson
{
    private MCPJson()
    {
    }

    //= Request properties ================================================

    /**
    @param json request
    @param key name of the property
    @return true if the request has the property, whatever its value
    */
    static boolean hasProperty(String json, String key)
    {
        return Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:").matcher(json).find();
    }

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such string property
    @return value of the string property, still escaped
    */
    static String stringProperty(String json, String key, String defaultValue)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return defaultValue;
        }
        return matcher.group(1);
    }

    /**
    @param json request
    @return the JSON-RPC id of the request, as written in it, or "null"
    */
    static String idProperty(String json)
    {
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[-0-9]+|null)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return "null";
        }
        return matcher.group(1);
    }

    /**
    @param json request
    @param key name of the property
    @param defaultValue value when the request has no such number property
    @return value of the number property
    */
    static double numberProperty(String json, String key, double defaultValue)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return defaultValue;
        }
        return Double.parseDouble(matcher.group(1));
    }

    /**
    @param json request
    @param key name of the property
    @return value of the boolean property, or null if the request has none
    */
    static Boolean booleanProperty(String json, String key)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return null;
        }
        return Boolean.valueOf(matcher.group(1));
    }

    //= Answers ===========================================================

    /**
    @param in text
    @return the text escaped to be written inside a JSON string ("" for null)
    */
    static String escape(String in)
    {
        if ( in == null ) {
            return "";
        }
        return in.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
    @param v vector
    @return JSON object with the coordinates of the vector
    */
    static String vector(Vector3Dd v)
    {
        return "{\"x\":" + v.x() + ",\"y\":" + v.y() + ",\"z\":" + v.z() + "}";
    }

    /**
    @param c color
    @return JSON object with the components of the color
    */
    static String color(ColorRgb c)
    {
        return "{\"r\":" + c.r() + ",\"g\":" + c.g() + ",\"b\":" + c.b() + "}";
    }

    /**
    @param path absolute path of a written file
    @return the answer of the tools that write a file
    */
    static String writtenFile(String path)
    {
        return "{\"ok\":true,\"path\":\"" + escape(path) + "\"}";
    }

    /**
    @param name name of a tool
    @param description description of the tool
    @return the description of the tool for `tools/list`
    */
    static String tool(String name, String description)
    {
        return "{\"name\":\"" + name + "\",\"description\":\""
            + escape(description)
            + "\",\"inputSchema\":{\"type\":\"object\",\"additionalProperties\":true}}";
    }

    /**
    @param json answer of a tool
    @return the answer wrapped as the text content of a `tools/call` result
    */
    static String content(String json)
    {
        return "{\"content\":[{\"type\":\"text\",\"text\":\""
            + escape(json) + "\"}],\"isError\":false}";
    }

    /**
    @param id JSON-RPC id of the request
    @param json result
    @return JSON-RPC result answer
    */
    static String result(String id, String json)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + json + "}";
    }

    /**
    @param id JSON-RPC id of the request
    @param code JSON-RPC error code
    @param message error message
    @return JSON-RPC error answer
    */
    static String error(String id, int code, String message)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id
            + ",\"error\":{\"code\":" + code + ",\"message\":\""
            + escape(message) + "\"}}";
    }
}
