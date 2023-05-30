import java.io.IOException;

public class ActiveTask implements Runnable{
    private MessageForwarder messageForwarder;

    public ActiveTask(MessageForwarder messageForwarder) {
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
