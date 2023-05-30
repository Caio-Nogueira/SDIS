public class MessageForwarder implements  Runnable{
    private Message message;

    public MessageForwarder(Message message){
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void run() {
        switch (this.message.getSubProtocol()){
            case PUTCHUNK:
                PutChunkTask putChunkTask = new PutChunkTask(this);
                putChunkTask.run();
                break;
            case STORED:
                StoredTask storedTask = new StoredTask(this);
                storedTask.run();
                break;
            case GETCHUNK:
                GetChunkTask getChunkTask = new GetChunkTask(this);
                getChunkTask.run();
                break;
            case CHUNK:
                ChunkTask chunkTask = new ChunkTask(this);
                chunkTask.run();
                break;

            case DELETE:
                DeleteTask deleteTask = new DeleteTask(this);
                deleteTask.run();
                break;

            case REMOVED:
                RemovedTask removedTask = new RemovedTask(this);
                removedTask.run();
                break;

            case ACTIVE:
                ActiveTask activeTask = new ActiveTask(this);
                activeTask.run();
                break;

            default: break;
        }
    }
}
