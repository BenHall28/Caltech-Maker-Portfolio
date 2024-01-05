package netApi;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Util class for storing, keeping track of, and running all listeners, you will likely not ever need to directly touch this
 * @hidden
 */
public class NetEventHandler {
    /**
     * Toggle for enabling dev mode. When false all errors will be hidden. When true all errors will be displayed in console. It is important to note that some errors are normal and expected, and do not disrupt the program.
     */
    public static boolean DEV_VERSION = false;
    /**
     * Toggle for ending all currently running programs
     */
    private static boolean done = false;
    private static Thread main;
    /**
     * Thread where all listener types are run
     */
    private static final Thread SVC = new Thread(() -> {
        while (!done && Thread.activeCount() > 1) {
            listenerCheck();
        }
    }, "NetEventHandler");
    /**
     * Storage of all ServerListeners
     */
    private static List<Server> servers, removeableServers;
    private static List<ClientSideConnection> serverReceiver, removeableClientDatas, removableServerReceivers;
    private static List<ServerSideConnection> removeableServerDatas;
    /**
     * Storage of all InputListeners
     */
    private static Map<ServerSideConnection, DataStream> serverDatas;
    /**
     * Storage of all ClientListeners
     */
    private static Map<ClientSideConnection, DataStream> localDatas;
    /**
     * Util buffers used for data transfer and handling
     */
    private static ByteBuffer datagram, serverData, byteIn, portIn, serverDataVerify, intIn;

    /**
     * Constructs the class and all it's methods. Should only be run once
     */
    private NetEventHandler() {
        if(SVC.isAlive()){return;}
        main = Thread.currentThread();
        servers = new ArrayList();
        serverReceiver = new ArrayList();
        serverDatas = new HashMap();
        removeableServerDatas = new ArrayList();
        removeableClientDatas = new ArrayList();
        removableServerReceivers = new ArrayList();
        removeableServers = new ArrayList();
        datagram = ByteBuffer.allocate(Integer.BYTES);
        byteIn = ByteBuffer.allocate(1);
        localDatas = new HashMap();
        portIn = ByteBuffer.allocate(2);
        intIn = ByteBuffer.allocate(Integer.BYTES);
        serverDataVerify = ByteBuffer.allocate("sinfo".getBytes().length);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SVC.interrupt();
            done = true;
            servers.forEach(server -> {
            try {
                server.close();
            }  catch (ConcurrentModificationException | IOException e) {
                throw new RuntimeException(e);
            }
            });
            localDatas.forEach((c, d) -> {
            try {
                c.leaveServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        },"NetEventCloser"));
        SVC.start();
    }

