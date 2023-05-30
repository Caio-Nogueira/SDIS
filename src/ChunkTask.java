import java.io.IOException;

public class ChunkTask implements Runnable{

    MessageForwarder messageForwarder;

    public ChunkTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
    }

    @Override
    public void run() {

        try {
            Peer.getRestoreMC().sendMessage(messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
