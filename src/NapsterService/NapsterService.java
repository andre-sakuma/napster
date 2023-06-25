package NapsterService;

import java.rmi.Remote;

public interface NapsterService extends Remote {
    public NapsterMessage search(String filename, String ip, int port) throws java.rmi.RemoteException;
    public NapsterMessage join(String[] filenames, String ip, int port) throws java.rmi.RemoteException;
    public NapsterMessage update(String filename, String ip, int port) throws java.rmi.RemoteException;
}
