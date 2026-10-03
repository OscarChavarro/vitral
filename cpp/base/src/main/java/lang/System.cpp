#include <chrono>
#include <cstdlib>
#include <cstring>
#include <map>
#include <mutex>
#include <string>

#include "java/lang/System.h"
namespace java {

java::FileOutputStream System::standardOutput("/dev/stdout");
java::FileOutputStream System::standardError("/dev/stderr");
java::PrintStream System::out(&System::standardOutput);
java::PrintStream System::err(&System::standardError);

[[noreturn]] void
System::exit(int status) {
    std::exit(status);
}

long long
System::nanoTime() {
    const auto now = std::chrono::steady_clock::now().time_since_epoch();
    return std::chrono::duration_cast<std::chrono::nanoseconds>(now).count();
}

long long
System::currentTimeMillis() {
    const auto now = std::chrono::system_clock::now().time_since_epoch();
    return std::chrono::duration_cast<std::chrono::milliseconds>(now).count();
}

namespace {

std::map<std::string, std::string>& properties()
{
    static std::map<std::string, std::string> table;
    return table;
}

std::mutex& propertiesMutex()
{
    static std::mutex mutex;
    return mutex;
}

}

java::String
System::getProperty(const java::String& key) {
    return getProperty(key, "");
}

java::String
System::getProperty(const java::String& key, const java::String& defaultValue) {
    std::lock_guard<std::mutex> lock(propertiesMutex());
    std::map<std::string, std::string>::const_iterator found =
        properties().find(key.c_str());
    if ( found == properties().end() ) {
        return defaultValue;
    }
    return java::String(found->second.c_str());
}

void
System::setProperty(const java::String& key, const java::String& value) {
    std::lock_guard<std::mutex> lock(propertiesMutex());
    properties()[key.c_str()] = value.c_str();
}

void
System::setPropertiesFromArguments(int argc, char* argv[]) {
    for ( int i = 1; i < argc; i++ ) {
        if ( argv[i] == nullptr || std::strncmp(argv[i], "-D", 2) != 0 ) {
            continue;
        }
        std::string definition(argv[i] + 2);
        std::string::size_type equals = definition.find('=');
        if ( equals == std::string::npos ) {
            setProperty(definition.c_str(), "true");
        }
        else {
            setProperty(definition.substr(0, equals).c_str(),
                        definition.substr(equals + 1).c_str());
        }
    }
}

}
