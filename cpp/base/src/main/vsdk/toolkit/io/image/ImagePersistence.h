#ifndef __VSDK_TOOLKIT_IO_IMAGE_IMAGEPERSISTENCE_H__
#define __VSDK_TOOLKIT_IO_IMAGE_IMAGEPERSISTENCE_H__

namespace java {
    class File;
    class String;
}

class Image;
class RGBImageUncompressed;
class RGBAImageUncompressed;

/**
This class is a front end through which images of various formats can be
exported and/or imported to/from files.

This class follows a Singleton design pattern.
*/
class ImagePersistence {

public:
    /**
    Given the filename of an input data file which contains an image, this
    method tries to recognize the file format and load the contents of it
    to the image.

    @param inImageFd - The file containing the image
    @return An RGBImageUncompressed entity that contains the image loaded in memory.
    */
    static RGBImageUncompressed* importRGB(const java::File& inImageFd);

    /**
    Given the filename of an input data file which contains an image, this
    method tries to recognize the file format and load the contents of it
    to the image.

    @param inImageFd - The file containing the image
    @return An RGBAImageUncompressed entity that contains the image loaded in memory.
    */
    static RGBAImageUncompressed* importRGBA(const java::File& inImageFd);

    /**
    This method writes the contents of the specified image to a file in
    binary RGB PPM format (i.e. P6 PPM sub-format). Returns true if everything
    works fine, false if something fails.
    @param fd
    @param img
    @return true if export was successful, false otherwise
    */
    static bool exportPPM(const java::File& fd, Image* img);

private:
    static bool isTextComment(const java::String& line);
    static java::String* extractExtensionFromFile(const java::File& fd);
};

#endif // __VSDK_TOOLKIT_IO_IMAGE_IMAGEPERSISTENCE_H__
