import java.io.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements RMIRemoteObject {
    private static int id;
    private static String protocolVersion;
    private static String remoteObjectName;
    private static boolean isInitiatorPeer = false;

    private static ScheduledThreadPoolExecutor workers = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(128);

    private static MulticastChannel controlMC; // control multicast channel
    private static MulticastChannel backupMC; // control multicast channel
    private static MulticastChannel restoreMC; // control multicast channel
    private static LocalStorage storage = new LocalStorage();

    public static LocalStorage getStorage() {
        return storage;
    }

    public static int getId() {
        return id;
    }

    public static MulticastChannel getControlMC() {
        return controlMC;
    }

    public static MulticastChannel getRestoreMC() {
        return restoreMC;
    }

    public static MulticastChannel getBackupMC() {
        return backupMC;
    }

    public static String getProtocolVersion() {
        return protocolVersion;
    }

    public static ScheduledThreadPoolExecutor getWorkers() {
        return workers;
    }

    public static void setStorage(LocalStorage storage) {
        Peer.storage = storage;
    }

    public static void main(String[] args) {
        if (args.length != 9) {
            System.out.println("Usage:\tjava Peer <protocolVersion> <peerId> <remoteObjectName> <mcIP> <mcPort> <mdbIp> <mdbPort> <mdrIp> <mdrPort>");
            return;
        }

            /*Uncomment next line to force system to prioritize IPV4 addresses over IPV6 */
        //System.setProperty("java.net.preferIPv4Stack", "true");


        protocolVersion = args[0];
        id = Integer.parseInt(args[1]);
        remoteObjectName = args[2];


        try {
            controlMC = new MulticastChannel(args[3], Integer.parseInt(args[4]));
            backupMC = new MulticastChannel(args[5], Integer.parseInt(args[6]));
            restoreMC = new MulticastChannel(args[7], Integer.parseInt(args[8]));
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        * SEND 'ACTIVE' MESSAGE (PROTOCOL VERSION -> '2.0')
        * MESSAGE FORMAT: 'ProtocolVersion ACTIVE SenderID'
        * */

        if (protocolVersion.equals("2.0")){
            Random random = new Random();
            int delay = random.nextInt(401);
            String helloMessage = protocolVersion + " ACTIVE " + id + " \r\n\r\n";
            MessageForwarder messageForwarder = new MessageForwarder(new Message(helloMessage.getBytes()));
            workers.schedule(messageForwarder, delay, TimeUnit.MILLISECONDS);
        }


        workers.execute(controlMC);
        workers.execute(backupMC);
        workers.execute(restoreMC);

        storage.deserializeStorageObject();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> storage.serializeStorageObject()));

        try {
            Peer obj = new Peer();
            RMIRemoteObject stub = (RMIRemoteObject) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjectName, stub);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void backup(String path, int desiredRepDegree) throws RemoteException {
        File backupFile = new File(path);
        if (!backupFile.exists()) {
            System.out.println("File " + path + " does not exist...");
            return;
        }
        FileInfo file = new FileInfo(path, desiredRepDegree);
        System.out.println("BACKING UP FILE " + file.getFile().getPath());
        for (FileInfo fileInfo: storage.getFiles()) {
            String normalPath = fileInfo.getFile().getPath().replaceAll("\\\\", "/");
            if (normalPath.equals(path)) {
                System.out.println("File " + path + " already backed up.\nExiting...");
                return;
            }
        }
        Peer.getStorage().addFile(file);

        long totalBytesRead = 0;
        int chunkNo = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedInputStream bis = new BufferedInputStream(fis);

        while (totalBytesRead < file.getFile().length()) {
            int bytesRead = 0;
            byte[] buffer = new byte[64000];
            try {
                if ((bytesRead = bis.read(buffer)) <= 0){
                    bis.close();
                    System.out.println("Error reading from file!\nExiting...");
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] body = Arrays.copyOf(buffer, bytesRead);
            Chunk chunk = new Chunk(file.getFileID(), body, chunkNo, desiredRepDegree);
            String header = protocolVersion + " PUTCHUNK " + id + " " + file.getFileID() + " " + chunk.getChunkNo() + " " + desiredRepDegree + " \r\n\r\n";
            byte[] asciiHeader = header.getBytes(StandardCharsets.US_ASCII);
            byte[] putChunkMessage = new byte[asciiHeader.length + body.length];
            System.arraycopy(asciiHeader, 0, putChunkMessage, 0, asciiHeader.length);
            System.arraycopy(body, 0, putChunkMessage, asciiHeader.length, body.length);

            MessageForwarder messageForwarder = new MessageForwarder(new Message(putChunkMessage));
            workers.execute(messageForwarder);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            file.addChunkToFile(chunk);
            totalBytesRead += bytesRead;
            chunkNo++;
        }

        try {
            bis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(file.getFile().length() % 64000 == 0){
            byte[] new_body = new byte[0];
            Chunk lastChunk = new Chunk(file.getFileID(), new_body, chunkNo,0);
            String header = protocolVersion + " PUTCHUNK " + id + " " + file.getFileID() + " " + chunkNo + " " + desiredRepDegree + " \r\n\r\n";
            byte[] asciiHeader = header.getBytes(StandardCharsets.US_ASCII);
            byte[] putChunkMessage = new byte[asciiHeader.length];
            System.arraycopy(asciiHeader, 0, putChunkMessage, 0, asciiHeader.length);
            file.addChunkToFile(lastChunk);
            MessageForwarder messageForwarder = new MessageForwarder(new Message(putChunkMessage));
            workers.execute(messageForwarder);
        }

    }

    @Override
    public void restore(String filePath) throws RemoteException {
        isInitiatorPeer = true;

        for (int i = 0; i < storage.getFiles().size(); i++) {
            String normalPath = storage.getFiles().get(i).getFile().getPath().replaceAll("\\\\", "/");
            if (normalPath.equals(filePath)) {
                System.out.println("Restoring file " + filePath);
                FileInfo fileInfo = storage.getFiles().get(i);
                Peer.getStorage().clearChunksFromRestoreList(fileInfo.getFileID());
                for (int j = 0; j < storage.getFiles().get(i).getChunks().size(); j++) {
                    Chunk chunk = storage.getFiles().get(i).getChunks().get(j);
                    String message = protocolVersion + " GETCHUNK " + id + " " + fileInfo.getFileID() + " " + chunk.getChunkNo() + " \r\n\r\n";
                    MessageForwarder messageForwarder = new MessageForwarder(new Message(message.getBytes()));
                    try {
                        workers.execute(messageForwarder);
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        isInitiatorPeer = false;
    }

    @Override
    public void delete(String path) throws RemoteException {
        FileInfo file = Peer.getStorage().getFileByName(path);
        if (file == null){
            System.out.println("PEER" + Peer.getId() + " does not have file " + path);
            return;
        }
        storage.removeFile(path);
        String s = Peer.getProtocolVersion() + " DELETE " + Peer.getId() +  " " + file.getFileID() + " \r\n\r\n";
        MessageForwarder messageForwarder = new MessageForwarder(new Message(s.getBytes()));
        workers.execute(messageForwarder);
    }

    @Override
    public void reclaim(long newSpaceAvailable) throws RemoteException {
        getStorage().reclaimingStorageService(newSpaceAvailable);
    }

    @Override
    public void state() throws RemoteException {
        System.out.println(Peer.getStorage());
    }
}