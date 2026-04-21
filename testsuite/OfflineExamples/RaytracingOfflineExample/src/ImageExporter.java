import java.io.File;
import java.util.Locale;

import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImage;

final class ImageExporter {
    boolean export(String outputFileName, RGBImage image)
    {
        File outputFile = new File(outputFileName);

        System.out.print("Exporting result image to file \"" + outputFileName + "\": ");
        boolean exported = export(outputFile, image);
        if ( exported ) {
            System.out.println(" OK!");
        }
        return exported;
    }

    private boolean export(File outputFile, RGBImage image)
    {
        String lowerName = outputFile.getName().toLowerCase(Locale.ROOT);
        if ( lowerName.endsWith(".png") ) {
            ImagePersistence.exportPNG(outputFile, image);
            return true;
        }
        if ( lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ) {
            return ImagePersistence.exportJPG(outputFile, image);
        }
        return ImagePersistence.exportPPM(outputFile, image);
    }
}
