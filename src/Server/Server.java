package Server;

import NapsterService.NapsterService;
import NapsterService.NapsterImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Classe que representa o Servidor Napster.
 */
public class Server {
    public static void main(String[] args) throws Exception {
        int port;
        String ip;

        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite o ip do servidor:");
        ip = scanner.nextLine();
        System.out.println("Digite a porta do servidor:");
        port = scanner.nextInt();

        // Cria o NapsterService
        NapsterService napster = new NapsterImpl();

        // Cria o registro na porta
        LocateRegistry.createRegistry(port);

        // Pega o registro
        Registry registry = LocateRegistry.getRegistry();

        // Vincula o NapsterService no registro
        registry.bind("rmi://" + ip + "/napster", napster);
        System.out.println("O Servidor Napster está pronto.");
    }
}
