#ifndef __JAVAX_XML_TRANSFORM_TRANSFORMER__
#define __JAVAX_XML_TRANSFORM_TRANSFORMER__

#include "java/io/File.h"
#include "java/lang/String.h"

class Document;


/**
Minimal emulation of an identity `javax.xml.transform.Transformer` that
serializes a DOM document into an XML file. Supported output properties are
the ones of `OutputKeys`: `doctype-system`, `indent` and `encoding`.
*/
class Transformer {
private:
    String doctypeSystem;
    bool indent;
    String encoding;

public:
    static const char* const DOCTYPE_SYSTEM;
    static const char* const INDENT;
    static const char* const ENCODING;

    Transformer();

    void setOutputProperty(const String& name,
                           const String& value);

    /**
    @param document document to serialize
    @return XML text of the document
    */
    String toXmlText(const Document* document) const;

    /**
    @param document document to serialize
    @param file output file
    @return true on success
    */
    bool transform(const Document* document,
                   const File& file) const;
};


#endif
