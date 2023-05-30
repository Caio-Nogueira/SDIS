import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RestoreMessageManager implements Runnable {

    byte[] message;
    private String header;
    byte[] body;

    public RestoreMessageManager(byte[] message) {
        this.message = message;
        parseMessage();
    }

    public void parseMessage() {
        Message message = new Message(this.message);
        this.header = message.getHeader();
        this.body = message.getBody();
    }

    public void getChunkManager() {

        String[] splitHeader = this.header.split(" ");
        int chunkNo = Integer.parseInt(splitHeader[4].trim());
        int senderId = Integer.parseInt(splitHeader[2]);
        String fileID = splitHeader[3];


        Chunk chunk = Peer.getStorage().getChunkByNo(chunkNo, fileID);

        if(chunk == null) {
            return;
        }

        Random random = new Random();
        int delay = random.nextInt(401);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //CHECK IF A 'CHUNK' MESSAGE HAS ALREADY BEEN SENT BY OTHER PEERS
        for (Chunk chunk1: Peer.getStorage().getChunksAlreadySent()) {
            if (chunk1.getFileID().equals(chunk.getFileID()) && chunk.getChunkNo() == chunk1.getChunkNo()){
                return;
            }
        }


        String header = Peer.getProtocolVersion() + " CHUNK " + Peer.getId() + " " + fileID + " " + chunk.getChunkNo() + " \r\n\r\n";
        byte[] asciiHeader = header.getBytes(StandardCharsets.US_ASCII);
        byte[] body = Peer.getStorage().retrieveContentFromChunk(chunk.getFileID(), chunk.getChunkNo());
        byte[] chunkMessage = new byte[asciiHeader.length + body.length];
        System.arraycopy(asciiHeader, 0, chunkMessage, 0, asciiHeader.length);
        System.arraycopy(body, 0, chunkMessage, asciiHeader.length, body.length);
        Message chunkMsg = new Message(chunkMessage);

        if(Peer.getId() != senderId) {

            MessageForwarder messageForwarder = new MessageForwarder(chunkMsg);
            Peer.getWorkers().execute(messageForwarder);
        }

    }

    public void chunkManager() {

        String fileID = this.header.split(" ")[3].trim();
        int chunkNo = Integer.parseInt(this.header.split(" ")[4].trim());
        Chunk chunk = new Chunk(fileID, body, chunkNo, 0);
        chunk.setBody(null);

        Peer.getStorage().getChunksAlreadySent().add(chunk);
        System.out.println("Initiator peer received chunk " + chunkNo);
        if (Peer.getStorage().getChunksAlreadySentFromFile(fileID) == Peer.getStorage().getFileChunksNumber(fileID) && !Peer.getStorage().containsFile(fileID)) {
            try {
                Thread.sleep(402);
                System.out.println("Clearing list for file: " + fileID);
                Peer.getStorage().clearChunksFromRestoreList(fileID);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        if (!Peer.getStorage().containsFile(fileID)) return;
        System.out.println("saving chunk " + chunkNo);

        String fileName = Peer.getStorage().getFileByFileID(fileID).getFile().getName();
        File file = new File("Peer" + Peer.getId() + File.separator +fileID + "_" + fileName);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        RandomAccessFile restoredFile = null;
        try {
            restoredFile = new RandomAccessFile(file, "rw");
            restoredFile.seek(chunkNo* 64000L);
            restoredFile.write(this.body);
            restoredFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        String messageType = this.header.split(" ")[1];
        
        switch(messageType) {
            case "GETCHUNK":
                getChunkManager();
                break;
            case "CHUNK":
                chunkManager();
                break;
            default:
                break;
        }
    }
}
