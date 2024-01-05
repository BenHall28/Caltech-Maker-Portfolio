import java.awt.image.BufferedImage;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import javax.swing.JPanel;
public class BrainPart{
   //list of every brainpart and associated color (for use in detection of which part was clicked)
   public static HashMap<Integer, BrainPart> colorStorage = new HashMap();
   //name of brainpart
   private String name;
   //list of lines of the description text
   private ArrayList<String> text = new ArrayList();
   //stored characteristics (default X pos, default Y pos, default size, and current animation frame)
   private int dX = -1, dY = -1, dSize, animInd = 0;
   //current position and size
   private double x, y, size;
   //whether the part is currently selected
   private boolean sel = false;
   //the shape and icon of the brain part
   private BufferedImage image, icon;
   
   public BrainPart(String name, Color mainC, Color borderC, int x, int y, int size, String[] text1){
      //instantiates various variables
      dX = x;
      dY = y;
      this.x = x;
      this.y = y;
      this.size = size;
      dSize = size;
      this.name = name;
      
      //adds the associated colors to color storage
      colorStorage.put(mainC.getRGB(), this);
      colorStorage.put(borderC.getRGB(), this);
      
      //adds the text to the description
      for(String s:text1){
         text.add(s);
      }
   }
   
   /**
    * Draws the brain part at its current location with a transparency given by trans
    */
   public void draw(Graphics2D g, float trans, JPanel panel){
      Composite comp = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trans));
      int offX = 0, width = panel.getWidth();
      if(panel.getWidth() > 8.03*panel.getHeight()/5){offX = (int)(panel.getWidth()-8.01*panel.getHeight()/5)/2; width-=2*offX;}
      g.drawImage(image, (int)(offX+x*width/100), (int)(y*panel.getHeight()/100), null);
      g.setComposite(comp);
   }
   
   /**
    * Draws the brain part at the given location fully opaque
    */
   public void draw(Graphics2D g, int x, int y, JPanel panel){
      int offX = 0, width = panel.getWidth();
      if(panel.getWidth() > 8.03*panel.getHeight()/5){offX = (int)(panel.getWidth()-8.01*panel.getHeight()/5)/2; width-=2*offX;}
      g.drawImage(image, offX+x*width/100, y*panel.getHeight()/100, null);
   }
   
   /**
    * Writes the description of the brain part out to the screen
    */
   public void write(Graphics g, int x, int y, int size, JPanel panel){
      size = panel.getHeight()/25;
      g.setFont(new Font(Font.SERIF, Font.BOLD, size+5));
      g.drawString(name, x*panel.getWidth()/100, y);
      g.setFont(new Font(Font.SERIF, Font.PLAIN, size));
      y+=size+10;
      for(String t:text){
         g.drawString(t, x*panel.getWidth()/100, y);
         y+=size+5;
      }
   }
   
   /**
    * Main painting method, will detect whether the window is on the home screen, in transition, or focused on a specific brain part and call the cooresponding method
    */
   public void drawIcon(Graphics2D g, int x, int y, JPanel panel){
      g.drawImage(icon, x*panel.getWidth()/100, y*panel.getHeight()/100, null);
   }
   
   /**
    * Main painting method, will detect whether the window is on the home screen, in transition, or focused on a specific brain part and call the cooresponding method
    */
   public void drawIcon(Graphics2D g, int x, int y, float trans, JPanel panel){
      Composite comp = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trans));
      g.drawImage(icon, x*panel.getWidth()/100, y*panel.getHeight()/100, null);
      g.setComposite(comp);
   }
   
   /**
    * Main painting method, will detect whether the window is on the home screen, in transition, or focused on a specific brain part and call the cooresponding method
    */
   int tempY = 0;
   public void setImage(JPanel panel){
      int offX = 0, scale = panel.getWidth();
      if(panel.getWidth() > 8.03*panel.getHeight()/5){offX = (int)(panel.getWidth()-8.01*panel.getHeight()/5)/2; scale-=2*offX;}
      Image temp = null;
      try{temp = ImageIO.read(new File(name+".png")).getScaledInstance((int)(size*scale/100), (int)(size*scale/100), Image.SCALE_SMOOTH);}
      catch(Exception ex){}
      image = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      image.getGraphics().drawImage(temp, 0, 0, null);
   }
   
   /**
    * Main painting method, will detect whether the window is on the home screen, in transition, or focused on a specific brain part and call the cooresponding method
    */
   public void updateIcon(JPanel panel){
      BufferedImage temp = null;
      int scale = panel.getWidth();
      if(panel.getWidth() > 10*panel.getHeight()/5){scale = 8*panel.getHeight()/5;}
      try{
         Image t = ImageIO.read(new File(name+"_icon.png"));
         temp = new BufferedImage(t.getWidth(null), t.getHeight(null), BufferedImage.TYPE_INT_ARGB);
         temp.getGraphics().drawImage(ImageIO.read(new File(name+"_icon.png")), 0, 0, null);
      }catch(Exception ex){}
      animInd++;
      if((animInd+1)*temp.getWidth() > temp.getHeight()){animInd = 0;}
      Image t = temp.getSubimage(0, temp.getWidth()*animInd, temp.getWidth(), temp.getWidth()-1).getScaledInstance(20*scale/100, 20*scale/100, Image.SCALE_SMOOTH);
      icon = new BufferedImage(t.getWidth(null), t.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      icon.getGraphics().drawImage(t, 0, 0, null);
   }
   
   public String getName(){
      return name;
   }
   
   public double getX(){
      return x;
   }
   
   public double getY(){
      return y;
   }
   
   /**
    * Sets the default x location
    */
   public void setdX(int i){
      dX = i;
   }
   
   /**
    * Sets the default y location
    */
   public void setdY(int i){
      dY = i;
   }
   
   public int getDefaultX(){
      return dX;
   }
   
   public int getDefaultY(){
      return dY;
   }
   
   /**
    * Resets the brain part's location and size
    */
   public void reset(){
      x = dX;
      y = dY;
      size = dSize;
   }
   
   public void addX(double inc){
      x+=inc;
   }
   
   public void addY(double inc){
      y+=inc;
   }
   
   /**
    * Toggles whether the brain part is marked as selected
    */
   public void toggleSel(){
      sel = !sel;
   }
   
   public boolean sel(){
      return sel;
   }
   
   public void addSize(double s){
      size += s;
   }
   
   /**
    * Returns the default size of the brain part
    */
   public int getDSize(){
      return dSize;
   }
}