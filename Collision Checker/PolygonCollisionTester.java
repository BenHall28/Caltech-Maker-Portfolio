import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class PolygonCollisionTester extends JPanel{

   //JPanels for use of graphical organization of the window
   private JPanel sidebar, main, shape1Display, shape2Display;
   
   //JLabel for displaying whether or not the shapes intersect
   private JLabel intersectionState, testTime;
   
   //Polygons used to display on screen and alter the user-created shapes
   private Polygon shape1, shape2, sel, buildShape;
   
   //The last position of the mouse, used to update polygon positions
   private int lastX, lastY;
   
   public PolygonCollisionTester(){
      //Instantiation of graphics objects
      super(new BorderLayout());
      shape1 = new Polygon();
      shape2 = new Polygon();
      sidebar = new JPanel(
         //Custom layout manager that places objects aligned along a vertical axis top to bottom with respect for their preferred sizes
         new LayoutManager(){
            public void addLayoutComponent(String name, Component comp){}
            public void removeLayoutComponent(Component comp){}
            public void layoutContainer(Container parent){
               int currY = 0;
               for(Component c : parent.getComponents()){
                  c.setBounds(100-c.getPreferredSize().width/2, currY, c.getPreferredSize().width, c.getPreferredSize().height);
                  currY+=c.getPreferredSize().height+10;
               }
            }
            public Dimension minimumLayoutSize(Container parent){
               int currY = 0;
               for(Component c : parent.getComponents()){
                  currY+=c.getPreferredSize().height+10;
               }
               return new Dimension(200, currY);
            }
            public Dimension preferredLayoutSize(Container parent){
               int currY = 0;
               for(Component c : parent.getComponents()){
                  currY+=c.getPreferredSize().height+10;
               }
               return new Dimension(200, currY);
            }
         });
      JPanel shape1Info = new JPanel(new BorderLayout()), shape2Info = new JPanel(new BorderLayout());
      shape1Display = new JPanel();
      shape1Display.setPreferredSize(new Dimension(200, 200));
      shape1Display.setBackground(Color.BLUE);
      shape1Info.add(shape1Display, BorderLayout.CENTER);
      shape1Info.add(new JLabel("Shape 1"), BorderLayout.NORTH);
      sidebar.add(shape1Info);
      shape2Display = new JPanel();
      shape2Display.setPreferredSize(new Dimension(200, 200));
      shape2Display.setBackground(Color.RED);
      shape2Info.add(shape2Display, BorderLayout.CENTER);
      shape2Info.add(new JLabel("Shape 2"), BorderLayout.NORTH);
      sidebar.add(shape2Info);
      intersectionState = new JLabel("No Intersection Detected");
      sidebar.add(intersectionState);
      testTime = new JLabel("Draw Both shapes to calculate");
      sidebar.add(testTime);
      
      //Add borders to the shape infos on the sidebar when clicked, and set the buildShape to the cooresponding polygon
      shape1Display.addMouseListener(
         new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
               if(buildShape != null){
                  buildShape = null;
                  shape1Display.setBorder(null);
                  shape2Display.setBorder(null);
               }
               shape1Display.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
               buildShape = shape1;
            }
         });
      shape2Display.addMouseListener(
         new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
               if(buildShape != null){
                  buildShape = null;
                  shape1Display.setBorder(null);
                  shape2Display.setBorder(null);
               }
               shape2Display.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
               buildShape = shape2;
            }
         });
         
      /* Custom repaint so that the window will paint the two polygons, and update 
         the intersection state whenever the window is updated. This is where my 
         custom polygon collision algorithm is used. */
      main = 
         new JPanel(){
            @Override
            public void paintComponent(Graphics g){
               if(shape1.npoints > 1 && shape2.npoints > 1){
                  long time = System.nanoTime();
                  if(PolygonCollision.twoPolygonCollision(shape1, shape2)){
                     time = System.nanoTime()-time;
                     testTime.setText("(Calculated in "+time+ " nanoseconds)");
                     intersectionState.setText("Intersection Detected");
                  }
                  else{
                     time = System.nanoTime()-time;
                     testTime.setText("(Calculated in "+time+ " nanoseconds)");
                     intersectionState.setText("No Intersection Detected");
                  }
               }
               super.paintComponent(g);
               g.setColor(Color.BLUE);
               g.fillPolygon(shape1);
               g.setColor(Color.RED);
               g.fillPolygon(shape2);
            }
         };
         
      //Mouse listeners to handle moving and altering of the polygons
      main.addMouseListener(
         new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){
               if(e.getButton() == MouseEvent.BUTTON3){
                  return;
               }
               if(buildShape != null){
                  buildShape = null;
                  shape1Display.setBorder(null);
                  shape2Display.setBorder(null);
               }
               if(shape2.contains(e.getX(), e.getY())){
                  sel = shape2;
               }
               else if(shape1.contains(e.getX(), e.getY())){
                  sel = shape1;
               }
               lastX = e.getX();
               lastY = e.getY();
            }
            @Override
            public void mouseReleased(MouseEvent e){
               sel = null;
            }
            @Override
            public void mouseClicked(MouseEvent e){
               if(e.getButton() == MouseEvent.BUTTON3 && buildShape != null){
                  buildShape.addPoint(e.getX(), e.getY());
                  main.repaint();
               }
            }
         });
      main.addMouseMotionListener(
         new MouseMotionAdapter(){
            @Override
            public void mouseDragged(MouseEvent e){
               if(sel != null){
                  sel.translate(e.getX()-lastX, e.getY()-lastY);
                  main.repaint();
               }
               lastX = e.getX();
               lastY = e.getY();
            }
         });
      
      add(main, BorderLayout.CENTER);
      add(sidebar, BorderLayout.EAST);
   }

   public static void main(String[] args){
      JFrame window = new JFrame("Polygon Collision");
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setContentPane(new PolygonCollisionTester());
      window.setMinimumSize(new Dimension(800, 550));
      window.setVisible(true);
   }
}