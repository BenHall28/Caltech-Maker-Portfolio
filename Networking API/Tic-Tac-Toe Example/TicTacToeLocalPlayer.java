import netApi.ClientSideConnection;
import netApi.NetEventHandler;
import netApi.NetworkUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
   
/**
 * A client-side representation of a tic-tac-toe game. This class handles all graphics needed for the game as well.
 */
public class TicTacToeLocalPlayer extends ClientSideConnection {
   public static void main(String[] args) throws IOException {
      new TicTacToeLocalPlayer();
   }
   //The window containing the game
   JFrame window;
   
   //Whether it is the player's turn, and if they're in a game
   private boolean myTurn, inGame;
   
   //Graphics components for rendering UI and the board
   JPanel mainMenu, hostPublicServer, hostPrivateServer, joinPublicServer, serverList, publicServerOptions, board, game;
   JLabel privateServerCode;
   JTextArea serverName;
   JButton mainMenuButton, closePrivateServerButton, closePublicServerButton;
   
   //The server that the game will be palyed on if the player hosts
   TicTacToeServer server;
   
   //The name of the server
   String serverNameString;
   
   //The tic-tac-toe board as an array
   int[][] boardArr;
   
   /**
    * Creates a new TicTacToeLocalPlayer, initializing all UI
    */
   public TicTacToeLocalPlayer() throws IOException {
      server = new TicTacToeServer(false);
      inGame = false;
      initializePanels();
      window = new JFrame();
      window.add(mainMenu);
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setVisible(true);
   }
   
   /**
    * Sets the screen to display the game board
    */
   private void openGame(){
      boardArr = new int[3][3];
      inGame = true;
      window.getContentPane().removeAll();
      window.getContentPane().add(board, 0);
      window.revalidate();
      window.repaint();
   }
   
   /**
    * Sets the screen to display the main menu
    */
   public void openMenu(){
      inGame = false;
      window.getContentPane().removeAll();
      window.getContentPane().add(mainMenu);
      window.revalidate();
      window.repaint();
   }
   
   /**
    * Returns the client to the main menu when they leave or are kicked from a game
    */
   @Override
   public void leftServer(String s, boolean b) {
      openMenu();
   }
   
   /**
    * Returns an integer used to verify server connections. 563402 was chosen completely arbitrarily, and was made to match the number given by TicTacToeServer.getServerType().
    */
   @Override
   public int getClientType() {
      return 563402;
   }
   
   /**
    * Handles incoming bytes from the server connection. These are parsed into board positions, and mark where the other player has added a piece.
    */
   @Override
   public void receiveByte(byte in) {
      boardArr[in>>Byte.SIZE/2][((byte)(in<<Byte.SIZE/2))>>Byte.SIZE/2] = 2;
      board.repaint();
      myTurn = true;
   }
   
   /**
    * Handles incoming booleans from the server connection. Depending on the client's state these could represent that a game has been started, or ended
    */
   @Override
    public void receiveBool(boolean in) {
      if(!inGame){
         openGame();
      }
      else{
         try {
            finishGame(in);
         } 
         catch (IOException e) {
            e.printStackTrace(System.out);
         }
      }
   }
   
   /**
    * Handles incoming server information. This will add servers to the list of available public servers.
    */
   public void receiveServerInfo(InetSocketAddress inetSocketAddress, byte[] bytes) {
      serverList.add(new ServerInfo(inetSocketAddress, bytes, this));
      serverList.revalidate();
      serverList.repaint();
   }
   
   /**
    * Utility class to store available servers inside buttons that join the servers when clicked.
    */
   private static class ServerInfo extends JButton{
      String name;
      public ServerInfo(InetSocketAddress a, byte[] data, TicTacToeLocalPlayer p){
         super(new String(data));
         addActionListener((e) -> {try {p.joinServer(a.getAddress(), a.getPort()); p.myTurn = false;} catch (Exception ex) {}});
      }
   }
   
