package netApi;

import java.net.*;

/**
 * Util class for handling ip addresses, ports, and {@link NetworkInterface}s. This can be used to generate and process codes representing IP addresses and ports, as well as manage {@link NetworkInterface} used for communication.
 */
public class NetworkUtil {
    /**
     * This is just here to stop javadocs compilation warnings
     * @hidden
     */
    public NetworkUtil(){}
    /**
     * Turns an ip address and a port into a code for client use when connecting to a server. The code can be turned back to an ip address and port through {@link NetworkUtil#processCode(String)}.
     * @param ip Ip address used to generate code
     * @param port Port used to generate code
     * @return Code version of ip address and port
     */
    public static String genCode(InetAddress ip, int port){
        return genCode(new InetSocketAddress(ip, port));
    }
    /**
     * Turns a {@link InetSocketAddress} into a code. The code can be turned back to an ip address and port through {@link NetworkUtil#processCode(String)}.
     * @param add {@link InetSocketAddress} used to generate code
     * @return Code version of {@link InetSocketAddress}
     */
    public static String genCode(InetSocketAddress add){
        long num = add.getPort();
        long mult = 100000;
        for(int i = add.getAddress().getAddress().length-1; i >= 0; i--){
            num+=mult*((256+add.getAddress().getAddress()[i])%256);
            mult*=1000;
        }
        String out = "";
        for(int i = (int)(Math.log(num)/Math.log(36)); i >= 0; i--){
            int place = (int)Math.max(0, num/Math.pow(36, i));
            num = num-(long)(place*Math.pow(36, i));
            if(place > 9){out+=(char)(65+place-10);}
            else{out+=place;}
        }
        return out;
    }
    /**
     * Turns a code into an {@link InetSocketAddress}. The code must have been created through {@link NetworkUtil#genCode(InetSocketAddress)} or {@link NetworkUtil#genCode(InetAddress, int)}.
     * @param code Code version of ip address
     * @return Ip address and port generated from code
     * @throws UnknownHostException If code is invalid
     */
    public static InetSocketAddress processCode(String code) throws UnknownHostException {
        long num = 0;
        for(int i = code.length()-1; i >= 0; i--){
            if(Character.isDigit(code.charAt(i))){num+=((long)Math.pow(36, code.length()-1-i))*Character.getNumericValue(code.charAt(i));}
            else{num+=((long)Math.pow(36, code.length()-1-i))*((int)code.charAt(i)-55);}
        }
        long ipNum = num/100000, port = num%100000;
        String ip = ""+ipNum;
        while((ip.length()/3) != (int)Math.ceil(1.0*ip.length()/3)){
            ip = "0"+ip;
        }
        byte[] address = new byte[(int)Math.ceil(1.0*ip.length()/3)];
        for(int i = 0; i < address.length; i++){
            address[i] = (byte)((Integer.parseInt(ip.substring(i*3, i*3+3))+128)%256-128);
        }
        return new InetSocketAddress(InetAddress.getByAddress(address), (int)port);
    }
    /**
     * Return's the {@link NetworkInterface} used for multicasting and general networking. While you will likely never use this it is here for your convenience.
     * @return The {@link NetworkInterface} used for multicasting and general networking
     * @throws SocketException If an I/O error occurs or there are no usable {@link NetworkInterface}s
     */
    public static NetworkInterface getUsableInterface() throws SocketException {
        for(Object inter : NetworkInterface.networkInterfaces().toArray()){
            if(((NetworkInterface)inter).isVirtual() || ((NetworkInterface)inter).getDisplayName().toLowerCase().contains("virtualbox") || !((NetworkInterface)inter).isUp() || ((NetworkInterface)inter).isLoopback()){continue;}
            for(InterfaceAddress iAdd : ((NetworkInterface)inter).getInterfaceAddresses()){
                if(iAdd.getAddress() instanceof Inet4Address){
                    return (NetworkInterface)inter;
                }
            }
        }
        throw new SocketException("No usable network interfaces found. This is almost definitely an issue with your computer, not the current program.");
    }
}
