import java.util.Random;

public class ActiveMessageManager implements Runnable{
    private int senderID = -1;
    private String protocolVersion;

    public ActiveMessageManager(byte[] message) {
        String messageStr = new String(message);
        String[] header = messageStr.split(" \r\n\r\n", 2);
        String[] messageFields = header[0].split( " ");
        this.protocolVersion = messageFields[0];

        if (messageFields.length != 3 || !this.protocolVersion.equals("2.0")){
            System.out.println("Invalid protocol version. Discarding ACTIVE message ...");
            return;
        }

        this.senderID = Integer.parseInt(messageFields[2]);

    }

    @Override
    public void run() {
        if (senderID == -1) return;

        else if (Peer.getProtocolVersion().equals("1.0")) return; //Protocol version not supported

        Random random = new Random();
        try {
            Thread.sleep(random.nextInt(401));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String fileID: Peer.getStorage().getFilesToDelete()){ //sends files to delete
            System.out.println("Sending DELETE message for file: " + fileID);
            System.out.println(Peer.getStorage().getFilesToDelete().size() + " files to delete");
            String deleteMessage = this.protocolVersion + " DELETE " + Peer.getId() + " " + fileID + " \r\n\r\n";
            MessageForwarder messageForwarder = new MessageForwarder(new Message(deleteMessage.getBytes()));
            Peer.getWorkers().execute(messageForwarder);
        }
    }
}
