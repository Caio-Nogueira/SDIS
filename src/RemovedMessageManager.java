import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RemovedMessageManager implements Runnable{

    private int senderID, chunkNO;
    private String fileID;

    public RemovedMessageManager(byte[] message){
        /*
        * Message format:
        * <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        * */

        String msg = new String(message);
        String removedMessage = msg.split("\r\n\r\n", 2)[0].trim();

        String[] splitMsg = removedMessage.split(" ");
        this.senderID = Integer.parseInt(splitMsg[2]);
        this.fileID = splitMsg[3];
        this.chunkNO = Integer.parseInt(splitMsg[4]);

    }

    public void manageRemovedMessage(){
        //handles REMOVED message

        Chunk chunkStub = new Chunk(fileID, null, chunkNO, 0);
        Peer.getStorage().decReplicationDegree(chunkStub, this.senderID);

        Chunk chunk = Peer.getStorage().getChunkByKey(this.fileID, this.chunkNO);
        if (chunk == null) {
            return;          //PEER HAS NO INFORMATION ABOUT THE CHUNK
        }

        Peer.getStorage().removeChunkFromBackup(chunk);

        Random random = new Random();

        int delay = random.nextInt(401);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int perceivedRepDeg = Peer.getStorage().getPerceivedRepDegree(chunk);

        if (perceivedRepDeg < chunk.getReplicationDegree()) {

            if (Peer.getStorage().isGettingBackup(chunk)) {
                return;
            }

            //ACTIVATE BACKUP SUB-PROTOCOL AFTER DELAY
            String header = Peer.getProtocolVersion() + " PUTCHUNK " + Peer.getId() + " " + chunk.getFileID() + " " + chunk.getChunkNo() + " " + chunk.getReplicationDegree() + " \r\n\r\n";
            byte[] asciiHeader = header.getBytes(StandardCharsets.US_ASCII);

            byte[] body = Peer.getStorage().retrieveContentFromChunk(fileID, chunkNO);
            if (body == null) {
                System.out.println("Could not retrieve body");
                return;
            }
            byte[] putChunkMessage = new byte[asciiHeader.length + body.length];
            System.arraycopy(asciiHeader, 0, putChunkMessage, 0, asciiHeader.length);
            System.arraycopy(body, 0, putChunkMessage, asciiHeader.length, body.length);
            MessageForwarder messageForwarder = new MessageForwarder(new Message(putChunkMessage));
            Peer.getWorkers().execute(messageForwarder);
        }
    }

    @Override
    public void run() {
        manageRemovedMessage();
    }
}
