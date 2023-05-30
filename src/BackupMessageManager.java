import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;


public class BackupMessageManager implements Runnable{
    private byte[] message;

    private String header;
    private byte[] body;
    private String protocolVersion;

    /*
    Message format putchunk: <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
    Message format stored: <Version> STORED <senderId> <FieldId> <ChunkNo> <CRLF><CLRF>
     */

    public BackupMessageManager(byte[] message){
        this.message = message;
        parseMessage();
    }

    public void parseMessage() {
        Message message = new Message(this.message);
        this.header = message.getHeader();
        this.body = message.getBody();
        this.protocolVersion = this.header.split(" ")[0];
    }

    public synchronized void managePutChunkMessage() {
        String[] splitMessage = this.header.split(" ");
        int senderID = Integer.parseInt(splitMessage[2].trim());
        String fileID = splitMessage[3].trim();
        int chunkNo = Integer.parseInt(splitMessage[4].trim());
        int replicationDegree = Integer.parseInt(splitMessage[5].trim());

        if (senderID != Peer.getId()) {     //only stores chunk if it doesnt belong to the current peer
            Chunk chunk = new Chunk(fileID, this.body, chunkNo, replicationDegree);
            chunk.setContentLength(this.body.length);
            receiveChunk(chunk);
        }
    }

    private void receiveChunk(Chunk chunk){
        for (int i = 0; i < Peer.getStorage().getFiles().size(); i++) {
            if (Peer.getStorage().getFiles().get(i).getFileID().equals(chunk.getFileID())) return; //the Peer owns the file - no need to store its chunks
        }

        Peer.getStorage().addChunkToBackup(chunk);

        if (Peer.getStorage().containsChunk(chunk)) {
            sendStoredMessage(chunk);
            return;
        }

        String filename = "Peer" + Peer.getId() + "/" + chunk.getFileID() + "/" + chunk.getFileID() + "-" + chunk.getChunkNo();
        File chunkFile = new File(filename);


        Random random = new Random();
        int delay = random.nextInt(401);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (Peer.getStorage().getSpaceAvailable() < chunk.getBody().length) {
            Peer.getStorage().deleteRedundantChunks();
            if (Peer.getStorage().getSpaceAvailable() < chunk.getBody().length) {
                return;
            }
        }


        if (chunkFile.exists()) {
            System.out.println("File already exists.");
        }

        int perceivedRepDeg = Peer.getStorage().getPerceivedRepDegree(chunk);

        if (perceivedRepDeg >= chunk.getReplicationDegree() && this.protocolVersion.equals("2.0") && Peer.getProtocolVersion().equals("2.0")) {
            //ENHANCED PROTOCOL
            return;
        }

        try {
            if (chunkFile.getParentFile().mkdirs()) return;
            Peer.getStorage().addChunk(chunk);
            Peer.getStorage().incReplicationDegree(chunk, Peer.getId());
            System.out.println("Created file: " + Peer.getId() + "-" + chunk.getChunkNo());
            FileOutputStream fos = new FileOutputStream(filename);

            if (this.body != null) fos.write(this.body);

            fos.flush();
            fos.close();

            Peer.getStorage().clearChunkBody(chunk.getFileID(), chunk.getChunkNo());

        } catch (IOException e) {
            e.printStackTrace();
        }

        sendStoredMessage(chunk);
    }

    public void sendStoredMessage(Chunk chunk) {

        String message = Peer.getProtocolVersion() + " " + "STORED " + Peer.getId() + " " + chunk.getFileID() + " " + chunk.getChunkNo() +  " \r\n\r\n";
        MessageForwarder messageForwarder = new MessageForwarder(new Message(message.getBytes()));
        Peer.getWorkers().execute(messageForwarder);
    }

    public synchronized void manageStoredMessage() {
        String[] splitMessage = this.header.split(" ");
        int senderID = Integer.parseInt(splitMessage[2].trim());
        int chunkNo = Integer.parseInt(splitMessage[4].trim());
        String fileID = splitMessage[3];
        Chunk chunk = new Chunk(fileID, this.message, chunkNo, 0);
        Peer.getStorage().incReplicationDegree(chunk, senderID);
    }

    @Override
    public void run() {

        String operation = this.header.split(" ")[1];

        switch (operation) {
            case "PUTCHUNK":
                managePutChunkMessage(); //handles PUTCHUNK message
                break;
            case "STORED":
                manageStoredMessage(); //handles STORED message
                break;
            default:
                break;
        }

    }
}
