#include "java/io/PrintWriter.h"
#include "java/lang/Exception.h"


PrintWriter::PrintWriter(OutputStream *out, bool autoFlush) :
    out(out), autoFlush(autoFlush), error(false)
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
        error = true;
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
        error = true;
    }
}

bool
PrintWriter::checkError()
{
    flush();
    return error;
}

