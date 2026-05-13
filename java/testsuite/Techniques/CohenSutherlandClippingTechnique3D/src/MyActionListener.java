import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;

public class MyActionListener implements ActionListener {
    private Component parent;

    MyActionListener(Component parent)
        {
            this.parent = parent;
        }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(parent, 
                                      "This program is a test for the Cohen-Sutherland 3D line clipping algorithm " +
                                      "implemented in the Camera class of the VSDK. Move each line edge with the " +
                                      "sliders and observe the line behavior respect the second camera view volume."
                                      );
    }
}
