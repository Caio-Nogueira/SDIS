import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIRemoteObject extends Remote {
    void backup(String path, int desiredRepDegree) throws RemoteException;

    void restore(String path) throws RemoteException;

    void delete(String path) throws RemoteException;

    void reclaim(long newSpaceAvailable) throws RemoteException;

    void state() throws RemoteException;
}
