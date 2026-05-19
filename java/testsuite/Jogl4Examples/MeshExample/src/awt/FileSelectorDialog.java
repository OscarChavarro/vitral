package awt;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

public class FileSelectorDialog {
    public File selectGeometryFile() {
        JFileChooser jfc = new JFileChooser(
            (new File("")).getAbsolutePath() + "/../../../../etc/geometry");

        jfc.removeChoosableFileFilter(jfc.getFileFilter());
        jfc.addChoosableFileFilter(new ObjectFilter("obj", "Obj Files"));
        jfc.addChoosableFileFilter(new ObjectFilter("3ds", "3ds Files"));
        jfc.addChoosableFileFilter(new ObjectFilter("ply", "Ply Files"));
        jfc.addChoosableFileFilter(new ObjectFilter("wrl", "VRML Files (exported from Renderpark only)"));
        jfc.addChoosableFileFilter(new ObjectFilter("ase", "3ds Files (Ascii Scene Export)"));
        jfc.addChoosableFileFilter(new ObjectFilter("vtk", "kitware's VTK legacy binary file"));

        int opc = jfc.showOpenDialog(new JPanel());
        if ( opc == JFileChooser.APPROVE_OPTION ) {
            return jfc.getSelectedFile();
        }
        return null;
    }
}
