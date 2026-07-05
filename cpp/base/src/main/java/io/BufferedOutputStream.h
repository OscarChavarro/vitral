#ifndef __BUFFEREDOUTPUTSTREAM__
#define __BUFFEREDOUTPUTSTREAM__

#include "java/io/File.h"
#include "java/io/OutputStream.h"
namespace java {

class BufferedOutputStream : public OutputStream {
  protected:
    OutputStream *outputStream;

  public:
    explicit BufferedOutputStream(OutputStream *outputStream = nullptr);
    ~BufferedOutputStream() override;

    void
    write(int value) override;

    void
    write(const unsigned char *buffer, int offset, int length) override;

    void
    flush() override;

    void
    close() override;

    void
    dispose() override;
};

}

#endif
