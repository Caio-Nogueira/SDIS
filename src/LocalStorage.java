import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage implements Serializable {

    private ArrayList<Chunk> listChunks = new ArrayList<>();
    private ArrayList<FileInfo> files = new ArrayList<>(); //Files backed up by other peers
    private List<Chunk> chunksGettingBackup = Collections.synchronizedList(new ArrayList<Chunk>());
    private ConcurrentHashMap<String, HashSet<Integer>> numberOfTimesChunkWasBackup = new ConcurrentHashMap<String, HashSet<Integer>>();
    private long spaceAvailable;
    private List<String> filesToDelete = new ArrayList<>();
    private HashSet<Chunk> chunksAlreadySent = new HashSet<>(Collections.newSetFromMap(new ConcurrentHashMap<Chunk,Boolean>()));

    public HashSet<Chunk> getChunksAlreadySent() {
        return chunksAlreadySent;
    }

    public List<Chunk> getChunksGettingBackup() {
        return chunksGettingBackup;
    }

    public List<String> getFilesToDelete() {
        return filesToDelete;
    }


    public FileInfo getFileByFileID(String FileID) {
        //seacrch a file by its ID
        for (FileInfo file: files){
            if (file.getFileID().equals(FileID)) return  file;
        }
        return null;
    }

    public LocalStorage() {
        this.spaceAvailable = 64000000000L; //set 64gb as maximum limit
    }

    public ArrayList<Chunk> getListChunks() {
        return listChunks;
    }

    public ArrayList<FileInfo> getFiles() {
        return files;
    }

    
    public Chunk getChunkByNo(int chunkNo, String fileID) {

        Chunk chunk = null;

        for (Chunk chunkTmp : listChunks) {
            if (chunkTmp.getChunkNo() == chunkNo && chunkTmp.getFileID().equals(fileID)) {
                return chunkTmp;
            }
        }
        return chunk;
    }

    public void addFile(FileInfo file) {
        files.add(file);
        for (int i = 0; i < filesToDelete.size(); i++) {
            if (filesToDelete.get(i).equals(file.getFileID())){
                filesToDelete.remove(i);
                return;
            }
        }
    }


    public ConcurrentHashMap<String, HashSet<Integer>> getNumberOfTimesChunkWasBackup() {
        return numberOfTimesChunkWasBackup;
    }

    

    public boolean containsFileChunks(String fileID) {
        for (Chunk chunk: this.listChunks) {
            if (chunk.getFileID().equals(fileID)) return true;
        }
        return false;
    }


    public void serializeStorageObject() {
        String path = "Peer" + Peer.getId();
        File file = new File(path);

        if (!file.exists()) {
            file.mkdirs();
        }

        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath() + File.separator + "LocalStorage.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deserializeStorageObject() {
        String path = "Peer" + Peer.getId() + File.separator + "LocalStorage.ser";
        File file = new File(path);

        if (!file.exists()) {
            System.out.println(".ser file does not exist");
            return;
        }

        LocalStorage storage = new LocalStorage();
        try {
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            storage = (LocalStorage) ois.readObject();
            fis.close();
            ois.close();
            Peer.setStorage(storage);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getChunksAlreadySentFromFile(String fileID) {
        int count = 0;
        for (Chunk chunk: chunksAlreadySent) {
            if (chunk.getFileID().equals(fileID)) count++;
        }
        return count;
    }

    public void clearChunksFromRestoreList(String fileID) {
        ArrayList<Chunk> ChunksAlreadySentArray = new ArrayList<Chunk> (chunksAlreadySent);
        for (int i = 0; i < ChunksAlreadySentArray.size(); i++) {
            Chunk chunk = ChunksAlreadySentArray.get(i);
            if (chunk.getFileID().equals(fileID)) {
                ChunksAlreadySentArray.remove(i);
                i--;
            }
        }
        chunksAlreadySent = new HashSet<>(ChunksAlreadySentArray);
    }


    public byte[] retrieveContentFromChunk(String fileID, int chunkNo) {
        //reads the content of a chunk stored in non volatile memory
        Chunk chunk = getChunkByKey(fileID, chunkNo);
        if (chunk == null) return null;
        String path = "Peer" + Peer.getId() + File.separator + fileID + File.separator + fileID + "-" + chunkNo;
        File chunkFile = new File(path);
        byte[] result = new byte[chunk.getContentLength()];

        try {
            int bytesRead = 0;
            FileInputStream fis = new FileInputStream(chunkFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] buffer = new byte[chunk.getContentLength()];
            if ((bytesRead = bis.read(buffer)) <= 0) {
                System.out.println("Error reading chunk " + chunkNo + " file");
            }
            result = Arrays.copyOf(buffer, bytesRead);
            bis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void clearChunkBody(String fileID, int chunkNo) {
        //clears the body of a chunk to free volatile memory
        Chunk chunk = getChunkByKey(fileID, chunkNo);
        if (chunk == null) return;
        chunk.clearBody();
    }

    public boolean containsFile(String fileID) {
        for (FileInfo f1: files) {
            if (f1.getFileID().equals(fileID)) return true;
        }
        return false;
    }

    public int getFileChunksNumber(String fileID) {
        int count = 0;
        for (Map.Entry<String, HashSet<Integer>> entry : numberOfTimesChunkWasBackup.entrySet()) {
            String key = entry.getKey();
            String currentFileID = key.split("-", 2)[0];
            if(currentFileID.equals(fileID)) count++;
        }
        return count;
    }

    public boolean isGettingBackup(Chunk chunk) {
        for (Chunk c: this.chunksGettingBackup) {
            if (c.getFileID().equals(chunk.getFileID()) && c.getChunkNo() == chunk.getChunkNo()) return true;
        }
        return false;
    }

    public boolean addChunkToBackup(Chunk chunk) {
        if (!isGettingBackup(chunk)){
            this.chunksGettingBackup.add(chunk);
            return true;
        }
        return false;
    }

    public void removeChunkFromBackup(Chunk chunk) {
        for (int i = 0; i < chunksGettingBackup.size(); i++) {
            if (chunksGettingBackup.get(i).getFileID().equals(chunk.getFileID()) && chunksGettingBackup.get(i).getChunkNo() == chunk.getChunkNo()) {
                chunksGettingBackup.remove(i);
                return;
            }
        }
    }

    public void setSpaceAvailable(long spaceAvailable) {
        this.spaceAvailable = spaceAvailable;
    }

    public long getSpaceAvailable() {
        return spaceAvailable;
    }

    public synchronized void addChunk(Chunk chunk) {
        for (Chunk listChunk : this.listChunks) {
            if (listChunk.getChunkNo() == chunk.getChunkNo() && listChunk.getFileID().equals(chunk.getFileID()))
                return; //check if chunk is duplicate
        }
        if(spaceAvailable >= chunk.getContentLength()) {
            listChunks.add(chunk);
        }

        spaceAvailable -= chunk.getContentLength();

    }


    public void removeChunk(Chunk chunk) {
        if (!containsChunk(chunk)) return;
        this.decReplicationDegree(chunk, Peer.getId());

        //DELETE CHUNK FROM STORAGE
        for (int i = 0; i < listChunks.size(); i++){
            if (listChunks.get(i).getFileID().equals(chunk.getFileID()) && listChunks.get(i).getChunkNo() == chunk.getChunkNo())
                listChunks.remove(i);
        }

        //DELETE CHUNK FROM FILESYSTEM
        String path = "Peer" + Peer.getId() + "/" + chunk.getFileID() + "/" + chunk.getFileID() + "-" + chunk.getChunkNo();
        File file = new File(path);
        if (!file.delete()) {
            System.out.println("Could not delete existing chunk");
        }

        spaceAvailable += chunk.getContentLength();

        //sends 'REMOVED' message
        String message = Peer.getProtocolVersion() + " REMOVED " + Peer.getId() + " " + chunk.getFileID() + " " + chunk.getChunkNo() + " \r\n\r\n";
        MessageForwarder messageForwarder = new MessageForwarder(new Message(message.getBytes()));
        Peer.getWorkers().execute(messageForwarder);

    }

    public void deleteChunksWithFileID(String fileID) {
        for (Map.Entry<String, HashSet<Integer>> entry : numberOfTimesChunkWasBackup.entrySet()) {
            String currentFileID = entry.getKey().split("-")[0];
            if(currentFileID.equals(fileID)) {
                numberOfTimesChunkWasBackup.remove(entry.getKey());
            }
        }
    }

    public Chunk getChunkByKey(String fileID, int chunkNo) {
        for (Chunk chunk: this.listChunks) {
            if (chunk.getChunkNo() == chunkNo && chunk.getFileID().equals(fileID)) return chunk;
        }

        return null; //CHUNK DOES NOT EXIST
    }


    public void removeFile(String path) {
        for (int i = 0; i < this.getFiles().size(); i++) {
            String normalPath = this.getFiles().get(i).getFile().getPath().replaceAll("\\\\", "/"); //Windows OS uses '\' instead of '/'
            if (normalPath.equals(path)){
                files.remove(i);
            }
        }
    }

    public FileInfo getFileByName(String name) {
        return containsFileName(name);
    }

    public FileInfo containsFileName(String name) {
        for (int i = 0; i < this.getFiles().size(); i++) {
            String normalPath = this.getFiles().get(i).getFile().getPath().replaceAll("\\\\", "/"); //Windows OS uses '\' instead of '/'
            if (normalPath.equals(name)){
                return this.getFiles().get(i);
            }
        }
        return null;
    }

    public boolean containsChunk(Chunk chunk){
        for (Chunk listChunk : this.listChunks) {
            if (listChunk.getFileID().trim().equals(chunk.getFileID().trim()) && listChunk.getChunkNo() == chunk.getChunkNo()) return true;
        }
        return false;
    }

    private int getChunkSpace() {
        int result = 0;
        for (Chunk chunk: this.listChunks) {
            result += chunk.getContentLength();
        }
        return result;
    }

    public void reclaimingStorageService(long newSpaceAvailable) { //RECLAIM SUB-PROTOCOL
        int chunkSpace = getChunkSpace();
        setSpaceAvailable(0);

        //START WITH CHUNKS STORED WITH EXCESSIVE REPLICATION DEGREE
        listChunks.sort(Comparator.comparing(c -> getPerceivedRepDegree(c) - c.getReplicationDegree()));

        while(chunkSpace > newSpaceAvailable) {
            Chunk chunkToRemove = listChunks.get(listChunks.size()-1);
            chunkSpace -= listChunks.get(listChunks.size()-1).getContentLength();
            listChunks.remove(listChunks.size()-1);
            Peer.getStorage().decReplicationDegree(chunkToRemove, Peer.getId());

            //REMOVE CHUNK FILE
            String path = "Peer" + Peer.getId() + "/" + chunkToRemove.getFileID() + "/" +   chunkToRemove.getFileID() + "-" + chunkToRemove.getChunkNo();
            File file = new File(path);
            if (!file.delete()){
                System.out.println("Could not delete file");
                continue;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String message = Peer.getProtocolVersion() + " REMOVED " + Peer.getId() + " " + chunkToRemove.getFileID() + " " + chunkToRemove.getChunkNo() + " \r\n\r\n";
            MessageForwarder messageForwarder = new MessageForwarder(new Message(message.getBytes()));
            Peer.getWorkers().execute(messageForwarder);
        }
        setSpaceAvailable(newSpaceAvailable - getChunkSpace());
    }

    public void deleteRedundantChunks() {
        //the peer may try to free some space by evicting chunks whose actual replication degree is higher than the desired replication degree.

        for (Chunk chunk: this.listChunks) {
            int perceivedRepDegree = getPerceivedRepDegree(chunk);
            if (perceivedRepDegree > chunk.getReplicationDegree()) {
                removeChunk(chunk);
            }
        }

    }

    public synchronized void incReplicationDegree(Chunk chunk, int PeerID) {
        //increments replication degree once a STORED message is received
        String key = chunk.getFileID() + "-" + chunk.getChunkNo();
        if (!numberOfTimesChunkWasBackup.containsKey(key)) {
            HashSet <Integer> set = new HashSet<>();
            set.add(PeerID);
            numberOfTimesChunkWasBackup.put(key, set);
            return;
        }

        for (Map.Entry<String, HashSet<Integer>> entry : numberOfTimesChunkWasBackup.entrySet()) {

            if(entry.getKey().equals(key)) {
                HashSet <Integer> set = numberOfTimesChunkWasBackup.get(key);
                set.add(PeerID);
                numberOfTimesChunkWasBackup.put(key, set);
                return;
            }
        }
    }

    public synchronized void decReplicationDegree(Chunk chunk, int PeerID) {
        //decrements replication degree once a STORED message is received
        String key = chunk.getFileID() + "-" + chunk.getChunkNo();
        if (!numberOfTimesChunkWasBackup.containsKey(key)) {
            return;
        }
        for (Map.Entry<String, HashSet<Integer>> entry : numberOfTimesChunkWasBackup.entrySet()) {
            if(entry.getKey().equals(key)) {
                HashSet <Integer> set = numberOfTimesChunkWasBackup.get(key);
                set.remove(PeerID);
                numberOfTimesChunkWasBackup.put(entry.getKey(), set);
                return;
            }
        }
    }

    public int getPerceivedRepDegree(Chunk chunk) {
        String key = chunk.getFileID() + "-" + chunk.getChunkNo();
        for (Map.Entry<String, HashSet<Integer>> entry : numberOfTimesChunkWasBackup.entrySet()) {
            if(entry.getKey().equals(key)) {
                return numberOfTimesChunkWasBackup.get(key).size();
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Files backed up:\n");
        for (int i = 0; i < Peer.getStorage().getFiles().size(); i++) {
            result.append("Pathname: \n");
            FileInfo fileInfo = Peer.getStorage().getFiles().get(i);
            result.append(fileInfo.getFile().getPath()).append("\n\t");

            result.append("File ID: \n");
            result.append(fileInfo.getFileID()).append("\n\t");

            result.append("Desired replication degree: \n\t");
            result.append(fileInfo.getDesiredReplicationDeg()).append("\n");


            result.append("Chunks: \n");
            for (int j = 0; j < fileInfo.getChunks().size(); j++) {
                Chunk chunk = fileInfo.getChunks().get(j);
                String key = chunk.getFileID() + "-" + chunk.getChunkNo();
                result.append("\tchunk id: ").append(key).append("\n");

                result.append("\tchunk perceived replication degree: ").append(Peer.getStorage().getNumberOfTimesChunkWasBackup().get(key).toString()).append("\n");
            }
        }

        result.append("Chunks backed up:\n");
        for (int i = 0; i < this.getListChunks().size(); i++){
            Chunk chunk = this.getListChunks().get(i);
            result.append("\tchunk id: ").append(chunk.getFileID()).append("-").append(chunk.getChunkNo()).append("\n");
            result.append("\tchunk size: ").append(chunk.getContentLength()).append("\n");
            result.append("\tdesired replication degree: ").append(chunk.getReplicationDegree()).append("\n");
            String key = chunk.getFileID() + "-" + chunk.getChunkNo();
            result.append("\tchunk perceived replication degree: ").append(Peer.getStorage().getNumberOfTimesChunkWasBackup().get(key).toString()).append("\n");
        }

        result.append("Peer storage capacity: ").append(this.spaceAvailable).append("\n");
        return result.toString();
    }
}

