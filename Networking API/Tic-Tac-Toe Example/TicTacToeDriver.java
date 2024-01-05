public class TicTacToeDriver {
   public static void main(String[] args) {
      try{
         new TicTacToeLocalPlayer();
      }catch(Exception ex){
         System.out.println("Could not start the game");
      }
   }
}