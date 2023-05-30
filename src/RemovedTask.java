import java.io.IOException;

public class RemovedTask implements Runnable{
    private MessageForwarder messageForwarder;

    public RemovedTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
    }

    @Override
    public void run() {
        try {
            Peer.getControlMC().sendMessage(this.messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
