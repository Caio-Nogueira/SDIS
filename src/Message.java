import java.nio.charset.StandardCharsets;

enum SubProtocol{
    PUTCHUNK,
    STORED,
    GETCHUNK,
    CHUNK,
    DELETE,
    REMOVED,
    ACTIVE
};

public class Message {
    private String header;
    private byte[] body;
    private final byte[] message;
    private SubProtocol subProtocol;

    public Message(byte[] message){
        this.message = message;
        parseMessage();
        parseOperation();
    }

    public byte[] getBody() {
        return body;
    }

    public byte[] getMessage() {
        return message;
    }

    public String getHeader() {
        return header;
    }

    public SubProtocol getSubProtocol() {
        return subProtocol;
    }

    public void parseMessage(){
        String msg = new String(this.message);
        String[] splitMsg = msg.split("\r\n\r\n", 2);
        this.header = splitMsg[0].trim();

        this.body = new byte[this.message.length - this.header.length() - 5];
        System.arraycopy(this.message, this.header.length() + 5, this.body, 0, this.body.length);
    }

    private void parseOperation() {
        String operation = this.header.trim().split(" ")[1];
        //interprets message content
        switch(operation) {
            case "PUTCHUNK":
                this.subProtocol = SubProtocol.PUTCHUNK;
                break;
            case "STORED":
                this.subProtocol = SubProtocol.STORED;
                break;
            case "GETCHUNK":
                this.subProtocol = SubProtocol.GETCHUNK;
                break;
            case "CHUNK":
                this.subProtocol = SubProtocol.CHUNK;
                break;
            case "DELETE":
                this.subProtocol = SubProtocol.DELETE;
                break;

            case "REMOVED":
                this.subProtocol = SubProtocol.REMOVED;
                break;

            case "ACTIVE":
                this.subProtocol = SubProtocol.ACTIVE;
                break;

            default:
                System.out.println("Invalid message\nDiscarding...");
                break;
        }
    }
}
