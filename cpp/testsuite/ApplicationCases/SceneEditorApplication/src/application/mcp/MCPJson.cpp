#include "java/lang/Double.h"
#include "java/lang/StringBuilder.h"
#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "application/mcp/MCPJson.h"

using java::util::regex::Matcher;
using java::util::regex::Pattern;

//= Request properties ====================================================

bool MCPJson::hasProperty(const java::String& json, const java::String& key)
{
    return Pattern::compile(java::String("\"") + Pattern::quote(key) +
        "\"\\s*:").matcher(json).find();
}

java::String MCPJson::stringProperty(const java::String& json,
                                     const java::String& key,
                                     const java::String& defaultValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return matcher.group(1);
}

java::String MCPJson::idProperty(const java::String& json)
{
    Pattern pattern = Pattern::compile(
        "\"id\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[-0-9]+|null)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return "null";
    }
    return matcher.group(1);
}

double MCPJson::numberProperty(const java::String& json,
                               const java::String& key, double defaultValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return java::Double::parseDouble(matcher.group(1));
}

bool MCPJson::booleanProperty(const java::String& json,
                              const java::String& key, bool& outValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(true|false)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return false;
    }
    outValue = matcher.group(1).equals("true");
    return true;
}

//= Answers ===============================================================

java::String MCPJson::escape(const java::String& in)
{
    return in.replace("\\", "\\\\").replace("\"", "\\\"");
}

java::String MCPJson::vector(const Vector3Dd& v)
{
    java::StringBuilder sb;
    sb.append("{\"x\":").append(v.x()).append(",\"y\":").append(v.y())
        .append(",\"z\":").append(v.z()).append('}');
    return sb.toString();
}

java::String MCPJson::color(const ColorRgb& c)
{
    java::StringBuilder sb;
    sb.append("{\"r\":").append(c.r()).append(",\"g\":").append(c.g())
        .append(",\"b\":").append(c.b()).append('}');
    return sb.toString();
}

java::String MCPJson::writtenFile(const java::String& path)
{
    return java::String("{\"ok\":true,\"path\":\"") + escape(path) + "\"}";
}

java::String MCPJson::tool(const char* name, const char* description)
{
    return java::String("{\"name\":\"") + name + "\",\"description\":\""
        + escape(description)
        + "\",\"inputSchema\":{\"type\":\"object\",\"additionalProperties\":true}}";
}

java::String MCPJson::content(const java::String& json)
{
    return java::String("{\"content\":[{\"type\":\"text\",\"text\":\"")
        + escape(json) + "\"}],\"isError\":false}";
}

java::String MCPJson::result(const java::String& id, const java::String& json)
{
    return java::String("{\"jsonrpc\":\"2.0\",\"id\":") + id + ",\"result\":" +
        json + "}";
}

java::String MCPJson::error(const java::String& id, int code,
                            const java::String& message)
{
    return java::String("{\"jsonrpc\":\"2.0\",\"id\":") + id
        + ",\"error\":{\"code\":" + java::String::valueOf(code) +
        ",\"message\":\"" + escape(message) + "\"}}";
}
