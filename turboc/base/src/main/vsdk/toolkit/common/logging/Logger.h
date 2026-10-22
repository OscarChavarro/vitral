#ifndef __LOGGER__
#define __LOGGER__


#include "java/lang/String.h"
#include "java/lang/Throwable.h"
class Logger {
public:
    static const int WARNING = 1;
    static const int ERROR = 2;
    static const int FATAL_ERROR = 3;
    static const int DEBUG = 4;
    static const int VERBOSE = 5;

    static void setWithSystemExit(bool flag);
    static void setWithFatalExceptions(bool flag);

    static void reportMessage(const String& className, int level, const String& method, const String& message);
    static void reportMessageWithException(const String& className, int level, const String& method, const String& message, const Throwable* cause);

private:
    static bool withSystemExit;
    static bool withFatalExceptions;
    static void processFatalError(const String& method, const String& message, const Throwable* cause);
};


#endif
