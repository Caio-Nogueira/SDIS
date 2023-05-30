import java.io.File;

public class DeleteMessageManager implements Runnable{
    private int senderID;
    private String fileID;


    public DeleteMessageManager(byte[] message){
        /* Message format
            <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
         */
        String msg = new String(message);
        String deleteMessage = msg.split("\r\n\r\n", 2)[0];

        String[] splitMsg = deleteMessage.split(" ");
        this.senderID = Integer.parseInt(splitMsg[2]);
        this.fileID = splitMsg[3].trim();
    }

    public void deleteChunks() {
        String filePath = "";
        Peer.getStorage().deleteChunksWithFileID(this.fileID);
        int sizeChunks = Peer.getStorage().getListChunks().size();
        for (int i = 0; i < sizeChunks; i++) {
            if (Peer.getStorage().getListChunks().get(i).getFileID().equals(this.fileID)) {

                Chunk chunk = Peer.getStorage().getListChunks().get(i);
                filePath = "Peer" + Peer.getId() + File.separator + chunk.getFileID() + File.separator + chunk.getFileID() + "-" + chunk.getChunkNo();
                File file = new File(filePath);
                if (!file.getParentFile().exists()) {
                    System.out.println("Chunks already erased");
                    return;
                }
                if (!file.delete()){
                    System.out.println("Could not delete chunk: no= " + chunk.getChunkNo());
                    //continue;
                }
                long spaceAvailable = Peer.getStorage().getSpaceAvailable();
                spaceAvailable += chunk.getContentLength();
                Peer.getStorage().setSpaceAvailable(spaceAvailable);
                Peer.getStorage().getListChunks().remove(i);
                i--;
                sizeChunks--;
            }
        }
        System.out.println("All chunks deleted. Deleting parent file...");
        deleteFileDirectory();

    }

    public void deleteFileDirectory() {
        String filePath = "Peer" + Peer.getId() + File.separator + this.fileID;
        File file = new File(filePath);
        file.delete();
    }

    @Override
    public void run() {

        if (senderID != Peer.getId()){
            if (!Peer.getStorage().containsFileChunks(fileID)) {
                return;
            }
            deleteChunks();
        }
    }
}
