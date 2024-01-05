package netApi;

import java.io.IOException;
    import java.net.*;
    import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

/**
 * The client-side version of a client.
 */
public abstract class ClientSideConnection {
    /**
     * The client's {@link SocketChannel}, which is used to connect to servers.
     */
    private SocketChannel channel;
    /**
     * The client's connection to the {@link Server} if connected.
     */
    private NetEventHandler.DataStream connection;
    /**
     * The client's port used to detect public servers.
     */
    private DatagramChannel udpPort;
    private boolean closed, inServer;
    /**
     * Creates a new LocalClient
     * @throws IOException If an I/O error occurs
     */
    public ClientSideConnection() throws IOException {
        closed = false;
        udpPort = DatagramChannel.open();
        udpPort.bind(new InetSocketAddress(0));
        udpPort.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        udpPort.configureBlocking(false);
        udpPort.socket();
        NetEventHandler.registerServerDataReceiver(this);
    }
    /**
     * Joins the {@link Server} denoted by the address ip.
     * @param ip Ip address of server being joined
     * @throws IOException If an I/O error occurs
     * @throws ServerTypeMismatchException If the server type does not match the client type
     */
    public void joinServer(InetSocketAddress ip) throws IOException, ServerTypeMismatchException {
        if(closed){throw new ClientClosedException();}
        if(inServer()){
            throw new IOException();
        }
        channel = SocketChannel.open();
        if(!channel.connect(ip)){
            throw new IOException();
        }
        ByteBuffer intTemp = ByteBuffer.allocate(Integer.BYTES);
        channel.read(intTemp);
        int type;
        if((type = intTemp.getInt(0)) != getClientType()){
            intTemp.clear();
            intTemp.putInt(0, -1);
            channel.write(intTemp);
            channel.close();
            throw new ServerTypeMismatchException("Server Type ("+type+") does not match client type ("+getClientType()+")");
        }
        intTemp.clear();
        intTemp.putInt(0, 1);
        channel.write(intTemp);
        authenticate(channel);
        channel.configureBlocking(false);
        inServer = true;
        connection = NetEventHandler.registerLocalClient(this, channel);
    }
    /**
     * Method used to authenticate the client when connecting to a {@link Server} for security and to make sure that the client is connecting to the right kind of {@link Server}. This correlates to {@link Server}'s {@link Server#serverSideAuth(SocketChannel)} method. This method is currently undefined, and you should define it if you want added security to your program.
     * @param in  {@link SocketChannel} the client is attempting to connect to
     */
    public void authenticate(SocketChannel in){}
    /**
     * Joins the server denoted by the code
     * @param code Code associated with the {@link Server}'s ip address and port
     * @return Whether the server was successfully joined
     * @throws IOException If an I/O error occurs
     * @throws ServerTypeMismatchException If the server type does not match the client type
     */
    public boolean joinServer(String code) throws IOException, ServerTypeMismatchException {
        try{joinServer(NetworkUtil.processCode(code)); return true;}
        catch (ServerTypeMismatchException s){throw s;}
        catch(Exception e){leaveServer(); return false;}
    }
    /**
     * Joins the server at the denoted ip address and port
     * @param address The {@link Server}'s ip address
     * @param port The {@link Server}'s port
     * @return Whether the server was successfully joined
     * @throws IOException If an I/O error occurs
     * @throws ServerTypeMismatchException If the server type does not match the client type
     */
    public boolean joinServer(InetAddress address, int port) throws IOException, ServerTypeMismatchException {
        try{joinServer(new InetSocketAddress(address, port)); return true;}
        catch (ServerTypeMismatchException s){throw s;}
        catch(Exception e){leaveServer(); return false;}
    }
    /**
     * Sends out a request for public Servers. Public servers that respond will be received through {@link ClientSideConnection#receiveServerInfo(InetSocketAddress, byte[])}
     * @throws IOException If an I/O error occurs
     */
    public void serverSearch() throws IOException {
        if(closed){throw new ClientClosedException();}
        ByteBuffer intOut = ByteBuffer.allocate(Integer.BYTES);
        intOut.putInt(0, getClientType());
        for(int i = 0; i < 1; i++){
            i+=udpPort.send(intOut, new InetSocketAddress(InetAddress.getByName("232.45.103.96"), 2562));
        }
    }

