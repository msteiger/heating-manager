package sma.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import sma.domain.em.DataBlock;

/**
 * The {@link EnergyMeterService} class is responsible for communication with the SMA device
 * and extracting the data fields out of the received telegrams.
 */
public class EnergyMeterService {

    private String multicastGroup;
    private int port;
    private int timeout;

    public static final String DEFAULT_MCAST_GRP = "239.12.255.254";
    public static final int DEFAULT_MCAST_PORT = 9522;
    public static final int DEFAULT_TIMEOUT = 5000;

    public EnergyMeterService() {
        this(DEFAULT_MCAST_GRP, DEFAULT_MCAST_PORT, DEFAULT_TIMEOUT);
    }

    public EnergyMeterService(String multicastGroup, int port, int timeout) {
        this.multicastGroup = multicastGroup;
        this.port = port;
        this.timeout = timeout;
    }

    public DataBlock waitForBroadcast() throws IOException {
        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.setSoTimeout(timeout);
            InetAddress address = InetAddress.getByName(multicastGroup);
            socket.joinGroup(address);

            byte[] bytes = new byte[608];
            DatagramPacket msgPacket = new DatagramPacket(bytes, bytes.length);
            socket.receive(msgPacket);

            String sma = new String(Arrays.copyOfRange(bytes, 0x00, 0x03));
            if (!sma.equals("SMA")) {
                throw new IOException("Not a SMA telegram." + sma);
            }

//            ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0x14, 0x18));
//            String serialNumber = String.valueOf(buffer.getInt());

            return new DataBlock(bytes);
        }
    }
}

