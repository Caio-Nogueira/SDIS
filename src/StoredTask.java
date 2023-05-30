import java.io.IOException;
import java.util.concurrent.Callable;

public class StoredTask implements Runnable {
    private final MessageForwarder messageForwarder;

    public StoredTask(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
    }

    @Override
    public void run()  {

        try {
            Peer.getControlMC().sendMessage(messageForwarder.getMessage().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
