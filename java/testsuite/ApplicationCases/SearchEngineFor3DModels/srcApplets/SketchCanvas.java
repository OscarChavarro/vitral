// Java basic classes
import java.util.ArrayList;

// Java AWT / Swing classes
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class SketchCanvas extends Canvas
 implements MouseListener, MouseMotionListener
{
    private int id;
    private Sketch parent;
    private History history = new History();
    private static final int NR_HELP_STRINGS = 6;
    private String[] helpStrings = new String[6];
    public static final int INACTIVE = 0;
    public static final int ACTIVE = 1;
    public static final int SENDING = 2;
    public static final int RECEIVING = 3;
    public static final int DISABLED = 4;
    private int status = 1;
    private Color backgroundColour = new Color(224, 224, 192);
    private Color drawColour = new Color(0, 0, 0);
    private Color outlineColour = new Color(125, 125, 125);
    private Color textColour = new Color(205, 133, 63);
    private Color grayColour = new Color(100, 100, 128);
    private Color helpColour = new Color(0, 0, 0);
    private Font textFont = new Font("Helvetica", 0, 14);
    private Font helpFont = new Font("Helvetica", 0, 10);
    private Image offscreen;
    private Image singleWidthImage;
    private int imageWidth;
    private int imageHeight;
    private Graphics offscreenGraphics;
    private Graphics singleGraphics;
    private Graphics panelGraphics;
    private ArrayList<Integer> pointsX;
    private ArrayList<Integer> pointsY;
    private ArrayList<Integer> startIndices;
    private ArrayList<Integer> operation;
    private ArrayList<Integer> prevPointsX;
    private ArrayList<Integer> prevPointsY;
    private ArrayList<Integer> prevStartIndices;
    private ArrayList<Integer> prevOperation;
    private static final int DRAW = 0;
    private static final int ERASE = 1;
    private static final int OVAL_SIZE = 20;

    public SketchCanvas(int id, Sketch sketch) {
        this.setBackground(Color.black);
        this.id = id;
        parent = sketch;
        pointsX = new ArrayList<Integer>();
        pointsY = new ArrayList<Integer>();
        operation = new ArrayList<Integer>();
        startIndices = new ArrayList<Integer>();
        prevPointsX = new ArrayList<Integer>();
        prevPointsY = new ArrayList<Integer>();
        prevOperation = new ArrayList<Integer>();
        prevStartIndices = new ArrayList<Integer>();
        int i;
        for ( i = 0; i < 6; i++ ) {
            helpStrings[i] = null;
        }
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    private void clearArray(ArrayList<Integer> arr)
    {
        while ( arr.size() > 0 ) {
            arr.remove(0);
        }
    }
    
    public void clear(Graphics graphics) {
        graphics.setColor(backgroundColour);
        graphics.fillRect(0, 0, imageWidth, imageHeight);
    }
    
    public void clear_points() {
        history.add_command("cl", parent.getTimer());
        int i = pointsX.size();
        if (i > 1) {
            clearArray(prevPointsX);
            clearArray(prevPointsY);
            clearArray(prevStartIndices);
            clearArray(prevOperation);
            for (int i_1_ = 0; i_1_ < i; i_1_++) {
                prevPointsX.add(pointsX.get(i_1_));
                prevPointsY.add(pointsY.get(i_1_));
            }
            int i_2_ = startIndices.size();
            for (int i_3_ = 0; i_3_ < i_2_; i_3_++) {
                prevStartIndices.add(startIndices.get(i_3_));
                prevOperation.add(operation.get(i_3_));
            }
        }
        clearArray(pointsX);
        clearArray(pointsY);
        clearArray(startIndices);
        clearArray(operation);
        paint();
    }
    
    private void draw_outline(Graphics graphics) {
        graphics.setColor(outlineColour);
        for (int i = 1; i <= 2; i++) {
            int i_4_ = i - 1;
            graphics.drawLine(i_4_, i_4_, imageWidth - i, i_4_);
            graphics.drawLine(imageWidth - i, i_4_, imageWidth - i,
                              imageHeight - i);
            graphics.drawLine(imageWidth - i, imageHeight - i, i_4_,
                              imageHeight - i);
            graphics.drawLine(i_4_, imageHeight - i, i_4_, i_4_);
        }
    }
    
    private void draw_sketch(Graphics graphics, boolean bool) {
        int i = startIndices.size();
        if ( i != 0 ) {
            int i_5_ = pointsX.size();
            graphics.setColor(drawColour);
            for (int i_6_ = 0; i_6_ < i; i_6_++) {
                int i_7_ = ((Integer) operation.get(i_6_)).intValue();
                if (i_7_ == 0)
                    graphics.setColor(drawColour);
                else
                    graphics.setColor(backgroundColour);
                int i_8_
                    = ((Integer) startIndices.get(i_6_)).intValue();
                int i_9_ = i_5_;
                if (i_6_ < i - 1)
                    i_9_ = ((Integer) startIndices.get(i_6_ + 1))
                               .intValue();
                int i_10_ = ((Integer) pointsX.get(i_8_)).intValue();
                int i_11_ = ((Integer) pointsY.get(i_8_)).intValue();
                for (int i_12_ = i_8_ + 1; i_12_ < i_9_; i_12_++) {
                    int i_13_
                        = ((Integer) pointsX.get(i_12_)).intValue();
                    int i_14_
                        = ((Integer) pointsY.get(i_12_)).intValue();
                    if (i_7_ == 0) {
                        graphics.drawLine(i_10_, i_11_, i_13_, i_14_);
                        if (bool) {
                            int i_15_ = i_13_ - i_10_;
                            int i_16_ = i_14_ - i_11_;
                            double d;
                            if (i_15_ != 0) {
                                double d_17_ = (double) (i_16_ / i_15_);
                                d = (180.0 * Math.atan(d_17_)
                                     / 3.141592653589793);
                            } else
                                d = 90.0;
                            if (d > 45.0) {
                                graphics.drawLine(i_10_ - 1, i_11_, i_13_ - 1,
                                                  i_14_);
                                graphics.drawLine(i_10_ + 1, i_11_, i_13_ + 1,
                                                  i_14_);
                            } else {
                                graphics.drawLine(i_10_, i_11_ - 1, i_13_,
                                                  i_14_ - 1);
                                graphics.drawLine(i_10_, i_11_ + 1, i_13_,
                                                  i_14_ + 1);
                            }
                        }
                    } else
                        graphics.fillOval(i_13_ - 10, i_14_ - 10, 20, 20);
                    i_10_ = i_13_;
                    i_11_ = i_14_;
                }
            }
        }
    }
    
    public String getHistory_text() {
        return history.getText();
    }
    
    public int getId() {
        return id;
    }
    
    public Image getImage() {
        return singleWidthImage;
    }
    
    public int getStatus() {
        return status;
    }
    
    public boolean is_empty() {
        int i = pointsX.size();
        return i == 0;
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(150, 150);
    }
    
    public void paint() {
        Dimension dimension = this.getSize();
        if ( offscreen == null || dimension.width != imageWidth
            || dimension.height != imageHeight ) {
            if (dimension.width < 1 || dimension.height < 1) {
                return;
            }
            System.out.println("SketchCanvas(" + id + ") size: "
                               + dimension.width + ", " + dimension.height);
            offscreen = this.createImage(dimension.width, dimension.height);
            singleWidthImage
                = this.createImage(dimension.width, dimension.height);
            offscreenGraphics = offscreen.getGraphics();
            singleGraphics = singleWidthImage.getGraphics();
            imageWidth = dimension.width;
            imageHeight = dimension.height;
        }
        clear(offscreenGraphics);
        clear(singleGraphics);
        if (status != 4) {
            draw_sketch(offscreenGraphics, true);
            draw_sketch(singleGraphics, false);
        }
        Graphics graphics = this.getGraphics();
        if (status == 2 || status == 3) {
            clear(graphics);
            draw_outline(graphics);
            graphics.setFont(((SketchCanvas) this).textFont);
            if (pointsX.size() > 0) {
                graphics.setColor(textColour);
                graphics.drawString("[Sending image]", 20, imageHeight / 2);
            } else {
                graphics.setColor(grayColour);
                graphics.drawString("[Skipping empty image]", 20,
                                    imageHeight / 2);
            }
        } else {
            if (status == 1)
                draw_outline(offscreenGraphics);
            if (pointsX.size() == 0 || status == 4) {
                offscreenGraphics.setFont(((SketchCanvas) this).helpFont);
                offscreenGraphics.setColor(helpColour);
                for (int i = 0; i < 6; i++) {
                    if (helpStrings[i] != null)
                        offscreenGraphics.drawString(helpStrings[i], 6, 30 + 20 * i);
                }
            }
            graphics.drawImage(offscreen, 0, 0, this);
        }
    }
    
    public void paint(Graphics graphics) {
        paint();
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(150, 150);
    }
    
    public void set_active(boolean bool) {
        if ( bool ) {
            status = 1;
        }
        else {
            status = 0;
        }
    }
    
    public void set_help_string(int i, String string) {
        helpStrings[i] = string;
    }
    
    public void setStatus(int i) {
        status = i;
        paint();
    }
    
    public void undo_segment() {
        history.add_command("ud", parent.getTimer());
        int i = startIndices.size();
        if ( i == 0 ) {
            int i_23_ = prevPointsX.size();
            if (i_23_ > 1) {
                ArrayList<Integer> vector = pointsX;
                pointsX = prevPointsX;
                prevPointsX = vector;
                vector = pointsY;
                pointsY = prevPointsY;
                prevPointsY = vector;
                vector = startIndices;
                startIndices = prevStartIndices;
                prevStartIndices = vector;
                vector = operation;
                operation = prevOperation;
                prevOperation = vector;
                paint();
            }
          }
          else {
            int i_24_ = ((Integer) startIndices.get(i - 1)).intValue();
            int i_25_ = pointsX.size();
            int i_26_ = i_25_ - i_24_;
            for (int i_27_ = i_25_ - 1; i_27_ >= i_24_; i_27_--) {
                pointsX.remove(i_27_);
                pointsY.remove(i_27_);
            }
            startIndices.remove(i - 1);
            operation.remove(i - 1);
            if (i_26_ == 1) {
                undo_segment();
            }
            paint();
        }
    }

    public void mouseEntered(MouseEvent e) {
        ;
    }

    public void mouseExited(MouseEvent e) {
        ;
    }

    public void mouseClicked(MouseEvent e) {
        ;
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int modifiers = e.getModifiers();

        //int modifiers = 0;
        if ( status == 4 ) {
            return;
        }
        parent.notify_canvas_active(id);
        int newOperationId = 0;
        if ((modifiers & 0x8) != 0) {
            undo_segment();
            return;
        }
        if ((modifiers & 0x4) != 0) {
            newOperationId = 1;
        }
        operation.add(new Integer(newOperationId));
        if (newOperationId == 0) {
            history.add_command("md", parent.getTimer());
        }
        else {
            history.add_command("er", parent.getTimer());
        }
        history.add_coords(x, y);
        int newSize = pointsX.size();
        startIndices.add(new Integer(newSize));
        pointsX.add(new Integer(x));
        pointsY.add(new Integer(y));
        paint();
    }

    public void mouseReleased(MouseEvent e) {
        if ( status == 4 ) {
            return;
        }
        history.add_command("mu", parent.getTimer());
    }

    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if ( status == 4 ) {
            return;
        }
        pointsX.add(new Integer(x));
        pointsY.add(new Integer(y));
        history.add_coords(x, y);
        paint();
    }

    public void mouseMoved(MouseEvent e) {
        ;
    }
}
