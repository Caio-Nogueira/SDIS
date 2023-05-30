import java.io.Serializable;
import java.util.Objects;

public class Chunk implements Serializable {
    private String fileID;
    private byte[] body;
    private int chunkNo;
    private int replicationDegree;
    private int senderID = -1;
    private int contentLength = 0;


    public Chunk(String fileID, byte[] body, int chunkNo, int replicationDegree) {
        this.fileID = fileID;
        this.body = body;
        this.chunkNo = chunkNo;
        this.replicationDegree = replicationDegree;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public byte[] getBody() {
        return body;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public String getFileID() {
        return fileID;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void clearBody() {   //TODO: CLEAR CHUNK BODY TO FREE VOLATILE MEMORY
        this.body = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return chunkNo == chunk.getChunkNo() && fileID.equals(chunk.getFileID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileID, chunkNo);
    }
}
