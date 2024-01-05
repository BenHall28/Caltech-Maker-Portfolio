import netApi.Server;
import netApi.ServerSideConnection;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class TicTacToeServer extends Server<TicTacToeServerPlayer> {
   //The name of the server (used for sending information if the server is public)
   public String name;
   
   //The tic-tac-toe board of this server
   public byte[][] board;
   
   /**
    * Creates a new TicTacToeServer
    */
   public TicTacToeServer(boolean isPublic) throws IOException {
      super(isPublic);
      name = "base";
   }
   
   /**
    * Returns an integer used to verify client connections. 563402 was chosen completely arbitrarily, and was made to match the number given by TicTacToeLocalPlayer.getClientType().
    */
   @Override
   public int getServerType() {
      return 563402;
   }
   
   /**
    * Returns a new serverside representation of a connection in the form of a TicTacToeServerPlayer object based around the supplied socket channel. This method is called by the api when a new client joins the server.
    */
   @Override
   public TicTacToeServerPlayer createClient(SocketChannel socketChannel) {
      try {
         return new TicTacToeServerPlayer(socketChannel, this);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * Returns a byte array representation of information about the server (in this case its name). This is later parsed by TicTacToeLocalPlayer.receiveServerInfo(InetSocketAddress, byte[])
    */
   @Override
   public byte[] getServerInfo(){
      return name.getBytes();
   }
   
   /**
    * Relays the supplied Object to all connected clients
    */
   public void broadcastInfo(Object in){
      System.out.print("");
      getUsers().forEach(
         u -> {
            try {
               u.send(in);
            } catch (Exception e) {
               e.printStackTrace(System.out);
            }
         });
   }
   
   /**
    * Checks if either player has won the game, or if the game has ended in a stalemate.
    */
   private void checkWin() throws IOException, ClassNotFoundException {
      if(board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[2][2] != 0){
         getUsers().get(board[0][0]-1).send(true);
         getUsers().get(board[0][0]%2).send(false);
         close();
         return;
      }
      if(board[2][0] == board[1][1] && board[1][1] == board[0][2] && board[2][0] != 0){
         getUsers().get(board[2][0]-1).send(true);
         getUsers().get(board[2][0]%2).send(false);
         close();
         return;
      }
      for(int r = 0; r < board.length; r++){
         if(board[r][0] == board[r][1] && board[r][1] == board[r][2] && board[r][0] != 0){
            getUsers().get(board[r][0]-1).send(true);
            getUsers().get(board[r][0]%2).send(false);
            close();
            return;
         }
      }
      for(int c = 0; c < board.length; c++){
         if(board[0][c] == board[1][c] && board[1][c] == board[2][c] && board[2][c] != 0){
            getUsers().get(board[0][c]-1).send(true);
            getUsers().get(board[0][c]%2).send(false);
            close();
            return;
         }
      }
      for(int r = 0; r < board.length; r++){
         for(int c = 0; c < board[r].length; c++){
            if(board[r][c] == 0){
               return;}
         }
      }
      getUsers().get(0).send(false);
      getUsers().get(1).send(false);
      close();
   }
   
   /**
    * This is called by the API when a client first begins connecting to the server. If a player has already joined then the server will stop any more clients from initializing connections.
    */
   public void clientConnectPreAuth(TicTacToeServerPlayer serverClient) {
      try {
         if(getUsers().size()+1==2) {
            stopAcceptingNewClients();
         }
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }
   
   /**
    * This is called by the API when a client stops connecting mid-attempt. This will close the server and kick any playes currently connected.
    */
   @Override
   public void clientCancelConnect(TicTacToeServerPlayer client) {
      try {
         close();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * This is called by the API when a client finished connecting to the server. If two players have been reached then the game begins.
    */
   public void clientConnectPostAuth(TicTacToeServerPlayer serverClient) {
      if(getUsers().size()==2) {
         board = new byte[3][3];
         ((TicTacToeServerPlayer)serverClient).setTurn(false);
         broadcastInfo(true);
      }
      else{
         ((TicTacToeServerPlayer)serverClient).setTurn(true);
      }
   }
   
   /**
    * This is called by the API when a client leaves the server. This will close the server and kick any playes currently connected.
    */
   public void clientDisconnect(TicTacToeServerPlayer serverClient, String in) {
      try {
         close();
      } catch (IOException ex) {
         ex.printStackTrace(System.out);
      }
   }
   
   /**
    * This is used to handle any bytes sent from connected clients, turn them into coordinates, and fill in the cooresponding spot on the tic-tac-toe board. It is called from TicTacToeServerPlayer.
    */
   public void processByte(byte b, TicTacToeServerPlayer s){
      if(s.isTurn()){
         int i= getUsers().indexOf(s);
         if(board[b>>(Byte.SIZE/2)][((byte)(b<<(Byte.SIZE/2)))>>(Byte.SIZE/2)] == 0) {
            board[b >> (Byte.SIZE / 2)][((byte) (b << (Byte.SIZE / 2))) >> (Byte.SIZE / 2)] = (byte) (i + 1);
            getUsers().get(i).setTurn(false);
            try {
               getUsers().get((i + 1) % 2).send(b);
               getUsers().get((i + 1) % 2).setTurn(true);
            } catch (Exception ex) {
               ex.printStackTrace(System.out);
            }
            try {
               checkWin();
            } catch (Exception e) {
               e.printStackTrace(System.out);
            }
         }
      }
   }
}
