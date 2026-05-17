#include "ImagePersistence.h"
#include "vsdk/toolkit/io/PersistenceElement.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageCompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/java/io/File.h"
#include "vsdk/toolkit/java/lang/String.h"
#include "vsdk/toolkit/java/io/FileInputStream.h"
#include "vsdk/toolkit/java/io/FileOutputStream.h"
#include "vsdk/toolkit/java/io/BufferedInputStream.h"
#include "vsdk/toolkit/java/io/BufferedOutputStream.h"
#include <cstring>
#include <cstdio>
#include <cctype>
#include <vector>

namespace {

int readIntLE(const unsigned char* data, int offset) {
    return (data[offset] & 0xFF) |
           ((data[offset + 1] & 0xFF) << 8) |
           ((data[offset + 2] & 0xFF) << 16) |
           ((data[offset + 3] & 0xFF) << 24);
}

int ddsFourCCToCompressionFormat(const char fourCC[5]) {
    if (std::strncmp(fourCC, "DXT1", 4) == 0) {
        return RGBAImageCompressed::COMPRESSION_DXT1;
    }
    if (std::strncmp(fourCC, "DXT3", 4) == 0) {
        return RGBAImageCompressed::COMPRESSION_DXT3;
    }
    if (std::strncmp(fourCC, "DXT5", 4) == 0) {
        return RGBAImageCompressed::COMPRESSION_DXT5;
    }
    return RGBAImageCompressed::COMPRESSION_UNKNOWN;
}

RGBAImageCompressed* importDDSCompressed(const java::File& inImageFd) {
    java::String filePath = inImageFd.getPath();
    FILE* f = std::fopen(filePath.toCString(), "rb");
    if (f == nullptr) {
        std::fprintf(stderr, "Error: could not open DDS file \"%s\"\\n", filePath.toCString());
        return nullptr;
    }

    std::fseek(f, 0, SEEK_END);
    long fileSize = std::ftell(f);
    std::fseek(f, 0, SEEK_SET);
    if (fileSize < 128) {
        std::fclose(f);
        std::fprintf(stderr, "Error: DDS file too short\\n");
        return nullptr;
    }

    std::vector<unsigned char> fileData(static_cast<size_t>(fileSize));
    size_t readCount = std::fread(fileData.data(), 1, fileData.size(), f);
    std::fclose(f);
    if (readCount != fileData.size()) {
        std::fprintf(stderr, "Error: could not read complete DDS file\\n");
        return nullptr;
    }

    if (fileData[0] != 'D' || fileData[1] != 'D' ||
        fileData[2] != 'S' || fileData[3] != ' ') {
        std::fprintf(stderr, "Error: DDS signature not recognized\\n");
        return nullptr;
    }

    int headerSize = readIntLE(fileData.data(), 4);
    if (headerSize != 124) {
        std::fprintf(stderr, "Error: DDS header size not recognized\\n");
        return nullptr;
    }

    int height = readIntLE(fileData.data(), 12);
    int width = readIntLE(fileData.data(), 16);
    int pixelFormatFlags = readIntLE(fileData.data(), 80);
    char fourCC[5] = {0, 0, 0, 0, 0};
    std::memcpy(fourCC, fileData.data() + 84, 4);

    if ((pixelFormatFlags & 0x04) == 0) {
        std::fprintf(stderr, "Error: DDS file does not use a FourCC compressed format\\n");
        return nullptr;
    }

    int compressionFormat = ddsFourCCToCompressionFormat(fourCC);
    if (compressionFormat == RGBAImageCompressed::COMPRESSION_UNKNOWN) {
        std::fprintf(stderr, "Error: DDS compressed format not supported: %.4s\\n", fourCC);
        return nullptr;
    }

    int dataOffset = 128;
    int dataSize = static_cast<int>(fileData.size()) - dataOffset;
    if (dataSize <= 0) {
        std::fprintf(stderr, "Error: DDS has no payload data\\n");
        return nullptr;
    }

    RGBAImageCompressed* image = new RGBAImageCompressed();
    if (!image->initCompressed(width, height, compressionFormat,
                               reinterpret_cast<char*>(fileData.data() + dataOffset),
                               dataSize)) {
        delete image;
        std::fprintf(stderr, "Error: could not initialize compressed DDS image\\n");
        return nullptr;
    }

    return image;
}

}

#ifdef VITRAL_WITH_JPEG
#include <jpeglib.h>
#endif