   /**
    * Ends the game, giving the player a congratulations or consolation message, then returning to the main menu
    */
   private void finishGame(boolean in) throws IOException {
      System.out.println(in);
      if(in && JOptionPane.showOptionDialog(game, "Yay, you won!", "Hooray", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"YAY"}, 0) >= Integer.MIN_VALUE){leaveServer(); openMenu();}
      else if(JOptionPane.showOptionDialog(game, "Sorry, you lost!", ": (", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{": ("}, 0) >= Integer.MIN_VALUE){leaveServer(); openMenu();}
   }
   
   //Unused methods. These methods are included to complete the ClientSideConnection abstract class
   public void receiveInt(int i) {}
   public void receiveChar(char c) {}
   public void receiveLong(long l) {}
   public void receiveDouble(double v) {}
   public void receiveShort(short i) {}
   public void receiveFloat(float v) {}
   public void receiveString(String in) {}
   public void receiveObject(Object o) {}
   
   /**
    * Initializes all UI
    */
   private void initializePanels() {
      //Creates the main menu and sets up all the buttons
      mainMenu = new JPanel(new BorderLayout());
      JPanel buttons = new JPanel(new GridLayout(2, 2));
      JButton hostPrivateGame = new JButton("Play a New Private Game"), hostPublicGame = new JButton("Play a New Public Game"), joinPrivateGame = new JButton("Join a Private Game"), joinPublicGame = new JButton("Join a Public Game");
      buttons.add(hostPrivateGame);
      buttons.add(hostPublicGame);
      buttons.add(joinPrivateGame);
      buttons.add(joinPublicGame);
      hostPrivateGame.addActionListener(
         (e) -> {
            try {
               server.setPublic(false);
               server.open();
               joinServer(server.getIP(), server.getPort());
               privateServerCode.setText("Join Code: "+NetworkUtil.genCode(server.getIP(), server.getPort()));
               window.getContentPane().removeAll();
               window.getContentPane().add(hostPrivateServer, 0);
               window.revalidate();
               window.repaint();
               myTurn = true;
            } catch (Exception ex) {
               ex.printStackTrace(System.out);}
         });
      hostPublicGame.addActionListener(
         (e) -> {
            try {
               server.setPublic(true);
               server.open();
               joinServer(server.getIP(), server.getPort());
               window.getContentPane().removeAll();
               window.getContentPane().add(hostPublicServer, 0);
               window.revalidate();
               window.repaint();
               myTurn = true;
            } catch (Exception ex) {
               ex.printStackTrace(System.out);}
         });
      joinPrivateGame.addActionListener(
         (e) -> {
            try {
               joinServer(JOptionPane.showInputDialog("Please input a join code")); 
               myTurn = false;
            } 
            catch (Exception ex) {
               ex.printStackTrace(System.out);
            }
         });
      joinPublicGame.addActionListener(
         (e) -> {
            try {
               serverList.removeAll(); 
               serverList.repaint(); 
               serverSearch(); 
               window.getContentPane().removeAll(); 
               window.getContentPane().add(joinPublicServer, 0); 
               window.revalidate(); 
               window.repaint();} 
            catch (IOException ex) {
               ex.printStackTrace(System.out);
            }
         });
      mainMenu.add(new JLabel("Tic-Tac-Toe"));
      mainMenu.add(buttons, BorderLayout.SOUTH);
      
      //Creates the public game join page
      joinPublicServer = new JPanel(new BorderLayout());
      JButton refreshServerList = new JButton("Refresh");
      refreshServerList.addActionListener(
         (e) -> {
            try {serverList.removeAll(); serverList.repaint(); serverSearch();} 
            catch (IOException ex) {
               ex.printStackTrace(System.out);}});
      mainMenuButton = new JButton("Main Menu");
      mainMenuButton.addActionListener(
         (e) -> {
            openMenu();
         });
      JPanel serverSearchInfo = new JPanel(new BorderLayout());
      JPanel serverInfoButtonHolder = new JPanel(new GridLayout(0, 1));
      serverInfoButtonHolder.add(refreshServerList);
      serverInfoButtonHolder.add(mainMenuButton);      
      serverList = new JPanel(
         new LayoutManager(){
            @Override
            public void addLayoutComponent(String name, Component comp) {}
            @Override
            public void removeLayoutComponent(Component comp) {}
            @Override
            public Dimension preferredLayoutSize(Container parent) {
               int height = 0;
               for(Component c : parent.getComponents()){
                  height+=c.getPreferredSize().height;
               }
               return new Dimension(parent.getWidth(), height);
            }
            @Override
            public Dimension minimumLayoutSize(Container parent) {
               return new Dimension(0, 0);
            }
            @Override
            public void layoutContainer(Container parent) {
               int height = 0;
               for(Component c : parent.getComponents()){
                  c.setBounds(0, height, parent.getWidth(), c.getPreferredSize().height);
                  height+=c.getPreferredSize().height;
               }
            }
         });
      JScrollPane serverListScrollPane = new JScrollPane(serverList);
      joinPublicServer.add(serverListScrollPane, BorderLayout.CENTER);
      serverSearchInfo.add(serverInfoButtonHolder, BorderLayout.SOUTH);
      joinPublicServer.add(serverSearchInfo, BorderLayout.EAST);
      
      //Creates the private game host page
      hostPrivateServer = new JPanel(new BorderLayout());
      closePrivateServerButton = new JButton("Close Server");
      closePrivateServerButton.addActionListener(
         (e) -> {
            try {
               server.close();
            }
            catch(IOException ex){
               ex.printStackTrace(System.out);
            } 
            openMenu();
         });
      privateServerCode = new JLabel();
      JLabel serverWait = new JLabel("Waiting for Another Player to Join...");
      serverWait.setFont(new Font(Font.SERIF, Font.BOLD, 30));
      hostPrivateServer.add(serverWait, BorderLayout.CENTER);
      hostPrivateServer.add(privateServerCode, BorderLayout.NORTH);
      hostPrivateServer.add(closePrivateServerButton, BorderLayout.SOUTH);
      
      //Creates the public game host page
      hostPublicServer = new JPanel(new BorderLayout());
      publicServerOptions = new JPanel(
         new LayoutManager(){
            @Override
            public void addLayoutComponent(String name, Component comp) {}
            @Override
            public void removeLayoutComponent(Component comp) {}
            @Override
            public Dimension preferredLayoutSize(Container parent) {
               int height = 0;
               for(Component c : parent.getComponents()){
                  height+=c.getPreferredSize().height;
               }
               return new Dimension(parent.getWidth(), height);
            }
            @Override
            public Dimension minimumLayoutSize(Container parent) {
               return new Dimension(0, 0);
            }
            @Override
            public void layoutContainer(Container parent) {
               int height = 0;
               for(Component c : parent.getComponents()){
                  c.setBounds(0, height, parent.getWidth(), c.getPreferredSize().height);
                  height+=c.getPreferredSize().height;
               }
            }
         });
      serverName = new JTextArea(serverNameString = "GenericPublicServer"+(int)(Math.random()*10000));
      server.name = serverNameString;
      serverName.setLineWrap(false);
      serverName.getDocument().addDocumentListener(
         new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
               server.name = serverNameString;
               if(!serverName.getText().isEmpty()){
                  serverNameString = serverName.getText();
                  server.name = serverNameString;
               }
               else{serverNameString = "Unnamed Server";
                  server.name = serverNameString;
               }
            }
         
            @Override
            public void removeUpdate(DocumentEvent e) {
               if(!serverName.getText().isEmpty()){
                  serverNameString = serverName.getText();
               }
               else{serverNameString = "Unnamed Server";}
            }
         
            @Override
            public void changedUpdate(DocumentEvent e) {
               if(!serverName.getText().isEmpty()){
                  serverNameString = serverName.getText();
               }
               else{serverNameString = "Unnamed Server";}
            }
         });
      publicServerOptions.add(new JLabel("Name:"));
      publicServerOptions.add(serverName);
      serverWait = new JLabel("Waiting for Another Player to Join...");
      serverWait.setFont(new Font(Font.SERIF, Font.BOLD, 30));
      publicServerOptions.add(serverWait);
      hostPublicServer.add(publicServerOptions, BorderLayout.CENTER);
      closePublicServerButton = new JButton("Close Server");
      closePublicServerButton.addActionListener(
         (e) -> {
            try {
               server.close();
               openMenu();
            }
            catch(IOException ex){
               ex.printStackTrace(System.out);
            } 
            openMenu();
         });
      hostPublicServer.add(closePublicServerButton, BorderLayout.SOUTH);
      
      //Initializes the tic-tac-toe board's UI
      board = 
         new JPanel(){
            public void paintComponent(Graphics g){
               Color temp = g.getColor();
               g.setColor(Color.BLACK);
               for(int i = 0; i < 3; i++){
                  for(int j = 0; j < 3; j++){
                     if(boardArr[i][j] == 1) {
                        g.drawOval(j * (this.getWidth() / 5) + (2 * j + 1) * (getWidth() / 15), i * (this.getHeight() / 5) + (2 * i + 1) * (getHeight() / 15), this.getWidth() / 5, this.getHeight() / 5);
                     }
                     else if(boardArr[i][j] == 2){
                        g.drawLine(j * (this.getWidth() / 5) + (2 * j + 1) * (getWidth() / 15), i * (this.getHeight() / 5) + (2 * i + 1) * (getHeight() / 15), j * (this.getWidth() / 5) + (2 * j + 1) * (getWidth() / 15)+this.getWidth() / 5, i * (this.getHeight() / 5) + (2 * i + 1) * (getHeight() / 15)+this.getHeight() / 5);
                        g.drawLine(j * (this.getWidth() / 5) + (2 * j + 1) * (getWidth() / 15)+this.getWidth() / 5, i * (this.getHeight() / 5) + (2 * i + 1) * (getHeight() / 15), j * (this.getWidth() / 5) + (2 * j + 1) * (getWidth() / 15), i * (this.getHeight() / 5) + (2 * i + 1) * (getHeight() / 15)+this.getHeight() / 5);
                     }
                  }
               }
               g.fillRect((this.getWidth() / 5) + (2) * (getWidth() / 15)-getWidth()/30, 0, getWidth()/15, getHeight());
               g.fillRect(2*(this.getWidth() / 5) + (4) * (getWidth() / 15)-getWidth()/30, 0, getWidth()/15, getHeight());
               g.fillRect(0, (this.getHeight() / 5) + (2) * (getHeight() / 15)-getHeight()/30, getWidth(), getHeight()/15);
               g.fillRect(0, 2*(this.getHeight() / 5) + (4) * (getHeight() / 15)-getHeight()/30, getWidth(), getHeight()/15);
               g.setColor(temp);
            }
         };
      board.addMouseListener(
         new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               if (!myTurn) {
                  return;
               }
               int x = -1, y = -1;
               if (e.getX() < (board.getWidth() / 5) + (2) * (board.getWidth() / 15) - board.getWidth() / 30) {
                  x = 0;
               } else if (e.getX() < 2 * (board.getWidth() / 5) + (4) * (board.getWidth() / 15) - board.getWidth() / 30 && e.getX() > (board.getWidth() / 5) + (2) * (board.getWidth() / 15) + board.getWidth() / 30) {
                  x = 1;
               } else if (e.getX() < board.getWidth() && e.getX() > 2 * (board.getWidth() / 5) + (4) * (board.getWidth() / 15) + board.getWidth() / 30) {
                  x = 2;
               }
               if (x == -1) {
                  return;
               }
               if (e.getY() < (board.getHeight() / 5) + (2) * (board.getHeight() / 15) - board.getHeight() / 30) {
                  y = 0;
               } else if (e.getY() < 2 * (board.getHeight() / 5) + (4) * (board.getHeight() / 15) - board.getHeight() / 30 && e.getY() > (board.getHeight() / 5) + (2) * (board.getHeight() / 15) + board.getHeight() / 30) {
                  y = 1;
               } else if (e.getY() < board.getHeight() && e.getY() > 2 * (board.getHeight() / 5) + (4) * (board.getHeight() / 15) + board.getHeight() / 30) {
                  y = 2;
               }
               if (y == -1 || boardArr[y][x] != 0) {
                  return;
               }
               try {
                  send((byte) ((y << Byte.SIZE / 2) | x));
               } catch (Exception ex) {
               }
               myTurn = false;
               boardArr[y][x] = 1;
               board.repaint();
            }
         });
   }
}