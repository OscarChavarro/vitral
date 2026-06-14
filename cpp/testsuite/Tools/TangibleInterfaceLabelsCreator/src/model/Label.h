#ifndef __LABEL_H__
#define __LABEL_H__

#include <java/lang/String.h>

class Label {
  public:
    Label();
    explicit Label(const java::String& title);
    ~Label();

    const java::String& getTitle() const;
    void setTitle(const java::String& title);

  private:
    java::String title_;
};

#endif
