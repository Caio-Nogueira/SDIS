import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class FileInfo implements Serializable {
    private File file;
    private ArrayList<Chunk> chunks = new ArrayList<>();
    private String fileID;
    private final int desiredReplicationDeg;
    private String fileName;

    public FileInfo(String path, int desiredReplicationDeg) {
        this.file = new File(path);
        this.desiredReplicationDeg = desiredReplicationDeg;
        generateFileID();
        //generateChunks();
        parseFileName(path);
    }

    public void addChunkToFile(Chunk chunk) {
        chunk.setBody(null); //Clear body to store the chunk
        this.chunks.add(chunk);
    }

    public String getFileID() {
        return fileID;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }

    public File getFile() {
        return file;
    }


    public String getFileName() {
        return this.fileName;
    }

    public int getDesiredReplicationDeg() {
        return desiredReplicationDeg;

    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    public void setChunks(ArrayList<Chunk> chunks) {
        this.chunks = chunks;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);
        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));
        // Pad with leading zeros
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }

    private void generateFileID() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            BasicFileAttributes attributes = Files.readAttributes(this.file.toPath(), BasicFileAttributes.class);
            String metadata = this.file.getAbsolutePath() + attributes.creationTime() + this.file.lastModified();
            this.fileID = toHexString(md.digest(metadata.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parseFileName(String path) {
         String[] splitPath = path.split("/");
         this.fileName = splitPath[splitPath.length-1];
    }

}