//===========================================================================

// Java basic classes
import java.util.StringTokenizer;

// Java GUI classes
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

// JOGL classes
import javax.media.opengl.GL;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;

public class ModifyPanelForFunctionalExplicitSurface extends ModifyPanel implements ActionListener
{
    private JTextField jtfFunction;
    private JTextField jtfMinX;
    private JTextField jtfMinY;
    private JTextField jtfMinZ;
    private JTextField jtfMaxX;
    private JTextField jtfMaxY;
    private JTextField jtfMaxZ;
    private JTextField jtfNX;
    private JTextField jtfNY;
    private boolean firstTimer;

    public ModifyPanelForFunctionalExplicitSurface(SceneEditorApplication parent)
    {
        super(parent);
        jtfFunction = null;
        jtfMinX = null;
        jtfMinY = null;
        jtfMinZ = null;
        jtfMaxX = null;
        jtfMaxY = null;
        jtfMaxZ = null;
        jtfNX = null;
        jtfNY = null;
        firstTimer = true;
    }

    public void notifyTargetBeginEdit(SimpleBody target, JPanel parentPanel)
    {
        //-----------------------------------------------------------------
        JPanel container1 = new JPanel();
        JPanel container2 = new JPanel();
        JPanel container3;

        JLabel label;

        Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        container1.setBorder(empty);
        container2.setBorder(empty);


        container2.setLayout(new GridLayout(21, 1));

        container1.add(container2);

        parentPanel.add(container1);

        this.target = target;
        removeAll();

        FunctionalExplicitSurface functionalSurface;
        functionalSurface = (FunctionalExplicitSurface)target.getGeometry();

        //-----------------------------------------------------------------
        label = new JLabel("FUNCTIONAL EXPLICIT SURFACE EDITOR", SwingConstants.CENTER);
        container2.add(label);

        //-----------------------------------------------------------------
        label = new JLabel("Predefined configurations:", SwingConstants.LEFT);
        container2.add(label);

        JComboBox jcb = new JComboBox();

        // This is strange, but makes combobox heavyweight and integrable with JOGL!
	jcb.getEditor().getEditorComponent().setBackground(java.awt.Color.WHITE);

        jcb.setLightWeightPopupEnabled(false);
        jcb.addActionListener(this);
        jcb.addItem("<None selected>");
        jcb.addItem("cos((PI*x)/2);-10;-10;-10;10;10;10;100;100");
        jcb.addItem("0.03*((1.5-x/2)*((2.25-y^2)^4)*(sin(PI*x/2))^2);-2;-1.5;-10;2;1.5;3;100;100");
        jcb.addItem("x*y*cos(x*y);-1.5;-1.5;-10;1.5;1.5;3;100;100");
        jcb.addItem("x^2+y^2;-2;-2;-10;2;2;2;100;100");
        jcb.addItem("x*y*((x^2 - y^2)/(x^2+y^2));-1.5;-1.5;-10;1.5;1.5;3;100;100");
        jcb.addItem("(1-sqrt(abs(x*y)));-1.5;-1.5;-10;1.5;1.5;10;100;100");
        jcb.addItem("(x^3*y^2)/2;-1.5;-1.5;-10;1.5;1.5;10;100;100");
        jcb.addItem("2*y^2*sin(2*x);-1.5;-1.5;-10;1.5;1.5;10;100;100");
        jcb.addItem("cos(7.17307*(x+3/2)+(2*y)+(-3))*(exp(2*y+(-3)));-1.5;-1.5;-10;1.5;1.5;3;100;100");
        container2.add(jcb);

        //-----------------------------------------------------------------
        label = new JLabel("z = f(x,y) =", SwingConstants.LEFT);
        container2.add(label);

        jtfFunction = new JTextField(functionalSurface.getFunctionExpression());
        jtfFunction.addActionListener(this);
        container2.add(jtfFunction);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MinX: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMinX = new JTextField("" + functionalSurface.getMinXBound());
        jtfMinX.addActionListener(this);
        container3.add(jtfMinX);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MinY: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMinY = new JTextField("" + functionalSurface.getMinYBound());
        jtfMinY.addActionListener(this);
        container3.add(jtfMinY);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MinZ: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMinZ = new JTextField("" + functionalSurface.getMinZBound());
        jtfMinZ.addActionListener(this);
        container3.add(jtfMinZ);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MaxX: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMaxX = new JTextField("" + functionalSurface.getMaxXBound());
        jtfMaxX.addActionListener(this);
        container3.add(jtfMaxX);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MaxY: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMaxY = new JTextField("" + functionalSurface.getMaxYBound());
        jtfMaxY.addActionListener(this);
        container3.add(jtfMaxY);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("MaxZ: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfMaxZ = new JTextField("" + functionalSurface.getMaxZBound());
        jtfMaxZ.addActionListener(this);
        container3.add(jtfMaxZ);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("Nx: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfNX = new JTextField("" + functionalSurface.getTesselationHintX());
        jtfNX.addActionListener(this);
        container3.add(jtfNX);

        container2.add(container3);

        //-----------------------------------------------------------------
        container3 = new JPanel();
        container3.setLayout(new BoxLayout(container3, BoxLayout.X_AXIS));

        label = new JLabel("Ny: ", SwingConstants.RIGHT);
        container3.add(label);

        jtfNY = new JTextField("" + functionalSurface.getTesselationHintY());
        jtfNY.addActionListener(this);
        container3.add(jtfNY);

        container2.add(container3);

        //-----------------------------------------------------------------

    }

    public void actionPerformed(ActionEvent ev) {
        //-----------------------------------------------------------------
        String label = ev.getActionCommand();
        boolean updateModel = false;
        FunctionalExplicitSurface functionalSurface;
        String newFunction;
        double minx, miny, minz, maxx, maxy, maxz;
        int nx, ny;

        functionalSurface = (FunctionalExplicitSurface)target.getGeometry();
        newFunction = functionalSurface.getFunctionExpression();
        minx = functionalSurface.getMinXBound();
        miny = functionalSurface.getMinYBound();
        minz = functionalSurface.getMinZBound();
        maxx = functionalSurface.getMaxXBound();
        maxy = functionalSurface.getMaxYBound();
        maxz = functionalSurface.getMaxZBound();
        nx = functionalSurface.getTesselationHintX();
        ny = functionalSurface.getTesselationHintY();

        if ( ev.getSource() instanceof JComboBox ) {
            JComboBox origin = (JComboBox)ev.getSource();
            label = (String)origin.getSelectedItem();
            StringTokenizer parser = new StringTokenizer(label, ";");
            if ( firstTimer ) {
                firstTimer = false;
            }
            else {
                updateModel = true;
            }

            int i;
            for ( i = 0; parser.hasMoreElements() && updateModel; i++ ) {
                String cad;
                cad = parser.nextToken();
                switch ( i ) {
                  case 0: // Function
                    newFunction = cad;
                    if ( cad.equals("<None selected>") ) {
                        updateModel = false;
                        break;
                    }
                    if ( jtfFunction != null ) {
                        jtfFunction.setText(cad);
                    }
                    break;
                  case 1: // xmin
                    minx = Double.parseDouble(cad);
                    if ( jtfMinX != null ) {
                        jtfMinX.setText(cad);
                    }
                    break;
                  case 2: // ymin
                    miny = Double.parseDouble(cad);
                    if ( jtfMinY != null ) {
                        jtfMinY.setText(cad);
                    }
                    break;
                  case 3: // zmin
                    minz = Double.parseDouble(cad);
                    if ( jtfMinZ != null ) {
                        jtfMinZ.setText(cad);
                    }
                    break;
                  case 4: // xmax
                    maxx = Double.parseDouble(cad);
                    if ( jtfMaxX != null ) {
                        jtfMaxX.setText(cad);
                    }
                    break;
                  case 5: // ymax
                    maxy = Double.parseDouble(cad);
                    if ( jtfMaxY != null ) {
                        jtfMaxY.setText(cad);
                    }
                    break;
                  case 6: // zmax
                    maxz = Double.parseDouble(cad);
                    if ( jtfMaxZ != null ) {
                        jtfMaxZ.setText(cad);
                    }
                    break;
                  case 7: // nx
                    nx = Integer.parseInt(cad);
                    if ( jtfNX != null ) {
                        jtfNX.setText(cad);
                    }
                    break;
                  case 8: // ny
                    ny = Integer.parseInt(cad);
                    if ( jtfNY != null ) {
                        jtfNY.setText(cad);
                    }
                    break;
                  default: break;
                }
            }
        }
        else if ( ev.getSource() instanceof JTextField ) {
            JTextField jtf = (JTextField)ev.getSource();
            updateModel = true;
            if ( jtf == jtfFunction ) {
                newFunction = label;
            }
            else if ( jtf == jtfMinX ) {
                minx = Double.parseDouble(label);
            }
            else if ( jtf == jtfMinY ) {
                miny = Double.parseDouble(label);
            }
            else if ( jtf == jtfMinZ ) {
                minz = Double.parseDouble(label);
            }
            else if ( jtf == jtfMaxX ) {
                maxx = Double.parseDouble(label);
            }
            else if ( jtf == jtfMaxY ) {
                maxy = Double.parseDouble(label);
            }
            else if ( jtf == jtfMaxZ ) {
                maxz = Double.parseDouble(label);
            }
            else if ( jtf == jtfNX ) {
                nx = Integer.parseInt(label);
            }
            else if ( jtf == jtfNY ) {
                ny = Integer.parseInt(label);
            }
            else {
                updateModel = false;
            }
        }
        //-----------------------------------------------------------------
        if ( updateModel ) {
            functionalSurface = new FunctionalExplicitSurface(newFunction);
            functionalSurface.setBounds(minx, miny, minz, maxx, maxy, maxz);
            functionalSurface.setTesselationHint(nx, ny);
            target.setGeometry(functionalSurface);
            parent.drawingArea.canvas.repaint();
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
