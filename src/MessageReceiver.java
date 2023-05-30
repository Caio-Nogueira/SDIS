public class MessageReceiver implements Runnable{
    private Message message;

    public MessageReceiver(Message message){
        this.message = message;
    }

    @Override
    public void run() {
        switch (this.message.getSubProtocol()){

            case PUTCHUNK, STORED:
                // if Operation is PUTCHUNK or STORED, message is sent to backupManager thread
                BackupMessageManager backupMessageManager = new BackupMessageManager(this.message.getMessage());
                backupMessageManager.run();
                break;
            case GETCHUNK, CHUNK:
                RestoreMessageManager restoreMessageManager = new RestoreMessageManager(this.message.getMessage());
                restoreMessageManager.run();
                break;

            case DELETE:
                DeleteMessageManager deleteMessageManager = new DeleteMessageManager(this.message.getMessage());
                deleteMessageManager.run();
                break;

            case REMOVED:
                RemovedMessageManager removedMessageManager = new RemovedMessageManager(this.message.getMessage());
                removedMessageManager.run();
                break;

            case ACTIVE:
                ActiveMessageManager activeMessageManager = new ActiveMessageManager(this.message.getMessage());
                activeMessageManager.run();
                break;

            default: break;
        }
    }
}
