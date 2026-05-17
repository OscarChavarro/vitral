#include "ImagePersistence.h"
#include "../PersistenceElement.h"
#include "../../media/RGBImageUncompressed.h"
#include "../../media/RGBAImageUncompressed.h"
#include "../../media/RGBPixel.h"
#include "../../common/logging/Logger.h"
#include "../../common/VSDK.h"
#include "../../java/File.h"
#include "../../java/String.h"
#include "../../java/io/FileInputStream.h"
#include "../../java/io/FileOutputStream.h"
#include "../../java/io/BufferedInputStream.h"
#include "../../java/io/BufferedOutputStream.h"
#include <cstring>
#include <cstdio>
#include <cctype>

bool ImagePersistence::isTextComment(const java::String& line) {
    const char* str = line.c_str();
    if (str == nullptr || strlen(str) == 0) {
        return false;
    }

    int i = 0;
    while (i < (int)strlen(str) &&
           (str[i] != ' ' && str[i] != '\t')) {
        i++;
    }

    return i < (int)strlen(str) && str[i] == '#';
}

java::String* ImagePersistence::extractExtensionFromFile(const java::File& fd) {
    java::String path = fd.getName();
    const char* pathStr = path.c_str();

    const char* lastDot = strrchr(pathStr, '.');
    if (lastDot != nullptr && lastDot != pathStr) {
        java::String* ext = new java::String(lastDot + 1);
        for (int i = 0; i < (int)ext->length(); i++) {
            (*ext)[i] = tolower((*ext)[i]);
        }
        return ext;
    }
    return new java::String("");
}

RGBImageUncompressed* ImagePersistence::importRGB(const java::File& inImageFd) {
    java::String* type = extractExtensionFromFile(inImageFd);
    RGBImageUncompressed* retImage = nullptr;

    if (type->equals("ppm")) {
        try {
            java::FileInputStream fis(inImageFd);
            java::BufferedInputStream bis(fis);

            bool exit = false;
            java::String* line = nullptr;
            int stage = 1;
            int xSize = 0, ySize = 0;
            int i = 0;

            do {
                if (line != nullptr) {
                    delete line;
                }
                line = vsdk::PersistenceElement::readAsciiLine(bis);

                if (line == nullptr) {
                    break;
                }

                if (line->equals("255")) {
                    exit = true;
                }

                if (isTextComment(*line)) {
                    continue;
                }

                switch (stage) {
                    case 1:
                        if (!line->startsWith("P6")) {
                            delete type;
                            delete line;
                            fprintf(stderr, "Error reading internal PPM file subformat\n");
                            return new RGBImageUncompressed();
                        }
                        stage++;
                        break;

                    case 2:
                        if (line->startsWith("#")) {
                        } else {
                            char lineCopy[256];
                            strcpy(lineCopy, line->c_str());

                            char* token = strtok(lineCopy, " \t");
                            if (token != nullptr) {
                                xSize = atoi(token);
                                token = strtok(nullptr, " \t");
                                if (token != nullptr) {
                                    ySize = atoi(token);
                                }
                            }
                            stage++;
                        }
                        break;
                }
            } while (!exit);

            if (line != nullptr) {
                delete line;
            }

            retImage = new RGBImageUncompressed();
            retImage->initNoFill(xSize, ySize);

            char* barr = new char[xSize * 3];
            for (i = 0; i < ySize; i++) {
                unsigned char* ubarr = (unsigned char*)barr;
                vsdk::PersistenceElement::readBytes(bis, ubarr, xSize * 3);
                for (int x = 0; x < xSize; x++) {
                    retImage->putPixel(x, i, barr[x*3], barr[x*3+1], barr[x*3+2]);
                }
            }

            delete[] barr;

            // Invert image (flip vertically)
            RGBPixel pa;
            RGBPixel pb;
            for (int y = 0; y < ySize / 2; y++) {
                for (int x = 0; x < xSize; x++) {
                    RGBPixel* ppa = retImage->getPixelRgb(x, y);
                    RGBPixel* ppb = retImage->getPixelRgb(x, ySize - y - 1);
                    retImage->putPixelRgb(x, y, ppb);
                    retImage->putPixelRgb(x, ySize - y - 1, ppa);
                    delete ppa;
                    delete ppb;
                }
            }

            delete type;
            return retImage;
        } catch (...) {
            fprintf(stderr, "Cannot import PPM image file\n");
            delete type;
            return new RGBImageUncompressed();
        }
    }

    delete type;
    return new RGBImageUncompressed();
}

RGBAImageUncompressed* ImagePersistence::importRGBA(const java::File& inImageFd) {
    RGBAImageUncompressed* retImage = new RGBAImageUncompressed();
    retImage->init(256, 256);
    retImage->createTestPattern();
    return retImage;
}

bool ImagePersistence::exportPPM(const java::File& fd, Image* img) {
    if (img == nullptr) {
        return false;
    }

    try {
        java::FileOutputStream fos(fd);
        java::BufferedOutputStream writer(fos);

        java::String line1("P6\n");
        java::String line2("# Image generated by VitralSDK (http://vitral.sf.net)\n");

        char sizeStr[256];
        snprintf(sizeStr, sizeof(sizeStr), "%d %d\n", img->getXSize(), img->getYSize());
        java::String line3(sizeStr);

        java::String line4("255\n");

        vsdk::PersistenceElement::writeAsciiString(writer, line1.c_str());
        vsdk::PersistenceElement::writeAsciiString(writer, line2.c_str());
        vsdk::PersistenceElement::writeAsciiString(writer, line3.c_str());
        vsdk::PersistenceElement::writeAsciiString(writer, line4.c_str());

        int x;
        int y;
        for (y = 0; y < img->getYSize(); y++) {
            for (x = 0; x < img->getXSize(); x++) {
                RGBPixel* p = img->getPixelRgb(x, y);
                vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->r);
                vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->g);
                vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->b);
                delete p;
            }
        }

        return true;
    } catch (...) {
        return false;
    }
}
