import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

public class DeleteTask implements Runnable{
    private final MessageForwarder messageForwarder;
    private String fileID;
    private String protocolVersion;

    public DeleteTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
        String msg = new String(messageForwarder.getMessage().getMessage());
        String deleteMessage = msg.split("\r\n\r\n", 2)[0];

        String[] splitMsg = deleteMessage.split(" ");
        this.fileID = splitMsg[3].trim();
        this.protocolVersion = splitMsg[0];
    }

    public void deleteFromInitiatorPeer() {
        //clears information about a file in initiator peer
        for (Map.Entry<String, HashSet<Integer>> entry : Peer.getStorage().getNumberOfTimesChunkWasBackup().entrySet()) {
            String key = entry.getKey();
            String fileID = key.split("-")[0];
            if (fileID.equals(this.fileID)) {
                Peer.getStorage().getNumberOfTimesChunkWasBackup().remove(entry.getKey());
            }
        }

        for (int i = 0; i < Peer.getStorage().getFiles().size(); i++) {
            if (Peer.getStorage().getFiles().get(i).getFileID().equals(this.fileID)) {
                Peer.getStorage().getFiles().remove(i);
                return;
            }
        }
    }

    @Override
    public void run() {
        try {
            Peer.getControlMC().sendMessage(this.messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Peer.getStorage().getFilesToDelete().contains(this.fileID) && this.protocolVersion.equals("2.0"))
            Peer.getStorage().getFilesToDelete().add(this.fileID);
        deleteFromInitiatorPeer();
    }
}
