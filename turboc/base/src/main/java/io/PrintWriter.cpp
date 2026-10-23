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
    catch ( const Exception & ) {
        error = TRUE;
    }
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
    catch ( const Exception & ) {
        error = TRUE;
    }
}

BOOLEAN
PrintWriter::checkError()
{
    flush();
    return error;
}