#ifdef VITRAL_WITH_PNG
#include <png.h>
#endif

bool ImagePersistence::isTextComment(const java::String& line) {
    if (line.isEmpty()) {
        return false;
    }

    int i = 0;
    while (i < line.length() &&
           (line.charAt(i) != ' ' && line.charAt(i) != '\t')) {
        i++;
    }

    return i < line.length() && line.charAt(i) == '#';
}

java::String* ImagePersistence::extractExtensionFromFile(const java::File& fd) {
    java::String path = fd.getName();
    const char* pathStr = path.toCString();

    const char* lastDot = strrchr(pathStr, '.');
    if (lastDot != nullptr && lastDot != pathStr) {
        java::String* ext = new java::String(lastDot + 1);
        char extBuffer[256];
        strcpy(extBuffer, ext->toCString());
        for (int i = 0; i < (int)strlen(extBuffer); i++) {
            extBuffer[i] = tolower(extBuffer[i]);
        }
        delete ext;
        java::String* result = new java::String(extBuffer);
        return result;
    }
    return new java::String("");
}

RGBImageUncompressed* ImagePersistence::importRGB(const java::File& inImageFd) {
    java::String* type = extractExtensionFromFile(inImageFd);
    RGBImageUncompressed* retImage = nullptr;

#ifdef VITRAL_WITH_JPEG
    if (type->equals("jpg") || type->equals("jpeg")) {
        java::String nameStr = inImageFd.getPath();
        const char* filename = nameStr.toCString();
        FILE* infile = fopen(filename, "rb");
        if (infile == nullptr) {
            fprintf(stderr, "Cannot open JPEG file: %s\n", filename);
            delete type;
            return new RGBImageUncompressed();
        }

        struct jpeg_decompress_struct cinfo;
        struct jpeg_error_mgr jerr;
        cinfo.err = jpeg_std_error(&jerr);
        jpeg_create_decompress(&cinfo);
        jpeg_stdio_src(&cinfo, infile);
        jpeg_read_header(&cinfo, TRUE);

        cinfo.out_color_space = JCS_RGB;
        jpeg_start_decompress(&cinfo);

        int xSize = (int)cinfo.output_width;
        int ySize = (int)cinfo.output_height;
        int channels = (int)cinfo.output_components;

        retImage = new RGBImageUncompressed();
        if (!retImage->initNoFill(xSize, ySize)) {
            fprintf(stderr, "Failed to allocate image memory for JPEG\n");
            jpeg_destroy_decompress(&cinfo);
            fclose(infile);
            delete retImage;
            delete type;
            return new RGBImageUncompressed();
        }

        unsigned char* rowBuffer = new unsigned char[xSize * channels];
        for (int y = 0; y < ySize; y++) {
            JSAMPROW rowPtr = rowBuffer;
            jpeg_read_scanlines(&cinfo, &rowPtr, 1);
            for (int x = 0; x < xSize; x++) {
                unsigned char r = rowBuffer[x * channels + 0];
                unsigned char g = (channels >= 3) ? rowBuffer[x * channels + 1] : r;
                unsigned char b = (channels >= 3) ? rowBuffer[x * channels + 2] : r;
                retImage->putPixel(x, y, (char)r, (char)g, (char)b);
            }
        }
        delete[] rowBuffer;

        jpeg_finish_decompress(&cinfo);
        jpeg_destroy_decompress(&cinfo);
        fclose(infile);

        delete type;
        return retImage;
    }
#endif

    if (type->equals("ppm")) {
        java::String ppmNameStr = inImageFd.getPath();
        java::FileInputStream fis(ppmNameStr.toCString());
        java::BufferedInputStream bis(&fis);

        bool exit = false;
        char* lineStr = nullptr;
        int stage = 1;
        int xSize = 0, ySize = 0;
        int i = 0;

        do {
            if (lineStr != nullptr) {
                delete[] lineStr;
            }
            lineStr = vsdk::PersistenceElement::readAsciiLine(bis);

            if (lineStr == nullptr) {
                break;
            }

            if (strcmp(lineStr, "255") == 0) {
                exit = true;
            }

            if (isTextComment(java::String(lineStr))) {
                continue;
            }

            switch (stage) {
                case 1:
                    if (strncmp(lineStr, "P6", 2) != 0) {
                        delete type;
                        delete[] lineStr;
                        fprintf(stderr, "Error reading internal PPM file subformat\n");
                        return new RGBImageUncompressed();
                    }
                    stage++;
                    break;

                case 2:
                    if (lineStr[0] == '#') {
                    } else {
                        char lineCopy[256];
                        strcpy(lineCopy, lineStr);

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

        if (lineStr != nullptr) {
            delete[] lineStr;
        }

        if (xSize <= 0 || ySize <= 0) {
            fprintf(stderr, "Invalid image dimensions\n");
            delete type;
            return new RGBImageUncompressed();
        }

        retImage = new RGBImageUncompressed();
        if (!retImage->initNoFill(xSize, ySize)) {
            fprintf(stderr, "Failed to allocate image memory\n");
            delete retImage;
            delete type;
            return new RGBImageUncompressed();
        }

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
        for (int y = 0; y < ySize / 2; y++) {
            for (int x = 0; x < xSize; x++) {
                RGBPixel* ppa = retImage->getPixelRgb(x, y);
                RGBPixel* ppb = retImage->getPixelRgb(x, ySize - y - 1);
                if (ppa != nullptr && ppb != nullptr) {
                    retImage->putPixelRgb(x, y, ppb);
                    retImage->putPixelRgb(x, ySize - y - 1, ppa);
                }
                delete ppa;
                delete ppb;
            }
        }

        delete type;
        return retImage;
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

Image* ImagePersistence::importImage(const java::File& inImageFd) {
    java::String* type = extractExtensionFromFile(inImageFd);
    if (type->equals("dds")) {
        RGBAImageCompressed* ddsImage = importDDSCompressed(inImageFd);
        delete type;
        return ddsImage;
    }

    delete type;
    return importRGB(inImageFd);
}

bool ImagePersistence::exportPPM(const java::File& fd, Image* img) {
    if (img == nullptr) {
        return false;
    }

    java::String ppmExportName = fd.getPath();
    java::FileOutputStream fos(ppmExportName.toCString());
    java::BufferedOutputStream writer(&fos);

    java::String line1("P6\n");
    java::String line2("# Image generated by VitralSDK (http://vitral.sf.net)\n");

    char sizeStr[256];
    snprintf(sizeStr, sizeof(sizeStr), "%d %d\n", img->getXSize(), img->getYSize());
    java::String line3(sizeStr);

    java::String line4("255\n");

    vsdk::PersistenceElement::writeAsciiString(writer, line1.toCString());
    vsdk::PersistenceElement::writeAsciiString(writer, line2.toCString());
    vsdk::PersistenceElement::writeAsciiString(writer, line3.toCString());
    vsdk::PersistenceElement::writeAsciiString(writer, line4.toCString());

    int x;
    int y;
    for (y = 0; y < img->getYSize(); y++) {
        for (x = 0; x < img->getXSize(); x++) {
            RGBPixel* p = img->getPixelRgb(x, y);
            if (p == nullptr) {
                fprintf(stderr, "Failed to get pixel at (%d, %d)\n", x, y);
                return false;
            }
            vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->r);
            vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->g);
            vsdk::PersistenceElement::writeByte(writer, (unsigned char)p->b);
            delete p;
        }
    }

    return true;
}

bool ImagePersistence::exportJPEG(const java::File& fd, Image* img, int quality) {
#ifndef VITRAL_WITH_JPEG
    fprintf(stderr, "ERROR: ImagePersistence: JPEG support not compiled in.\n");
    fprintf(stderr, "       Recompile with -DWITH_JPEG=ON to enable JPEG support.\n");
    return false;
#else
    if (img == nullptr) {
        return false;
    }

    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE* outfile = nullptr;

    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);

    java::String jpegExportName = fd.getPath();
    const char* filename = jpegExportName.toCString();
    if ((outfile = fopen(filename, "wb")) == nullptr) {
        fprintf(stderr, "Cannot open file %s for writing\n", filename);
        return false;
    }

    jpeg_stdio_dest(&cinfo, outfile);

    cinfo.image_width = img->getXSize();
    cinfo.image_height = img->getYSize();
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_RGB;

    jpeg_set_defaults(&cinfo);
    if (quality < 0) quality = 0;
    if (quality > 100) quality = 100;
    jpeg_set_quality(&cinfo, quality, TRUE);

    jpeg_start_compress(&cinfo, TRUE);

    JSAMPROW row_pointer;
    unsigned char* row_buffer = new unsigned char[img->getXSize() * 3];
    if (row_buffer == nullptr) {
        fprintf(stderr, "Failed to allocate row buffer\n");
        fclose(outfile);
        jpeg_destroy_compress(&cinfo);
        return false;
    }

    for (int y = 0; y < img->getYSize(); y++) {
        for (int x = 0; x < img->getXSize(); x++) {
            RGBPixel* p = img->getPixelRgb(x, y);
            if (p == nullptr) {
                fprintf(stderr, "Failed to get pixel at (%d, %d)\n", x, y);
                delete[] row_buffer;
                fclose(outfile);
                jpeg_destroy_compress(&cinfo);
                return false;
            }
            row_buffer[x * 3 + 0] = (unsigned char)p->r;
            row_buffer[x * 3 + 1] = (unsigned char)p->g;
            row_buffer[x * 3 + 2] = (unsigned char)p->b;
            delete p;
        }
        row_pointer = row_buffer;
        jpeg_write_scanlines(&cinfo, &row_pointer, 1);
    }

    delete[] row_buffer;

    jpeg_finish_compress(&cinfo);
    fclose(outfile);
    jpeg_destroy_compress(&cinfo);

    return true;
#endif
}

bool ImagePersistence::exportPNG(const java::File& fd, Image* img) {
#ifndef VITRAL_WITH_PNG
    fprintf(stderr, "ERROR: ImagePersistence: PNG support not compiled in.\n");
    fprintf(stderr, "       Recompile with -DWITH_PNG=ON to enable PNG support.\n");
    return false;
#else
    if (img == nullptr) {
        return false;
    }

    FILE* fp = nullptr;
    png_structp png_ptr = nullptr;
    png_infop info_ptr = nullptr;

    java::String pngExportName = fd.getPath();
    const char* filename = pngExportName.toCString();
    fp = fopen(filename, "wb");
    if (!fp) {
        fprintf(stderr, "Cannot open file %s for writing\n", filename);
        return false;
    }

    png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, nullptr, nullptr, nullptr);
    if (!png_ptr) {
        fclose(fp);
        return false;
    }

    info_ptr = png_create_info_struct(png_ptr);
    if (!info_ptr) {
        png_destroy_write_struct(&png_ptr, nullptr);
        fclose(fp);
        return false;
    }

    png_init_io(png_ptr, fp);

    png_set_IHDR(png_ptr, info_ptr,
                 img->getXSize(), img->getYSize(),
                 8, PNG_COLOR_TYPE_RGB,
                 PNG_INTERLACE_NONE,
                 PNG_COMPRESSION_TYPE_DEFAULT,
                 PNG_FILTER_TYPE_DEFAULT);

    png_write_info(png_ptr, info_ptr);

    png_bytep* row_pointers = new png_bytep[img->getYSize()];
    if (row_pointers == nullptr) {
        fprintf(stderr, "Failed to allocate row pointers\n");
        png_destroy_write_struct(&png_ptr, &info_ptr);
        fclose(fp);
        return false;
    }

    unsigned char* image_data = new unsigned char[img->getXSize() * img->getYSize() * 3];
    if (image_data == nullptr) {
        fprintf(stderr, "Failed to allocate image data\n");
        delete[] row_pointers;
        png_destroy_write_struct(&png_ptr, &info_ptr);
        fclose(fp);
        return false;
    }

    for (int y = 0; y < img->getYSize(); y++) {
        row_pointers[y] = image_data + y * img->getXSize() * 3;
        for (int x = 0; x < img->getXSize(); x++) {
            RGBPixel* p = img->getPixelRgb(x, y);
            if (p == nullptr) {
                fprintf(stderr, "Failed to get pixel at (%d, %d)\n", x, y);
                delete[] image_data;
                delete[] row_pointers;
                png_destroy_write_struct(&png_ptr, &info_ptr);
                fclose(fp);
                return false;
            }
            row_pointers[y][x * 3 + 0] = (unsigned char)p->r;
            row_pointers[y][x * 3 + 1] = (unsigned char)p->g;
            row_pointers[y][x * 3 + 2] = (unsigned char)p->b;
            delete p;
        }
    }

    png_write_image(png_ptr, row_pointers);
    png_write_end(png_ptr, nullptr);

    delete[] image_data;
    delete[] row_pointers;

    png_destroy_write_struct(&png_ptr, &info_ptr);
    fclose(fp);

    return true;
#endif
}
