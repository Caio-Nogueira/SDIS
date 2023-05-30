import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PutChunkTask implements Runnable{
    private final MessageForwarder messageForwarder;
    private final int desiredReplicationDegree;
    private final Chunk chunk;
    private int time = 1;
    private int numTries = 0;
    private String fileID;
    private int chunkNo;

    public PutChunkTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
        String header = messageForwarder.getMessage().getHeader();
        String[] headerArgs = header.split(" ");
        this.desiredReplicationDegree = Integer.parseInt(headerArgs[5]);

        this.fileID = headerArgs[3].trim();
        this.chunkNo = Integer.parseInt(headerArgs[4]);
        this.chunk = new Chunk(this.fileID, this.messageForwarder.getMessage().getBody(), this.chunkNo, desiredReplicationDegree);

    }

    @Override
    public void run() {

        try {
            Peer.getBackupMC().sendMessage(this.messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(this.time * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int nbackups = Peer.getStorage().getPerceivedRepDegree(chunk);

        if (nbackups < this.desiredReplicationDegree && numTries < 5) {
            System.out.println("STORED message not received -> Retransmitting (n_tries=" + numTries + "; perceivedRepDegree: " + nbackups + ")");
            Peer.getWorkers().execute(this);
            this.numTries++;
            this.time *= 2;
        }
        else {
            Chunk chunk = Peer.getStorage().getChunkByKey(this.fileID, this.chunkNo);
            chunk.clearBody(); //CLEAR CHUNK FROM VOLATILE MEMORY
        }



    }
}
