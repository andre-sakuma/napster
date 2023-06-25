package NapsterService;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class NapsterImpl extends UnicastRemoteObject implements NapsterService {
    /**
     * Classe para representar um peer registrado no Servidor Napster.
     */
    public static class NapsterPeer {
        private String ip;
        private int port;
        private ArrayList<String> filenames;

        public NapsterPeer(String ip, int port, String[] filenames) {
            this.ip = ip;
            this.port = port;
            this.filenames = new ArrayList<>(Arrays.asList(filenames));
        }

        public String getIp() {
            return this.ip;
        }

        public int getPort() {
            return this.port;
        }

        public String[] getFilenames() {
            return this.filenames.toArray(new String[this.filenames.size()]);
        }

        public boolean hasFile(String filename) {
            for (String file : this.filenames) {
                if (file.equals(filename)) {
                    return true;
                }
            }

            return false;
        }

        public void addFile(String filename) {
            this.filenames.add(filename);
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * HashMap para armazenar os peers registrados no Servidor Napster.
     *
     * A chave é composta por <ip>:<port>.
     */
    private HashMap<String, NapsterPeer> peers;

    /**
     * HashMap para a busca de arquivos, retornando uma lista de peers que possuem.
     *
     * A chave é o nome do arquivo.
     */
    private HashMap<String, ArrayList<NapsterPeer>> fileToPeers;

    public NapsterImpl() throws java.rmi.RemoteException {
        super();

        this.peers = new HashMap<>();
        this.fileToPeers = new HashMap<>();
    }

    /**
     * Seria a representação da requisição SEARCH.
     *
     * @param ip   IP do peer
     * @param port Porta do peer
     * @return retorna SEARCH_OK com uma lista de peers que possuem o arquivo buscado como o primeiro argumento.
     */
    public NapsterMessage search(String filename, String ip, int port) throws java.rmi.RemoteException {
        System.out.println("Peer " + ip + ":" + port + " solicitou o arquivo " + filename);

        if (!this.fileToPeers.containsKey(filename)) {
            return new NapsterMessage("SEARCH", "SEARCH_OK", new String[] {""});
        }

        ArrayList<NapsterPeer> peers = this.fileToPeers.get(filename);
        String peersString = "";

        for (int i = 0; i < peers.size(); i++) {
            peersString += peers.get(i).getIp() + ":" + peers.get(i).getPort() + "\n";
        }

        return new NapsterMessage("SEARCH", "SEARCH_OK", new String[] {peersString});
    }

    /**
     * Seria a representação da requisição JOIN.
     *
     * @param filenames Lista de arquivos que o peer possui
     * @param ip        IP do peer
     * @param port      Porta do peer
     * @return retorna JOIN_OK caso o peer tenha sido registrado corretamente.
     */
    public NapsterMessage join(String[] filenames, String ip, int port) throws java.rmi.RemoteException {
        NapsterPeer peer = new NapsterPeer(ip, port, filenames);

        for (String filename : filenames) {
            // Se o arquivo não existe ainda, cria uma nova lista
            if (!this.fileToPeers.containsKey(filename)) {
                this.fileToPeers.put(filename, new ArrayList<NapsterPeer>());
            }

            // Adiciona o peer na lista de peers que possuem o arquivo
            this.fileToPeers.get(filename).add(peer);
        }

        // Adiciona o peer no HashMap de peers
        this.peers.put(ip + ":" + port, peer);

        System.out.println("Peer " + ip + ":" + port + " adicionado com arquivos " + Arrays.toString(filenames));

        return new NapsterMessage("JOIN", "JOIN_OK", new String[] { "New peer registred!" } );
    }

    /**
     * Seria a representação da requisição UPDATE.
     *
     * @param filename Nome do arquivo que o peer deseja adicionar
     * @param ip       IP do peer
     * @param port     Porta do peer
     * @return retorna UPDATE_OK caso o peer tenha sido atualizado corretamente.
     */
    public NapsterMessage update(String filename, String ip, int port) throws java.rmi.RemoteException {
        if (!this.peers.containsKey(ip + ":" + port)) {
            return new NapsterMessage("UPDATE", "UPDATE_ERROR", new String[] { "Peer not found" } );
        }

        NapsterPeer peer = this.peers.get(ip + ":" + port);

        if (peer.hasFile(filename)) {
            return new NapsterMessage("UPDATE", "UPDATE_ERROR", new String[] { "Peer already has file" } );
        }

        if (!this.fileToPeers.containsKey(filename)) {
            this.fileToPeers.put(filename, new ArrayList<NapsterPeer>());
        }

        this.fileToPeers.get(filename).add(this.peers.get(ip + ":" + port));


        return new NapsterMessage("UPDATE", "UPDATE_OK", new String[] { "" } );
    }
}
