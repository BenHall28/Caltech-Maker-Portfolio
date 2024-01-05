package netApi;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Class for hosting and managing clients. This essentially acts as a collection of {@link ServerSideConnection}s that automatically updates when one joins or leaves.
 * @param <T> Type of {@link ServerSideConnection} the server is associated with
 */
public abstract class Server<T extends ServerSideConnection> {
    /**
     * Port used to broadcast server (if public)
     */
    private DatagramChannel serverBroadcast;
    /**
     * Socket for opening connections to clients
     */
    private ServerSocketChannel socket;
    private int port;
    /**
     * Lists of all users connected.
     */
    private ArrayList<T> users;
    /**
     * Whether the server is public.
     */
    private boolean isPublic;
    /**
     * Whether the server is open.
     */
    private boolean isOpen;
    /**
     * Creates a new server that is initialized closed
     * @param isPublic Whether the server will start public
     * @throws IOException If an I/O error occurs
     */
    public Server(boolean isPublic) throws IOException {
        this.isPublic = isPublic;
        isOpen = false;
        port = 0;
        users = new ArrayList();
    }
    /**
     * Creates a new server that is initialized closed
     * @param isPublic Whether the server will start public
     * @throws IOException If an I/O error occurs
     * @param port The port that the server will be set to use
     */
    public Server(int port, boolean isPublic) throws IOException {
        this.isPublic = isPublic;
        this.port = port;
        isOpen = false;
        users = new ArrayList();
    }
    /**
     * Sends the server info to the SocketAddress a through udp (called automatically when clients are searching for servers and the server is public)
     * @param a Destination of server info
     */
    public void sendServerInfo(SocketAddress a){
        try{
            if(a instanceof InetSocketAddress){
                byte[] data = getServerInfo();
                serverBroadcast.send(ByteBuffer.wrap("sinfo".getBytes()), a);
                serverBroadcast.send(ByteBuffer.wrap(new byte[]{(byte)(getPort()&255), (byte)(byte)(getPort() >> 8)}), a);
                ByteBuffer temp = ByteBuffer.allocate(Integer.BYTES);
                temp.putInt(0, data.length);
                serverBroadcast.send(temp, a);
                serverBroadcast.send(ByteBuffer.wrap(data), a);
            }
        }catch(Exception ex){}
    }
    /**
     * Abstract method that returns the server's info as an array of bytes (to be processed by {@link ClientSideConnection#receiveServerInfo(InetSocketAddress, byte[])})
     * @return Server's info as an array of bytes
     */
    public abstract byte[] getServerInfo();
    /**
     * Returns the server's ip address as an {@link InetAddress}.
     * @return Server's ip address
     */
    public InetAddress getIP() {
        if(isOpen()) {
            return socket.socket().getInetAddress();
        }
        return null;
    }
    /**
     * Returns the server's port as an {@link Integer}.
     * @return Server's port
     */
    public int getPort(){
        return socket.socket().getLocalPort();
    }
    /**
     * Sets whether the server is public.
     * @param in Whether the server should be set as public or not
     * @throws IOException If an I/O error occurs
     */
    public void setPublic(boolean in) throws IOException {
        if(isPublic && !in){
            serverBroadcast.close();
        }
        else if(in != isPublic && isOpen()){
            serverBroadcast = DatagramChannel.open();
            serverBroadcast.bind(new InetSocketAddress(2562));
            serverBroadcast.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverBroadcast.join(InetAddress.getByName("232.45.103.96"), NetworkUtil.getUsableInterface());
            serverBroadcast.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
            serverBroadcast.configureBlocking(false);
        }
        isPublic = in;
    }
    /**
     * Whether the server is public.
     * @return Whether the server is public
     */
    public boolean isPublic(){
        return isPublic;
    }
    /**
     * Opens the server to incoming connections.
     * @throws IOException If an I/O error occurs
     */
    public void open() throws IOException{
        if(!isOpen()){
            socket = ServerSocketChannel.open();
            socket.configureBlocking(false);
            socket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), port));
            socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            isOpen = true;
            if(isPublic){
                serverBroadcast = DatagramChannel.open();
                serverBroadcast.bind(new InetSocketAddress(2562));
                serverBroadcast.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                serverBroadcast.join(InetAddress.getByName("232.45.103.96"), NetworkUtil.getUsableInterface());
                serverBroadcast.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
                serverBroadcast.configureBlocking(false);
            }
            NetEventHandler.registerServer(this);
        }
    }
    /**
     * Whether the server is open.
     * @return If the server is open
     */
    public boolean isOpen(){
        return isOpen;
    }
    /**
     * Closes the server, clearing all clients and stopping all listeners.
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        if(isOpen()) {
            NetEventHandler.removeServer(this);
            isOpen = false;
            socket.close();
            if (serverBroadcast != null) {
                serverBroadcast.close();
            }
            users.forEach(u -> {
                u.kick("The server is Closing");
            });
            users = new ArrayList();
        }
    }
    /**
     * Method used to authenticate the client when connecting to a server for security and to make sure that the client is connecting to the right type of server. This correlates to {@link ClientSideConnection}'s {@link ClientSideConnection#authenticate(SocketChannel)} method. This method is currently undefined, and you should define it if you want added security to your program.
     * @param client Ip address of client
     * @return Whether the client passed authentication
     */
    public boolean serverSideAuth(SocketChannel client){
        return true;
    }
    /**
     * Adds a client to this server.
     * @param c Client to be added
     * @throws IOException If any I/O errors occur
     */
    public void addServerClient(T c) throws IOException {
        users.add(c);
    }
    /**
     * Removes a {@link ServerSideConnection} from this server.
     * @param c {@link ServerSideConnection} to be removed
     */
    public void removeServerClient(T c) {
        c.kick();
        users.remove(c);
    }
    /**
     * Removes a {@link ServerSideConnection} from this server.
     * @param in Message sent to the {@link ClientSideConnection} upon disconnect
     * @param c {@link ServerSideConnection} to be removed
     */
    public void removeServerClient(T c, String in) {
        c.kick(in);
    }
    /**
     * Returns a list of {@link ServerSideConnection}s currently connected to the server
     * @return A list of {@link ServerSideConnection}s currently connected to the server
     */
    public ArrayList<T> getUsers(){
        return new ArrayList(users);
    }
    /**
     * Stops accepting new clients to join the server, you can restart accepting new clients through either calling {@link #open()}, or {@link #setPublic(boolean)}
     * @throws IOException If an I/O error occurs
     */
    public void stopAcceptingNewClients() throws IOException {//todo
        socket.close();
    }
    /**
     * Starts accepting new clients to join the server if the server is open, you can restart accepting new clients through either calling {@link #open()}, or {@link #setPublic(boolean)}
     * @throws IOException If an I/O error occurs
     */
    public void restartAcceptingNewClients() throws IOException {//todo
        if(!isOpen){throw new ServerClosedException();}
        socket = ServerSocketChannel.open();
        socket.configureBlocking(false);
        socket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), port));
        socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    }

    /**
     * Sets the port that the server should use and closes then reopens the server to apply this change
     * @param p The port that the server should use (0 if it can use any)
     * @throws IOException If an I/O error occurs
     */
    public void setPort(int p) throws IOException {
        port = p;
        stopAcceptingNewClients();
        open();
    }

    /**
     * Returns whether the server is accepting new clients
     * @return Whether the server is accepting new clients
     */
    public boolean acceptingNewClients(){
        return socket.isOpen();
    }
    /**
     * Returns the type associated with the server. This should match the type of the server that the client wants to connect to (found from {@link ClientSideConnection#getClientType()} in the correlating {@link ClientSideConnection} class).
     * @return The type associated with the server
     */
    public abstract int getServerType();
    /**
     * Returns the port used to broadcast server info (when public).
     * @return The port used to broadcast server info (null when not public)
     */
    public DatagramChannel getUdpPort(){
        return serverBroadcast;
    }
    /**
     * Returns the socket used to open connections with clients.
     * @return The socket used to open connections with clients
     */
    public ServerSocketChannel getSocket(){
        return socket;
    }
    /**
     * Returns a new ServerClient of the type associated with this server.
     * @return A new ServerClient of the type associated with this server
     * @param c The {@link SocketChannel} to be associated with the new {@link ServerSideConnection}
     * @throws IOException If an IO error occurs
     */
    public abstract T createClient(SocketChannel c) throws IOException;
    /**
     * Fires when a client has attempted to connect to the server, but has not yet completed authentication
     * @param client {@link ServerSideConnection} attempting to connect to the {@link Server}
     */
    public abstract void clientConnectPreAuth(T client);
    /**
     * Fires when a client has stopped connecting to the server (this can happen for many reasons)
     * @param client {@link ServerSideConnection} that has stopped connecting to the {@link Server}
     */
    public abstract void clientCancelConnect(T client);
    /**
     * Fires when a client has disconnected from the server
     * @param client {@link ServerSideConnection} that has disconnected from the {@link Server}
     */
    public abstract void clientConnectPostAuth(T client);
    /**
     * Fires when a client has disconnected from the server
     * @param client {@link ServerSideConnection} that has disconnected from the {@link Server}
     * @param reason The reason provided (null if none was supplied) for the disconnect
     */
    public abstract void clientDisconnect(T client, String reason);
}