    /**
     * Util method for checking all listeners
     */
    static synchronized private void listenerCheck() {
        if(done){return;}
        serverMemberUpdate();
        if(done){return;}
        dataStreamUpdate();
        if(done){return;}
        clientDataStreamUpdate();
    }
    /***
     *  Checks through all registered {@link Server}s to fire all active events
     */
    private static synchronized void serverMemberUpdate() {
        servers.forEach(s -> {
            if(s.isOpen() && !done){
                try {
                    if (s.acceptingNewClients()) {
                        SocketAddress add;
                        SocketChannel c = s.getSocket().accept();
                        while (c != null) {
                            c.finishConnect();
                            if (c.isOpen()) {
                                ServerSideConnection tempC = s.createClient(c);
                                s.clientConnectPreAuth(tempC);
                                if (s.serverSideAuth(c)) {
                                    s.addServerClient(tempC);
                                    s.clientConnectPostAuth(tempC);
                                } else {
                                    c.close();
                                }
                            }
                            try {
                                c = s.getSocket().accept();
                            } catch (Exception e) {
                                c = null;
                            }
                        }
                    }
                    //ServerSideConnection[] disconnected = s.removedClients();
                    //for (int i = 0; i < disconnected.length; i++) {
                    //    int finalI = i;
                    //    s.clientDisconnect(disconnected[finalI]);
                    //}
                } catch (IOException e) {
                    if(NetEventHandler.DEV_VERSION){
                        e.printStackTrace(System.out);
                    }
                }
            }
            if (s.isOpen() && s.isPublic() && s.acceptingNewClients() && !done) {
                SocketAddress a = null;
                try {
                    if ((a = s.getUdpPort().receive(datagram)) != null) {
                        if (datagram.getInt(0) == s.getServerType()) {
                            s.sendServerInfo(a);
                        }
                        datagram.clear();
                    }
                } catch (Exception ex) {
                    if(NetEventHandler.DEV_VERSION){
                        ex.printStackTrace(System.out);
                    }
                }
            }
        });
        removeableServers.forEach(servers::remove);
        removeableServers.clear();
    }
    /***
     *  Checks through all registered {@link ServerSideConnection}s to fire events
     */
    private static synchronized void dataStreamUpdate(){
        serverDatas.forEach((c, d) -> {
            try {
                if (!done && c.isConnected() && d.getDataSender().read(byteIn) != 0) {
                    byte type = byteIn.get(0);
                    byteIn.clear();
                    switch (type) {
                        case 0:
                            try {
                                c.receiveInt(d.readInt());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 1:
                            try {
                                c.receiveChar(d.readChar());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 2:
                            try {
                                c.receiveLong(d.readLong());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 3:
                            try {
                                c.receiveDouble(d.readDouble());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 4:
                            try {
                                c.receiveByte(d.readByte());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 5:
                            try {
                                c.receiveShort(d.readShort());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 6:
                            try {
                                c.receiveFloat(d.readFloat());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 7:
                            try {
                                c.receiveBool(d.readBool());
                            } catch (IOException e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 8:
                            try {
                                c.receiveString(d.readString());
                            } catch (Exception e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 9:
                            try {
                                c.receiveObject(d.readObject());
                            } catch (Exception e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            break;
                        case 11:
                            try {
                                c.server.clientDisconnect(c, d.readString());
                                d.close();
                                removeableServerDatas.add(c);
                            } catch (Exception e) {
                                if(NetEventHandler.DEV_VERSION){
                                    e.printStackTrace(System.out);
                                }
                            }
                            d.close();
                            break;
                        case 12:
                            c.server.clientDisconnect(c, null);
                            d.close();
                            removeableServerDatas.add(c);
                    }
                }
            } catch (ClosedChannelException e){}
            catch(SocketException e){
                if(!done && d != null && c.getServer() != null){
                    c.server.clientDisconnect(c, null);
                    c.getServer().removeServerClient(c);
                    removeableServerDatas.add(c);
                }
            }
            catch (IOException e) {
                if(NetEventHandler.DEV_VERSION){
                    e.printStackTrace(System.out);
                }
            }
        });
        removeableServerDatas.forEach(serverDatas::remove);
        removeableServerDatas.clear();
    }
    /***
     *  Checks through all registered {@link ClientSideConnection}s and their associated {@link DataStream}s to fire all active events
     */
    private static synchronized void clientDataStreamUpdate(){
        localDatas.forEach((c, d) -> {
                    try {
                        if (!done && c.inServer() && d.getDataSender().read(byteIn) != 0) {
                            byte type = byteIn.get(0);
                            byteIn.clear();
                            switch (type) {
                                case 0:
                                    try {
                                        c.receiveInt(d.readInt());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 1:
                                    try {
                                        c.receiveChar(d.readChar());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 2:
                                    try {
                                        c.receiveLong(d.readLong());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 3:
                                    try {
                                        c.receiveDouble(d.readDouble());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 4:
                                    try {
                                        c.receiveByte(d.readByte());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 5:
                                    try {
                                        c.receiveShort(d.readShort());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 6:
                                    try {
                                        c.receiveFloat(d.readFloat());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 7:
                                    try {
                                        c.receiveBool(d.readBool());
                                    } catch (IOException e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 8:
                                    try {
                                        c.receiveString(d.readString());
                                    } catch (Exception e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 9:
                                    try {
                                        c.receiveObject(d.readObject());
                                    } catch (Exception e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 11:
                                    try {
                                        c.leaveServer(d.readString(), true);
                                        removeLocalClient(c);
                                    } catch (Exception e) {
                                        if (NetEventHandler.DEV_VERSION) {
                                            e.printStackTrace(System.out);
                                        }
                                    }
                                    break;
                                case 12:
                                    c.leaveServer(null, true);
                                    break;
                            }
                        }
                    } catch (SocketException e) {
                        try {
                            c.leaveServer("The connection was forcibly closed from the other side", true);
                        } catch (IOException ex) {
                        }
                    } catch (IOException e) {
                        if (NetEventHandler.DEV_VERSION) {
                            e.printStackTrace(System.out);
                        }
                    }
                    if (c.inServer() && !d.isOpen()) {
                        try {
                            c.leaveServer("The connection was forcibly closed from the other side", true);
                        } catch (IOException e) {
                            if (NetEventHandler.DEV_VERSION) {
                                e.printStackTrace(System.out);
                            }
                        }
                    }
                }
            );
            removeableClientDatas.forEach(u -> {
                localDatas.remove(u);
            });
            removeableClientDatas.clear();
            serverReceiver.forEach(c -> {try {
                        SocketAddress a;
                LinkedList<Byte> out = new LinkedList();
                if (!done && (a = c.getUdpPort().receive(serverDataVerify)) != null) {
                    if(new String(serverDataVerify.array()).equals("sinfo")) {
                        serverDataVerify.clear();
                        while(c.getUdpPort().receive(portIn) == null){}
                        int port = ((portIn.array()[1] & 255) << Byte.SIZE) | (portIn.array()[0] & 255);
                        portIn.clear();
                        while(c.getUdpPort().receive(intIn) == null){}
                        int temp = intIn.getInt(0);
                        intIn.clear();
                        serverData = ByteBuffer.allocate(temp);
                        while (c.getUdpPort().receive(serverData) == null) {
                        }
                        byte[] serverDataArr = Arrays.copyOf(serverData.array(), serverData.position());
                        serverData.clear();
                        SocketAddress finalA = a;
                            c.receiveServerInfo(new InetSocketAddress(((InetSocketAddress) finalA).getAddress(), port), serverDataArr);
                    }
                }
            } catch (Exception ex) {
                if(NetEventHandler.DEV_VERSION){
                    ex.printStackTrace(System.out);
                }
            }
            }
            );
        removableServerReceivers.forEach(serverReceiver::remove);
        removableServerReceivers.clear();
    }
    public static DataStream registerServerClient(ServerSideConnection s){
        new NetEventHandler();
        serverDatas.put(s, new DataStream(s.connection));
        return serverDatas.get(s);
    }
    public static DataStream registerLocalClient(ClientSideConnection s, SocketChannel in){
        new NetEventHandler();
        localDatas.put(s, new DataStream(in));
        return localDatas.get(s);
    }
    public static void removeServerClient(ServerSideConnection s){
        removeableServerDatas.add(s);
    }
    public static void removeLocalClient(ClientSideConnection s){
        removeableClientDatas.add(s);
    }
    public static void registerServer(Server s){
        new NetEventHandler();
        if(!servers.contains(s)) {
            servers.add(s);
        }
    }

    public static void removeServer(Server s) {
        removeableServers.add(s);
    }
    public static void registerServerDataReceiver(ClientSideConnection s){
        new NetEventHandler();
        serverReceiver.add(s);
    }
    public static void removeServerDataReceiver(ClientSideConnection s){
        removableServerReceivers.add(s);
    }

    /**
     * Used for the transfer of data
     * @hidden
     */
    public static class DataStream{
        protected final SocketChannel stored;
        /**
         * Whether the connection is open
         */
        protected boolean open;
        /**
         * {@link ByteBuffer}s for use of reading and writing data
         */
        protected final ByteBuffer intSend = ByteBuffer.allocate(Integer.BYTES), charSend = ByteBuffer.allocate(Character.BYTES), longSend = ByteBuffer.allocate(Long.BYTES), doubleSend = ByteBuffer.allocate(Double.BYTES), byteSend = ByteBuffer.allocate(1), shortSend = ByteBuffer.allocate(Short.BYTES), floatSend = ByteBuffer.allocate(Float.BYTES), booleanSend = ByteBuffer.allocate(1);
        /**
         * The associated {@link ObjectOutputStream}
         */
        protected final ObjectOutputStream objectOut;
        /**
         * The associated {@link PipedInputStream}
         */
        protected final PipedInputStream pipeIn;
        /**
         * The associated {@link ObjectInputStream}
         */
        protected final ObjectInputStream objectIn;
        /**
         * The associated {@link PipedOutputStream}
         */
        protected final PipedOutputStream byteOut;
        /**
         * Initializes the DataStream
         * @param in Input source used by the DataStream
         */
        private DataStream(SocketChannel in){
            this.stored = in;
            open = true;
            try {
                byteOut = new PipedOutputStream();
                PipedInputStream tempIn = new PipedInputStream(byteOut);
                new ObjectOutputStream(byteOut);
                objectIn = new ObjectInputStream(tempIn);
                pipeIn = new PipedInputStream();
                objectOut = new ObjectOutputStream(new PipedOutputStream(pipeIn));
                new ObjectInputStream(pipeIn);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        /**
         * Sends an {@link Integer} through the associated {@link ObjectOutputStream}
         * @param in {@link Integer} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendInt(int in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{0}));
            intSend.putInt(0, in);
            stored.write(intSend);
            intSend.clear();
        }
        /**
         * Sends a {@link Character} through the associated {@link ObjectOutputStream}
         * @param in {@link Character} that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendChar(char in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{1}));
            charSend.putChar(0, in);
            stored.write(charSend);
            charSend.clear();}
        /**
         * Sends a {@link Long} through the associated {@link ObjectOutputStream}
         * @param in {@link Long} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendLong(long in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{2}));
            longSend.putLong(0, in);
            stored.write(longSend);
            longSend.clear();
        }
        /**
         * Sends a {@link Double} through the associated {@link ObjectOutputStream}
         * @param in {@link Double} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendDouble(double in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{3}));
            doubleSend.putDouble(0, in);
            stored.write(doubleSend);
            doubleSend.clear();
        }
        /**
         * Sends a {@link Byte} through the associated {@link ObjectOutputStream}
         * @param in {@link Byte} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendByte(byte in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{4}));
            byteSend.put(0, in);
            stored.write(byteSend);
            byteSend.clear();
        }
        /**
         * Sends a {@link Short} through the associated {@link ObjectOutputStream}
         * @param in {@link Short} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendShort(short in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{5}));
            shortSend.putShort(0, in);
            stored.write(shortSend);
            shortSend.clear();
        }
        /**
         * Sends a {@link Float} through the associated {@link ObjectOutputStream}
         * @param in {@link Float} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendFloat(float in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{6}));
            floatSend.putFloat(0, in);
            stored.write(floatSend);
            floatSend.clear();
        }
        /**
         * Sends a {@link Boolean} through the associated {@link ObjectOutputStream}
         * @param in {@link Boolean} value that was received from the connection
         * @throws IOException If an I/O error occurs while writing to the underlying stream.
         */
        synchronized public void sendBool(boolean in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{7}));
            if(in){booleanSend.put(0, (byte) 1);}else{booleanSend.put(0, (byte) 0);}
            stored.write(booleanSend);
            booleanSend.clear();
        }
        /**
         * Sends a {@link String} through the associated {@link ObjectOutputStream}
         * @param in {@link String} that was received from the connection
         * @throws IOException Any exception thrown by the underlying {@link OutputStream}.
         * @throws InvalidClassException Something is wrong with a class used by serialization.
         */
        synchronized public void sendString(String in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            stored.write(ByteBuffer.wrap(new byte[]{8}));
            intSend.putInt(0, in.length());
            stored.write(intSend);
            intSend.clear();
            for(int i = 0; i < in.length(); i++){
                charSend.putChar(0, in.charAt(i));
                stored.write(charSend);
                charSend.clear();
            }
        }
        /**
         * Sends an {@link Object} through the associated {@link ObjectOutputStream}
         * @param in {@link Object} to be sent through the stream
         * @throws IOException Any exception thrown by the underlying {@link OutputStream}.
         * @throws InvalidClassException Something is wrong with a class used by serialization.
         */
        synchronized public void sendObject(Object in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            objectOut.writeObject(in);
            objectOut.flush();
            byte temp = 0, ind = Byte.SIZE-1;
            if(pipeIn.available() == 0){return;}
            byte i;
            for(i = (byte)pipeIn.read();; i = (byte)pipeIn.read()){
                byteSend.put(0, (byte)((((255&i)<<(Byte.SIZE-1-ind))&~(1<<Byte.SIZE-1))|temp));
                temp=(byte)((255&i)>>(ind--));
                stored.write(byteSend);
                byteSend.clear();
                if(ind == 0){
                    byteSend.put(0, temp);
                    stored.write(byteSend);
                    byteSend.clear();
                    ind = Byte.SIZE-1;
                    temp = 0;
                }
                if(pipeIn.available() == 0){break;}
            }
            if(ind == 1){
                byteSend.put(0, (byte)(((255&i)<<(Byte.SIZE-1-ind))|temp|(1<<Byte.SIZE-1)));
            }
            else{
                byteSend.put(0, (byte)(((255&i)<<(Byte.SIZE-1-ind))|temp));
                temp=(byte)(((255&i)>>ind)|(1<<Byte.SIZE-1));
                stored.write(byteSend);
                byteSend.clear();
                byteSend.put(0, temp);
            }
            stored.write(byteSend);
            byteSend.clear();
        }

        /**
         * Util method for closing DataStreams, you should not have to use this
         * @param in Reason for closing
         * @throws IOException If any I/O errors occur
         */
        public void sendCloseUpdate(String in) throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            if(in == null) {
                stored.write(ByteBuffer.wrap(new byte[]{12}));
            }
            else{
                stored.write(ByteBuffer.wrap(new byte[]{11}));
                intSend.putInt(0, in.length());
                stored.write(intSend);
                intSend.clear();
                for(int i = 0; i < in.length(); i++){
                    charSend.putChar(0, in.charAt(i));
                    stored.write(charSend);
                    charSend.clear();
                }
            }
        }
        /**
         * Receives an {@link Integer} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Integer} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public int readInt() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Integer.BYTES){
                i+=stored.read(intSend);
            }
            int out = intSend.getInt(0);
            intSend.clear();
            return out;
        }
        /**
         * Receives an {@link Character} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return A {@link Character} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public char readChar() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Character.BYTES){
                i+=stored.read(charSend);
            }
            char out = charSend.getChar(0);
            charSend.clear();
            return out;
        }
        /**
         * Receives an {@link Double} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Double} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public double readDouble() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Double.BYTES){
                i+=stored.read(doubleSend);
            }
            double out = doubleSend.getDouble(0);
            doubleSend.clear();
            return out;}
        /**
         * Receives an {@link Long} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Long} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public long readLong() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Long.BYTES){
                i+=stored.read(longSend);
            }
            long out = longSend.getLong(0);
            longSend.clear();
            return out;
        }
        /**
         * Receives an {@link Byte} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Byte} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public byte readByte() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Byte.BYTES){
                i+=stored.read(byteSend);
            }
            byte out = byteSend.get(0);
            byteSend.clear();
            return out;
        }
        /**
         * Receives an {@link Short} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Short} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public short readShort() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Short.BYTES){
                i+=stored.read(shortSend);
            }
            short out = shortSend.getShort(0);
            shortSend.clear();
            return out;
        }
        /**
         * Receives an {@link Float} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Float} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public float readFloat() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < Float.BYTES){
                i+=stored.read(floatSend);
            }
            float out = floatSend.getFloat(0);
            floatSend.clear();
            return out;
        }
        /**
         * Receives an {@link Boolean} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Boolean} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public boolean readBool() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int i = 0;
            while(i < 1){
                i+=stored.read(booleanSend);
            }
            boolean out = booleanSend.get(0) == 1;
            booleanSend.clear();
            return out;
        }
        /**
         * Receives an {@link String} through the associated {@link ObjectOutputStream} (Freezing the thread until one is available if it isn't already)
         * @return An {@link String} received through the DataStream
         * @throws IOException If an I/O error occurs while reading from the underlying stream.
         */
        synchronized public String readString() throws IOException {
            if(!open){throw new IOException("The DataStream is closed");}
            int length = readInt();
            String out = "";
            for(int i = 0; i < length; i++){
                out+=readChar();
            }
            return out;
        }
        /**
         * Receives an {@link Object} through the associated {@link SocketChannel} (Freezing the thread until one is available if it isn't already)
         * @return An {@link Object} received through the DataStream
         * @throws IOException Any exception thrown by the underlying {@link SocketChannel}.
         * @throws InvalidClassException Something is wrong with a class used by serialization.
         * @throws ClassNotFoundException Class of a serialized object cannot be found.
         */
        synchronized public Object readObject() throws IOException, ClassNotFoundException {if(!open){throw new IOException("The DataStream is closed");}
            byte read = 0, buffer = 0, ind = 0;
            byteSend.clear();
            while((read&(1<<Byte.SIZE-1)) == 0){
                while(stored.read(byteSend) == 0){byteSend.clear();}
                read = byteSend.get(0);
                byte temp = (byte)(read&~(1<<Byte.SIZE-1));
                byteSend.clear();
                buffer|=temp<<ind; ind+=Byte.SIZE-1;
                if(ind > 7){
                    byteOut.write(buffer);
                    ind%=8;
                    buffer=(byte)(temp>>Byte.SIZE-1-ind);
                }
            }
            return objectIn.readObject();
        }
        /**
         * Closes the DataStream and associated {@link SocketChannel}. You should not call this directly unless you know what you're doing.
         */
        public void close() {
            open = false;
            try {stored.close();} catch (IOException e) {}
        }

        /**
         * Checks if the DataStream is open
         * @return If the DataStream is open
         */
        public boolean isOpen() {
            return open;
        }
        /**
         * Returns the {@link SocketChannel} associated with this DataStream
         * @return The DataStream's associated {@link SocketChannel} used for reading and writing
         */
        public SocketChannel getDataSender() {
            return stored;
        }
    }
}