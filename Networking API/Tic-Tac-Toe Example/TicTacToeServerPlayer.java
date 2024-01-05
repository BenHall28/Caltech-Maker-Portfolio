import netApi.Server;
import netApi.ServerSideConnection;

import java.io.IOException;
import java.nio.channels.SocketChannel;
   
/**
 * A serverside representation of connected clients
 */
public class TicTacToeServerPlayer extends ServerSideConnection {
   //Whether is is currently this client's turn
   private boolean isTurn;
   
   /**
    * Creates a new TicTacToeServerPlayer connected to the given server and based on the supplied SocketChannel
    */
   public TicTacToeServerPlayer(SocketChannel s, Server serv) throws IOException {
      super(s, serv);
   }
   
   /**
    * Sets whether or not it is this player's turn
    */
   public void setTurn(boolean in){
      isTurn = in;
   }
   
   /**
    * Whether or not it is this player's turn
    */
   public boolean isTurn(){
      return isTurn;
   }
   
   /**
    * Called when the client sends a byt through the connection. This byte is then relayed to the server for processing.
    */
   public void receiveByte(byte b) {
      ((TicTacToeServer)server).processByte(b, this);
   }
   
   //Unused methods. These methods are included to complete the ClientSideConnection abstract class
   public void receiveInt(int i) {}
   public void receiveChar(char c) {}
   public void receiveLong(long l) {}
   public void receiveDouble(double v) {}
   public void receiveShort(short i) {}
   public void receiveFloat(float v) {}
   public void receiveBool(boolean b) {}
   public void receiveString(String s) {}
   public void receiveObject(Object o) {}
}
