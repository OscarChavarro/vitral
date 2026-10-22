#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

#include "java/util/concurrent/locks/ReentrantLock.h"
#include "java/util/TreeMap.h"

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


static TreeMap<String, String>& properties()
{
    static TreeMap<String, String> table;
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
    const String *found = properties().get(key);
    if ( found == NULL ) {
        return defaultValue;
    }
    return *found;
}

void
System::setProperty(const String& key, const String& value) {
    ReentrantLockGuard guard(propertiesLock());
    properties().put(key, value);
}

void
System::setPropertiesFromArguments(int argc, char* argv[]) {
    for ( int i = 1; i < argc; i++ ) {
        if ( argv[i] == NULL || strncmp(argv[i], "-D", 2) != 0 ) {
            continue;
        }
        String definition(argv[i] + 2);
        int equals = definition.indexOf('=');
        if ( equals < 0 ) {
            setProperty(definition, "true");
        }
        else {
            setProperty(definition.substring(0, equals),
                        definition.substring(equals + 1));
        }
    }
}

