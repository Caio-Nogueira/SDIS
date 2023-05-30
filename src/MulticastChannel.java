import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

public class MulticastChannel extends MulticastSocket implements Runnable{
    private InetAddress address;
    private int mcPort;

    public MulticastChannel(String address, int port) throws IOException {
        super(port);

        try {
            this.address = InetAddress.getByName(address);
            this.mcPort = port;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, address, mcPort);
        this.send(packet);
    }

    @Override
    public void run() {
        byte[] buf = new byte[64500];

        try {

            this.joinGroup(address); //deprecated for some JAVA versions

            while(true){
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.receive(packet);
                byte[] bufferCopy = Arrays.copyOf(buf, packet.getLength()); //last chunk will not be 64kb long
                MessageReceiver messageReceiver = new MessageReceiver(new Message(bufferCopy));
                Peer.getWorkers().execute(messageReceiver);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
