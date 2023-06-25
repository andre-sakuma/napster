package NapsterService;

import java.io.Serializable;

/**
 * Classe para representar uma mensagem de resposta do Servidor Napster.
 */
public class NapsterMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String method;
    private String message;
    private String[] args;

    public NapsterMessage(String method, String message, String[] args) {
        this.method = method;
        this.message = message;
        this.args = args;
    }

    public String getMethod() {
        return this.method;
    }

    public String getMessage() {
        return this.message;
    }

    public String[] getArgs() {
        return this.args;
    }
}
