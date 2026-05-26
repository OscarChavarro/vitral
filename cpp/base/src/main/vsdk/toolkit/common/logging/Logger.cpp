#include "vsdk/toolkit/common/logging/Logger.h"
#include "java/lang/String.h"

#include <cstdlib>
#include "java/lang/String.h"
#include <iostream>
#include "java/lang/String.h"

#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "java/lang/String.h"

bool Logger::withSystemExit_ = true;
bool Logger::withFatalExceptions_ = true;

void Logger::setWithSystemExit(bool flag) { withSystemExit_ = flag; }
void Logger::setWithFatalExceptions(bool flag) { withFatalExceptions_ = flag; }

void Logger::processFatalError(const java::String& method, const java::String& message, const std::exception* cause)
{
    if ( withSystemExit_ ) {
        std::exit(1);
    }
    if ( !withFatalExceptions_ ) {
        return;
    }
    java::String m = method.empty() ? "VSDK fatal error" : ("VSDK fatal error at " + method);
    if ( !message.empty() ) {
        m += ": " + message;
    }
    if ( cause != nullptr ) {
        m += " | cause: ";
        m += cause->what();
    }
    throw VSDKFatalException(m);
}

void Logger::reportMessage(const java::String& className, int level, const java::String& method, const java::String& message)
{
    std::cerr << "[VSDK][" << className << "] " << method << ": " << message << "\n";
    if ( level == FATAL_ERROR ) {
        processFatalError(method, message, nullptr);
    }
}

void Logger::reportMessageWithException(const java::String& className, int level, const java::String& method, const java::String& message, const std::exception* cause)
{
    std::cerr << "[VSDK][" << className << "] " << method << ": " << message;
    if ( cause != nullptr ) {
        std::cerr << " | exception: " << cause->what();
    }
    std::cerr << "\n";
    if ( level == FATAL_ERROR ) {
        processFatalError(method, message, cause);
    }
}

