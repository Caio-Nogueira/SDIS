import java.io.IOException;

public class GetChunkTask implements Runnable {

    private MessageForwarder messageForwarder;

    public GetChunkTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
    }

    @Override
    public void run() {

        try {
            Peer.getControlMC().sendMessage(messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
