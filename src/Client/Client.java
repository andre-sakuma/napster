package Client;

import NapsterService.NapsterService;
import NapsterService.NapsterMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Classe que representa um peer.
 */
public class Client {
    /**
     * Classe auxiliar para lidar com as informações de um próprio peer.
     */
    public static class ClientPeer {
        int port;
        String ip;
        private String directory;
        private ArrayList<String> filenames;

        public ClientPeer(String directory, String ip, int port) {
            this.directory = directory;
            this.filenames = this.fetchFilenames();
            this.ip = ip;
            this.port = port;
        }

        public String getDirectory() {
            return this.directory;
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
            return this.filenames.contains(filename);
        }

        public void addFile(String filename) {
            this.filenames.add(filename);
        }

        public File getFile(String filename) {
            return new File(this.directory + "/" + filename);
        }

        private ArrayList<String> fetchFilenames() {
            File dir = new File(this.directory);
            if (!dir.exists()) {
                dir.mkdirs();
                return new ArrayList<>();
            }
            File[] files = dir.listFiles();

            ArrayList<String> filenames = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                filenames.add(files[i].getName());
            }

            return filenames;
        }
    }

    /**
     * Classe para representar uma thread que lida com o download de um arquivo.
     */
    public static class HandleDownloadThread extends Thread {
        private Socket client;
        private ClientPeer clientPeer;
        public HandleDownloadThread(Socket client, ClientPeer clientPeer) {
            super();
            this.client = client;
            this.clientPeer = clientPeer;
        }
        public void run() {
            try {
                InputStream input = this.client.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead = input.read(buffer);
                String filename = new String(buffer, 0, bytesRead);

                File file = this.clientPeer.getFile(filename);
                if (!file.exists()) {
                    throw new Exception("Esse arquivo não existe.");
                }
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream output = this.client.getOutputStream();

                // Envia o arquivo em blocos de 1024 bytes.
                byte[] fileBuffer = new byte[1024];
                int fileBytesRead;
                while ((fileBytesRead = fileInputStream.read(fileBuffer)) != -1) {
                    output.write(fileBuffer, 0, fileBytesRead);
                }

                fileInputStream.close();
                output.close();
                this.client.close();
            } catch (Exception e) {
                System.out.println("Falha ao realizar download: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String ip;
        int port;
        String directory;

        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite o ip:");
        ip = scanner.nextLine();
        System.out.println("Digite a porta do peer:");
        port = Integer.parseInt(scanner.nextLine());
        System.out.println("Digite o diretório de arquivos:");
        directory = scanner.nextLine();

        // Inicializa a Classe que vai conter o ip, porta, nome do peer e vai fazer o gerenciamento dos arquivos
        ClientPeer client = new ClientPeer(directory, ip, port);

        // Thread que vai executar a interface do peer
        Thread interfaceThread = new Thread(interfaceThread(client));

        // Thread que vai executar o servidor do peer (que vai lidar as requisições de download de outros peers)
        Thread serverThread = new Thread(serverThread(client));

        interfaceThread.start();
        serverThread.start();
    }

    /**
    * DOWNLOAD
    */
    private static void download(NapsterService napster, ClientPeer client, String[] args) {
        String filename;
        String ip;
        int port;

        // Verifica se o comando foi digitado corretamente
        try {
            filename = args[1];
            ip = args[2];
            port = Integer.parseInt(args[3]);
        } catch (Exception e) {
            System.out.println("Erro de sintaxe.");
            System.out.println("Tem certeza que você digitou tudo certo?");
            System.out.println("O comando DOWNLOAD precisa seguir o segunite formato: DOWNLOAD <filename> <ip> <port>");
            return;
        }

        try {
            // Cria um socket para se conectar com o outro peer
            Socket socket = new Socket(ip, port);

            // Envia o nome do arquivo que queremos baixar para outro peer
            OutputStream output = socket.getOutputStream();
            output.write(filename.getBytes());
            output.flush();

            // Recebe o arquivo do outro peer e escreve no disco
            File file = client.getFile(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            InputStream input = socket.getInputStream();

            // divide o arquivo em blocos de 1024 bytes para lidar com arquivos grandes
            byte[] buffer = new byte[1024];
            int bytesRead;

            // enquanto tiver bytes para ler, escreve na stream de saída
            while ((bytesRead = input.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.close();
            input.close();
            socket.close();
            System.out.println("Arquivo " + filename + " baixado com sucesso na pasta " + client.getDirectory());

            // faz a requisição de UPDATE para o servidor, atualizando a sua lista de arquivos
            update(napster, client, filename);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * UPDATE
     */
    private static void update(NapsterService napster, ClientPeer client, String filename) {
        String ip = client.getIp();
        int port = client.getPort();

        try {
            // Faz a requisição de UPDATE para o servidor
            napster.update(filename, ip, port);
        } catch (RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * JOIN
     */
    private static void join(NapsterService napster, ClientPeer client) {
        // Pega a lista de arquivos do peer
        String[] filenames = client.getFilenames();

        String ip = client.getIp();
        int port = client.getPort();

        try {
            // Faz a requisição de JOIN para o servidor
            NapsterMessage message = napster.join(filenames, ip, port);

            String response = message.getMessage();
            String filenamesString = "";
            for (String filename : filenames) {
                filenamesString += filename + " ";
            }
            if (response.equals("JOIN_OK")) {
                // Se o servidor respondeu com JOIN_OK, então o peer foi adicionado com sucesso
                System.out.println("Sou o peer " + ip + ":" + port + " com os arquivos " + filenamesString);
            } else {
                System.out.println("Error: " + response);
            }
        } catch (RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * SEARCH
     */
    private static void search(NapsterService napster, ClientPeer client, String[] args) {
        String filename;

        // Verifica se o comando foi digitado corretamente
        try {
            filename = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Erro de sintaxe.");
            System.out.println("Tem certeza que você digitou tudo certo?");
            System.out.println("O comando SEARCH precisa seguir o segunite formato: SEARCH <filename>");
            return;
        }

        String ip = client.getIp();
        int port = client.getPort();

        try {
            // Faz a requisição de SEARCH para o servidor
            NapsterMessage message = napster.search(filename, ip, port);

            String response = message.getMessage();
            if (response.equals("SEARCH_OK")) {
                // Se o servidor respondeu com SEARCH_OK, então foi recebido uma lista de peers que possuem o arquivo
                System.out.println("peers com arquivo solicitado:\n" + message.getArgs()[0]);
            } else {
                System.out.println("Error: " + response);
            }
        } catch (RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Comando para listar os comandos disponíveis e como usá-los
     */
    private static void help() {
        System.out.println("Comandos disponíveis:");
        System.out.println("- SEARCH <filename>: busca por um arquivo no sistema");
        System.out.println("- JOIN: se registra no sistema");
        System.out.println("- DOWNLOAD <filename> <ip> <port>: baixa um arquivo de um peer");
        System.out.println("- EXIT: sai do sistema");
        System.out.println("- HELP: mostra os comandos disponíveis");
    }

    /*
     * Thread que fica esperando o usuário digitar os comandos (interface)
     */
    private static Runnable interfaceThread (ClientPeer client) {
        return new Runnable() {
            @Override
            public void run() throws RuntimeException {
                try {
                    // Cria um registro para se conectar com o servidor
                    Registry registry = LocateRegistry.getRegistry();

                    // Pega o objeto remoto do servidor
                    NapsterService napster = (NapsterService) registry.lookup("rmi://127.0.0.1/napster");

                    // Mostra os comandos disponíveis
                    help();

                    while (true) {
                        Scanner scanner = new Scanner(System.in);
                        String line = scanner.nextLine();
                        String[] args = line.split(" ");
                        String command = args[0];

                        switch (command) {
                            case "EXIT":
                                System.out.println("Fechando o cliente.");
                                System.exit(0);
                                break;
                            case "HELP":
                                help();
                                break;
                            case "SEARCH":
                                search(napster, client, args);
                                break;
                            case "JOIN":
                                join(napster, client);
                                break;
                            case "DOWNLOAD":
                                download(napster, client, args);
                                break;
                            default:
                                System.out.println("Comando inválido.");
                                break;
                        }

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /*
     * Thread que fica esperando por conexões de outros peers para download
     */
    private static Runnable serverThread (ClientPeer client) {
        return new Runnable() {
            @Override
            public void run() throws RuntimeException {
                try {
                    System.out.println("O Servidor do peer está pronto.");

                    // Cria um SocketServer para o peer
                    ServerSocket server = new ServerSocket(client.getPort());

                    // Fica esperando por conexões
                    while (true) {
                        Socket clientSocket = server.accept();

                        // Caso receba uma conexão, cria uma thread para tratar o download
                        HandleDownloadThread thread = new HandleDownloadThread(clientSocket, client);
                        thread.start();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
