
import java.io.File;
import java.io.FileOutputStream;

public class writerTest {
    public static void main(String args[])
    {
        File fd = new File("outjava.raw");
        try {
            FileOutputStream escritor = new FileOutputStream(fd);

            for ( long i = 0; i < 1048576L; i++ ) {
                escritor.write(0x01);
                escritor.write(0x02);
                escritor.write(0x03);
            }
        }
        catch (Exception e) {
            return;
        }
    }
}
