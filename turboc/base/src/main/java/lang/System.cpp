#include <stdlib.h>
#include <string.h>
#include <map>
#include <string>
#include <sys/time.h>

#include "java/util/concurrent/locks/ReentrantLock.h"

#include "java/lang/System.h"

FileOutputStream System::standardOutput("/dev/stdout");
FileOutputStream System::standardError("/dev/stderr");
PrintStream System::out(&System::standardOutput);
PrintStream System::err(&System::standardError);

void
System::exit(int status) {
    ::exit(status);
}

long long
System::nanoTime() {
    // C++98 has no monotonic clock: microsecond resolution wall clock
    struct timeval now;
    gettimeofday(&now, NULL);
    return (long long)now.tv_sec * 1000000000LL + (long long)now.tv_usec * 1000LL;
}

long long
System::currentTimeMillis() {
    struct timeval now;
    gettimeofday(&now, NULL);
    return (long long)now.tv_sec * 1000LL + (long long)now.tv_usec / 1000LL;
}


static std::map<std::string, std::string>& properties()
{
    static std::map<std::string, std::string> table;
    return table;
}

static ReentrantLock& propertiesLock()
{
    static ReentrantLock lock;
    return lock;
}


String
System::getProperty(const String& key) {
    return getProperty(key, "");
}

String
System::getProperty(const String& key, const String& defaultValue) {
    ReentrantLockGuard guard(propertiesLock());
    std::map<std::string, std::string>::const_iterator found =
        properties().find(key.c_str());
    if ( found == properties().end() ) {
        return defaultValue;
    }
    return String(found->second.c_str());
}

void
System::setProperty(const String& key, const String& value) {
    ReentrantLockGuard guard(propertiesLock());
    properties()[key.c_str()] = value.c_str();
}

void
System::setPropertiesFromArguments(int argc, char* argv[]) {
    for ( int i = 1; i < argc; i++ ) {
        if ( argv[i] == NULL || strncmp(argv[i], "-D", 2) != 0 ) {
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

