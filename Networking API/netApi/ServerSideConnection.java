package netApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * The server-side version of a client. This is the type of client that is stored and accessed on servers.
 * @param <T> The type of server that this type of connection is associated with
 */
public abstract class ServerSideConnection<T extends Server> {
    /**
     * The server that the client is connected to
     */
    protected T server;
    /**
     * The {@link netApi.NetEventHandler.DataStream} connection to the client's device
     */
    protected NetEventHandler.DataStream serverSideConnection;
    /**
     * The connection to the client's device
     */
    protected SocketChannel connection;
    /**
     * Creates a new ServerClient
     * @param s Connection associated with the associated {@link ClientSideConnection}
     * @param serv {@link Server} associated with the ServerClient
     * @throws IOException If any I/O errors occur
     */
    public ServerSideConnection(SocketChannel s, T serv) throws IOException {
        ByteBuffer temp = ByteBuffer.allocate(Integer.BYTES);
        temp.putInt(0, serv.getServerType());
        s.write(temp);
        temp.clear();
        s.read(temp);
        if(temp.getInt(0) == -1){
            s.close();
            serv.clientCancelConnect(this);
            return;
        }
        s.configureBlocking(false);
        server = serv;
        connection = s;
        serverSideConnection = NetEventHandler.registerServerClient(this);
    }
    /**
     *Returns the associated {@link SocketChannel}
     * @return The {@link SocketChannel} associated with the client
     */
    public SocketChannel getConnection(){
        return connection;
    }
    /**
     * Closes the connection with the {@link ClientSideConnection}, removing them from the {@link Server} with the message being in's value
     * @param in Reason for being kicked
     */
    public void kick(String in) {
        try {
            serverSideConnection.sendCloseUpdate(in);
            serverSideConnection.close();
            NetEventHandler.removeServerClient(this);
        } catch (Exception e) {
            if (NetEventHandler.DEV_VERSION) {
                e.printStackTrace(System.out);
            }
        }
    }
    /**
     * Closes the connection with the {@link ClientSideConnection}, removing them from the {@link Server} with the message sent being null
     */
    public void kick(){
        kick(null);
    }
    /**
     * Checks whether the client is still connected to the {@link Server}
     * @return Whether the client is still connected to the {@link Server}
     */
    public boolean isConnected(){
        return serverSideConnection != null && serverSideConnection.isOpen();
    }
    /**
     * Sends the client the {@link Object} o, automatically parsing it if it is a primitive or a {@link String}
     * @param o Data to send to the {@link ClientSideConnection}
     * @throws IOException If any I/O errors occur
     */
    public void send(Object o) throws IOException {
        if(isConnected()) {
            if (o instanceof Integer) {
                serverSideConnection.sendInt((int) o);
            } else if (o instanceof Character) {
                serverSideConnection.sendChar((char) o);
            } else if (o instanceof Long) {
                serverSideConnection.sendLong((long) o);
            } else if (o instanceof Double) {
                serverSideConnection.sendDouble((double) o);
            } else if (o instanceof Byte) {
                serverSideConnection.sendByte((byte) o);
            } else if (o instanceof Short) {
                serverSideConnection.sendShort((short) o);
            } else if (o instanceof Float) {
                serverSideConnection.sendFloat((float) o);
            } else if (o instanceof Boolean) {
                serverSideConnection.sendBool((boolean) o);
            } else if (o instanceof String) {
                serverSideConnection.sendString((String) o);
            } else {
                serverSideConnection.sendObject(o);
            }
        }
    }

    /**
     * Returns the {@link Server} the connection is associated with.
     * @return The {@link Server} the connection is associated with.
     */
    public T getServer(){
        return server;
    }
    /**
     * Fires when the server receives data in the form of an {@link Integer}, this is where you should handle incoming data
     * @param in The {@link Integer} received by the server
     */
    public abstract void receiveInt(int in);
    /**
     * Fires when the server receives data in the form of an {@link Character}, this is where you should handle incoming data
     * @param in The {@link Character} received by the server
     */
    public abstract void receiveChar(char in);
    /**
     * Fires when the server receives data in the form of an {@link Long}, this is where you should handle incoming data
     * @param in The {@link Long} received by the server
     */
    public abstract void receiveLong(long in);
    /**
     * Fires when the server receives data in the form of an {@link Double}, this is where you should handle incoming data
     * @param in The {@link Double} received by the server
     */
    public abstract void receiveDouble(double in);
    /**
     * Fires when the server receives data in the form of an {@link Byte}, this is where you should handle incoming data
     * @param in The {@link Byte} received by the server
     */
    public abstract void receiveByte(byte in);
    /**
     * Fires when the server receives data in the form of an {@link Short}, this is where you should handle incoming data
     * @param in The {@link Short} received by the server
     */
    public abstract void receiveShort(short in);
    /**
     * Fires when the server receives data in the form of an {@link Float}, this is where you should handle incoming data
     * @param in The {@link Float} received by the server
     */
    public abstract void receiveFloat(float in);
    /**
     * Fires when the server receives data in the form of an {@link Boolean}, this is where you should handle incoming data
     * @param in The {@link Boolean} received by the server
     */
    public abstract void receiveBool(boolean in);
    /**
     * Fires when the server receives data in the form of an {@link String}, this is where you should handle incoming data
     * @param in The {@link String} received by the server
     */
    public abstract void receiveString(String in);
    /**
     * Fires when the server receives data in the form of an {@link Object}, this is where you should handle incoming data
     * @param in The {@link Object} received by the server
     */
    public abstract void receiveObject(Object in);
}
