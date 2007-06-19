
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class writerTestSolution {
    public static void main(String args[])
    {
        File fd = new File("outjavasol.raw");
        try {
            FileOutputStream fos = new FileOutputStream(fd);
            OutputStream os = (OutputStream)fos;
            BufferedOutputStream escritor = new BufferedOutputStream(os);

            for ( long i = 0; i < 1048576L; i++ ) {
                escritor.write(0x01);
                escritor.write(0x02);
                escritor.write(0x03);
            }
            escritor.close();
        }
        catch (Exception e) {
            return;
        }
    }
}
