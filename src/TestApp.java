import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage:\tjava TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
        }


        String host = "localhost";  //assume hostname is "localhost"
        String RemoteObjectName = args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            RMIRemoteObject RMIObject = (RMIRemoteObject) registry.lookup(RemoteObjectName);

            String subProtocol = args[1];
            String filePath;

            switch (subProtocol){
                case "BACKUP":
                    filePath = args[2];
                    int replicationDegree = Integer.parseInt(args[3]);
                    RMIObject.backup(filePath, replicationDegree);
                    break;
                case "RESTORE":
                    filePath = args[2];
                    RMIObject.restore(filePath);
                    break;

                case "STATE":
                    RMIObject.state();
                    break;

                case "DELETE":
                    filePath = args[2];
                    RMIObject.delete(filePath);
                    break;

                case "RECLAIM":
                    long newSpaceAvailable = Long.parseLong(args[2]);
                    RMIObject.reclaim(newSpaceAvailable);
                    break;
                default: break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
