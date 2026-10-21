#include "java/lang/Double.h"
#include "java/lang/StringBuilder.h"
#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "application/mcp/MCPJson.h"


//= Request properties ====================================================

bool MCPJson::hasProperty(const String& json, const String& key)
{
    return Pattern::compile(String("\"") + Pattern::quote(key) +
        "\"\\s*:").matcher(json).find();
}

String MCPJson::stringProperty(const String& json,
                                     const String& key,
                                     const String& defaultValue)
{
    Pattern pattern = Pattern::compile(String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return matcher.group(1);
}

String MCPJson::idProperty(const String& json)
{
    Pattern pattern = Pattern::compile(
        "\"id\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[-0-9]+|null)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return "null";
    }
    return matcher.group(1);
}

double MCPJson::numberProperty(const String& json,
                               const String& key, double defaultValue)
{
    Pattern pattern = Pattern::compile(String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return Double::parseDouble(matcher.group(1));
}

bool MCPJson::booleanProperty(const String& json,
                              const String& key, bool& outValue)
{
    Pattern pattern = Pattern::compile(String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(true|false)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return false;
    }
    outValue = matcher.group(1).equals("true");
    return true;
}

//= Answers ===============================================================

String MCPJson::escape(const String& in)
{
    return in.replace("\\", "\\\\").replace("\"", "\\\"");
}

String MCPJson::vector(const Vector3Dd& v)
{
    StringBuilder sb;
    sb.append("{\"x\":").append(v.x()).append(",\"y\":").append(v.y())
        .append(",\"z\":").append(v.z()).append('}');
    return sb.toString();
}

String MCPJson::color(const ColorRgb& c)
{
    StringBuilder sb;
    sb.append("{\"r\":").append(c.r()).append(",\"g\":").append(c.g())
        .append(",\"b\":").append(c.b()).append('}');
    return sb.toString();
}

String MCPJson::writtenFile(const String& path)
{
    return String("{\"ok\":true,\"path\":\"") + escape(path) + "\"}";
}

String MCPJson::tool(const char* name, const char* description)
{
    return String("{\"name\":\"") + name + "\",\"description\":\""
        + escape(description)
        + "\",\"inputSchema\":{\"type\":\"object\",\"additionalProperties\":true}}";
}

String MCPJson::content(const String& json)
{
    return String("{\"content\":[{\"type\":\"text\",\"text\":\"")
        + escape(json) + "\"}],\"isError\":false}";
}

String MCPJson::result(const String& id, const String& json)
{
    return String("{\"jsonrpc\":\"2.0\",\"id\":") + id + ",\"result\":" +
        json + "}";
}

String MCPJson::error(const String& id, int code,
                            const String& message)
{
    return String("{\"jsonrpc\":\"2.0\",\"id\":") + id
        + ",\"error\":{\"code\":" + String::valueOf(code) +
        ",\"message\":\"" + escape(message) + "\"}}";
}
