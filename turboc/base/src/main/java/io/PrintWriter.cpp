#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/PrintWriter.h"
#include "java/lang/Exception.h"


PrintWriter::PrintWriter(OutputStream *out, BOOLEAN autoFlush) :
    out(out), autoFlush(autoFlush), error(FALSE)
{
}

void
PrintWriter::print(const String &text)
{
    try {
        out->write(((const unsigned char *)(text.c_str())), 0,
                   text.length());
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const Exception & ) {
        error = TRUE;
    }
#endif
}

void
PrintWriter::println(const String &text)
{
    print(text);
    print("\n");
    if ( autoFlush ) {
        flush();
    }
}

void
PrintWriter::flush()
{
    try {
        out->flush();
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const Exception & ) {
        error = TRUE;
    }
#endif
}

BOOLEAN
PrintWriter::checkError()
{
    flush();
    return error;
}