    /**
     * Returns whether the client is currently connected to a server.
     * @return Whether the client is currently connected to a server.
     */
    public boolean inServer(){
        return inServer;
    }
    /**
     * Returns the port used for auto-detecting public servers
     * @return Client's Udp port for searching for public servers
     */
    public DatagramChannel getUdpPort(){
        return udpPort;
    }
    /**
     * Leaves the server the client is connected to (if they're connected at all)
     * @throws IOException If an I/O error occurs
     */
    public void leaveServer() throws IOException {
        if(closed){throw new ClientClosedException();}
        if(!inServer){return;}
        inServer = false;
        connection.sendCloseUpdate(null);
        connection.close();
        channel.close();
        connection = null;
        NetEventHandler.removeLocalClient(this);
        leftServer("The client closed the connection", false);
    }
    /**
     * Leaves the server the client is connected to (if they're connected at all)
     * @param forced Whether the server leaving was forced
     * @param in The reason for the server leaving (null if none is provided)
     * @throws IOException If an I/O error occurs
     */
    public void leaveServer(String in, boolean forced) throws IOException {
        if(closed){throw new ClientClosedException();}
        if(!inServer){return;}
        inServer = false;
        connection.sendCloseUpdate(null);
        connection.close();
        channel.close();
        connection = null;
        NetEventHandler.removeLocalClient(this);
        leftServer(in, forced);
    }
    /**
     * Returns the type associated with the client. This should match the type of the {@link Server} that the client wants to connect to ({@link Server#getServerType()} in the correlating {@link Server} class).
     * @return Type associated with client
     */
    public abstract int getClientType();
    /**
     * Sends the {@link Server} the {@link Object} o, automatically parsing it if it is a primitive or a {@link String}
     * @param o Data to send to the {@link Server}
     * @throws IOException If any I/O errors occur
     */
    public void send(Object o) throws IOException {
        if(closed){throw new ClientClosedException();}
        if(connection.isOpen()) {
            switch (o) {
                case Integer i -> connection.sendInt((int) o);
                case Character c -> connection.sendChar((char) o);
                case Long l -> connection.sendLong((long) o);
                case Double v -> connection.sendDouble((double) o);
                case Byte b -> connection.sendByte((byte) o);
                case Short i -> connection.sendShort((short) o);
                case Float v -> connection.sendFloat((float) o);
                case Boolean b -> connection.sendBool((boolean) o);
                case String s -> connection.sendString(s);
                case null, default -> connection.sendObject(o);
            }
        }
    }
    /**
     * Fires when the client receives data in the form of an {@link Integer}, this is where you should handle incoming data
     * @param in The {@link Integer} received by the client
     */
    public abstract void receiveInt(int in);
    /**
     * Fires when the client receives data in the form of an {@link Character}, this is where you should handle incoming data
     * @param in The {@link Character} received by the client
     */
    public abstract void receiveChar(char in);
    /**
     * Fires when the client receives data in the form of an {@link Long}, this is where you should handle incoming data
     * @param in The {@link Long} received by the client
     */
    public abstract void receiveLong(long in);
    /**
     * Fires when the client receives data in the form of an {@link Double}, this is where you should handle incoming data
     * @param in The {@link Double} received by the client
     */
    public abstract void receiveDouble(double in);
    /**
     * Fires when the client receives data in the form of an {@link Byte}, this is where you should handle incoming data
     * @param in The {@link Byte} received by the client
     */
    public abstract void receiveByte(byte in);
    /**
     * Fires when the client receives data in the form of an {@link Short}, this is where you should handle incoming data
     * @param in The {@link Short} received by the client
     */
    public abstract void receiveShort(short in);
    /**
     * Fires when the client receives data in the form of an {@link Float}, this is where you should handle incoming data
     * @param in The {@link Float} received by the client
     */
    public abstract void receiveFloat(float in);
    /**
     * Fires when the client receives data in the form of an {@link Boolean}, this is where you should handle incoming data
     * @param in The {@link Boolean} received by the client
     */
    public abstract void receiveBool(boolean in);
    /**
     * Fires when the client receives data in the form of an {@link String}, this is where you should handle incoming data
     * @param in The {@link String} received by the client
     */
    public abstract void receiveString(String in);
    /**
     * Fires when the client receives data in the form of an {@link Object}, this is where you should handle incoming data
     * @param in The {@link Object} received by the client
     */
    public abstract void receiveObject(Object in);
    /**
     * Fires when client leaves or is kicked from a server, this is where you should handle when the client leaves a server
     * @param in The reason that the client is no longer connected, null if none was provided
     * @param forced False if the client left voluntarily (through a call to {@link ClientSideConnection#leaveServer()}), and true if the client's connection was closed forcibly (For instance through the {@link Server} on the other end being closed)
     */
    public abstract void leftServer(String in, boolean forced);
    /**
     * Fires when client receives information about a public server, this is where you should handle incoming public server data
     * @param address The address of the server that data was received from
     * @param data The data received from the server
     */
    public abstract void receiveServerInfo(InetSocketAddress address, byte[] data);

    /**
     * Closes the client, meaning it cannot be used again unless {@link ClientSideConnection#reopen()} is called. Any attempts to use the client to manage connections will throw a {@link ClientClosedException} until {@link ClientSideConnection#reopen()} is called.
     * @throws IOException If an I/O exception occurs
     */
    public void close() throws IOException {
        if(closed){throw new ClientClosedException();}
        closed = true;
        NetEventHandler.removeServerDataReceiver(this);
        leaveServer();
        udpPort.close();
    }

    /**
     * Returns whether the client has been closed. If it has, and you would like to open it again you can call {@link ClientSideConnection#reopen()}.
     * @return Whether the client is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Reopens the client after {@link ClientSideConnection#close()} is called.
     * @throws IOException If an I/O exception occurs
     */
    public void reopen() throws IOException {
        closed = false;
        udpPort = DatagramChannel.open();
        udpPort.bind(new InetSocketAddress(0));
        udpPort.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        udpPort.configureBlocking(false);
        udpPort.socket();
        NetEventHandler.registerServerDataReceiver(this);
    }
}
