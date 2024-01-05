import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.io.File;

public class CelebrityBrain extends JPanel implements ActionListener{
   //button to exit showcase mode
   private BufferedImage back = null;
   //Timer for animation timing
   private Timer ticker;
   //numbers for use in animation (timings and placement)
   private int tFrame = 0, width = 800, height = 500, offH = 0, offW = 0;
   //the currently selected brain part (null if none)
   private BrainPart sel;
   //the window holding the page
   private JFrame holder = new JFrame();
   //stores all the parts of the brain
   private ArrayList<BrainPart> brain = new ArrayList();
   //represents the current state of the screen (home, transition, or showcase)
   private int state = 0;
   //Constants to represent the current state of the display
   public static final int MAIN = 0, TRANSITION_IN = 1, SHOWCASE = 2, TRANSITION_OUT = 3;
   
   public CelebrityBrain(){      
      //instantiates the window                                                                                  
      holder.setMinimumSize(new Dimension(800, 500));
      holder.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      holder.add(this);
      holder.setSize(800, 500);
      setBackground(Color.LIGHT_GRAY);
      holder.setVisible(true);
      
      //instantiates all parts of the brain
      brain.add(new BrainPart("Frontal Lobe", new Color(140, 255, 251), new Color(125, 227, 223), 11, 5, 40, new String[]{"The frontal lobe controls countless things", "in our daily life. It includes the motor cortex,", "which allows for voluntary movements. It also", "helps with logical processing, personality,", "and contains Broca's area, which allows for the", "writing and speaking of languages.", "", "This would've helped Archimedes think through his theorems", "and come up with creative solutions to problems,", "like the archimedes screw."}));
      brain.add(new BrainPart("Parietal Lobe", new Color(189, 44, 238), new Color(116, 41, 142), 45, 11, 31, new String[]{"The parietal lobe is composed mainly of", "association areas, area of the brain that help", "associate incoming information with motor", "functions. The parietal lobe also contains the", "sensory cortex, which assists in the processing", "of incoming sensory information.", "", "This would've allowed Archimedes to see and feel", "the water rising when he got in the bath, to later", "extrapolate to use to measure volume of the crown."}));
      brain.add(new BrainPart("Occipital Lobe", new Color(44, 252, 248), new Color(48, 212, 209), 64, 15, 30, new String[]{"The occipital lobe processes visual sensory", "input. It assists with spacial and depth", "processing, as well as object recognition.", "", "This would've allowed Archimedes to see and", "process the world around him, allowing for his", "stargazing."}));
      brain.add(new BrainPart("Temporal Lobe", new Color(55, 233, 141), new Color(43, 197, 117), 19, 50, 30, new String[]{"The temporal lobe processes auditory", "sensory input, and contains Wernike's area,", "a part of the brain essential to processing", "language.", "", "This would've allowed Archimedes to hear", "and understand Greek."}));
      brain.add(new BrainPart("Hypothalamus", new Color(255, 174, 200), new Color(218, 156, 176), 42, 58, 4, new String[]{"The hypothalamus regulates many parts of", "homeostasis essential to keep the body", "functioning. The hypothalamus regulates hunger,", "body temperature, thirst, and countless other", "aspects of daily life.", "", "This would've allowed Archimedes to stay alive", "by contributing to the moderation of his", "internal body temperature."}));
      brain.add(new BrainPart("Thalamus", new Color(255, 202, 24), new Color(255, 127, 39), 46, 55, 9, new String[]{"The thalamus is the sensory switchboard", "of the brain. It relays sensory data about", "sight, touch, hearing, and taste to the", "cooresponding areas of the brain.", "", "This would've allowed Archimedes to observe", "and experience the world around him."}));
      brain.add(new BrainPart("Cerebellum", new Color(176, 227, 19), new Color(147, 185, 29), 55, 52, 25, new String[]{"The cerebellum, at the rear of the brain,", "helps control coordinated muscle movements.", "It is essential for countless everyday actions.", "", "This would have helped Archimedes control the", "pen while he wrote."}));
      brain.add(new BrainPart("Amygdala", new Color(185, 122, 86), new Color(160, 78, 31), 43, 66, 5, new String[]{"The amygdala is heavily related to", "emotional processing and response. It is heavily", "related to the fight or flight response, as it", "acts as a central processor for stressful or", "threatening situations.", "", "This likely contributed to his supposed last", "quote: \"Do not disturb my circles\" as he was", "likely in a state of distress."}));
      brain.add(new BrainPart("Hippocampus", new Color(244, 81, 168), new Color(218, 74, 151), 48, 63, 9, new String[]{"The hippocampus helps control memory and", "learning. It helps store long-term memories,", "and plays a role in emotional control.", "", "This would've allowed Archimedes to store memories", "and associations needed to form theorems", "and ideas."}));
      brain.add(new BrainPart("Reticular Formation", new Color(63, 72, 204), new Color(50, 57, 159), 49, 74, 6, new String[]{"The reticular formation is the part of the", "brainstem associated with the retaining of", "consciousness, arousal, and countless", "other essential processes.", "", "This would've allowed Archimedes to stay", "awake and alert to work and think."}));
      brain.add(new BrainPart("Medulla Oblongata", new Color(236, 28, 36), new Color(136, 0, 27), 49, 85, 8, new String[]{"The medulla oblongata acts as a communication", "center between the brain and the spinal cord,", "helping regulate countless essential processes", "such as breathing or heart rate.", "", "This would've allowed for Archimedes to breathe", "and perform other essential activities for", "staying alive."}));
      
      //instantiates various variables
      ticker = new Timer(30, this);
      Image temp = null;
      try{temp = ImageIO.read(new File("back.png")).getScaledInstance(100, 30, Image.SCALE_SMOOTH);}
      catch(Exception ex){}
      back = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      back.getGraphics().drawImage(temp, 0, 0, null);
      
      //adds a mouse listener to listen for client interactions with the window
      holder.addMouseListener(
         new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
               if(state == MAIN){
                  int i = 0;
                  try{i = (new Robot()).getPixelColor(e.getXOnScreen(), e.getYOnScreen()).getRGB();}
                  catch(Exception ex){}
                  if(BrainPart.colorStorage.containsKey(i)){
                     sel = BrainPart.colorStorage.get(i);
                     BrainPart.colorStorage.get(i).toggleSel();
                     state = TRANSITION_IN;
                     ticker.start();
                  }
               }
               else if(state == SHOWCASE){
                  Color i = null;
                  try{i = (new Robot()).getPixelColor(e.getXOnScreen(), e.getYOnScreen());}
                  catch(Exception ex){}
                  if(i.equals(new Color(230, 230, 230)) || i.equals(new Color(30, 30, 30))){state = TRANSITION_OUT;}
               }
            }
         });
         
      //adds a listener for window resizes such that when the window resizes the components are as well
      holder.addComponentListener(
         new ComponentAdapter(){
            public void componentResized(ComponentEvent e){
               for(BrainPart part:brain){
                  part.setImage(getThis());
               }
            }
         });
      for(BrainPart part:brain){part.setImage(getThis());}
      repaint();
   }
   
   /**
    * Util method used to access main class when inside nested classes
    */
   public CelebrityBrain getThis(){
      return this;
   }
   
   /**
    * Method called every 30 ms by timer, handles animation timing
    */
   public void actionPerformed(ActionEvent e){
      if(state == TRANSITION_IN){
         if(tFrame < 30){
            //If the page is transitioning to showcase this sets the frame count and shifts and resizes the selected brain element further into place
            tFrame++;
            sel.addX((55.0-sel.getDefaultX())/30);
            sel.addY((5.0-sel.getDefaultY())/30); 
            sel.addSize((30.0-sel.getDSize())/30); 
         }
         else{
            //If the page is transitioning to showcase and animations are completed then this sets the state to showcase and resets the frame count
            state = SHOWCASE;
            tFrame = 0;
         }
      }
      else if(state == SHOWCASE){
         //If the page is transitioning to showcase and animations are completed then this sets the state to showcase and resets the frame count
         sel.updateIcon(this);
      }
      else if(state == TRANSITION_OUT){
         if(tFrame < 30){
            //If the page is transitioning to the home screen this sets the frame count and shifts and resizes the selected brain element further into place
            tFrame++;
            sel.addX(-1*(55.0-sel.getDefaultX())/30);
            sel.addY((-5.0+sel.getDefaultY())/30); 
            sel.addSize((-30.0+sel.getDSize())/30);
         }
         else{
            //If the page is transitioning to the home screen and animations are completed then this sets the state to main, resets the frame count, and unselects the previously selected brain piece
            sel.reset(); 
            sel.toggleSel(); 
            sel = null; 
            state = MAIN; 
            tFrame = 0; 
            ticker.stop();
         }
      }
      //repaints the screen once all animation frames are updated
      repaint();
   }
   
   /**
    * Main painting method, will detect whether the window is on the home screen, in transition, or focused on a specific brain part and call the cooresponding method
    */
   public void paintComponent(Graphics g){
      Graphics2D g1 = (Graphics2D) g;
      g1.setBackground(Color.LIGHT_GRAY);
      if(state == MAIN){paintMain(g1);}
      else if(state == TRANSITION_IN){
         paintTransitionIn(g1);
      }
      else if(state == SHOWCASE){
         paintShowcase(g1);
      }
      else if(state == TRANSITION_OUT){
         paintTransitionOut(g1);
      }
   }
   
   /**
    * Paints the home screen, displaying all brain parts.
    */
   public void paintMain(Graphics2D g){
      g.clearRect(0, 0, this.getWidth(), this.getHeight()); //clears the screen
      try{
         //draws the title
         Image temp = ImageIO.read(new File("title.png")).getScaledInstance(Math.min(this.getWidth(), 13*(this.getHeight()/20-6)), this.getHeight()/20-6, Image.SCALE_SMOOTH);
         g.drawImage(temp, (this.getWidth()-temp.getWidth(null))/2, 5, null);
      }
      catch(Exception ex){}
      brain.forEach(part -> {part.draw(g, 1.0f, this);}); //draws the brain
   }
   
   /**
    * Paints the screen in transition, drawing the selected brain part in transition and fading out all other parts
    */
   public void paintTransitionIn(Graphics2D g){
      g.clearRect(0, 0, this.getWidth(), this.getHeight()); //clears the screen
      //draws the brain, fading out the unselected parts
      brain.forEach(
         part -> {
            if(!part.sel()){
               part.draw(g, Math.max(0.0f, (float)(1.0-(double)tFrame/15)), this);
            }
            else{
               part.setImage(this); 
               part.draw(g, 1.0f, this);
            }
         });
   }
   
   /**
    * Paints the screen showcasing the selected brain part, showing its description and associated animations
    */
   public void paintShowcase(Graphics2D g){
      g.clearRect(0, 0, this.getWidth(), this.getHeight()); //clears the screen
      //draws the selected brain part
      sel.draw(g, 55, 5, this);
      sel.write(g, 7, 80, 20, this);
      sel.drawIcon(g, 70, 60, this);
      g.drawImage(back, 20, 20, null);
   }
   
   /**
    * Paints the screen in transition, drawing the selected brain part in transition and fading in all other parts
    */
   public void paintTransitionOut(Graphics2D g){
      g.clearRect(0, 0, this.getWidth(), this.getHeight()); //clears the screen
      //draws the brain
      brain.forEach(
         part -> {
            if(!part.sel()){
               part.draw(g, Math.min(1.0f, (float)((double)tFrame/15)), this);
            }
            else{
               part.setImage(this);
               part.draw(g, 1.0f, this);
            }
         });
   }
   
   public static void main(String[] args){
      new CelebrityBrain();
   }
}