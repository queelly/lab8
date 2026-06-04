package manager;

import network.TCPClient;
import network.Request;
import network.Response;
import models.builders.RequestBuilder;

public class ClientController {
    private final TCPClient client;
    private String username;
    private String password;

    public ClientController(TCPClient client) {
        this.client = client;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Response sendCommand(String commandName, String[] args) {
        String requestPassword = commandName.equals("login") ? null : this.password;

        Request request = RequestBuilder.createRequest(commandName, args, null, null, username, requestPassword);

        if (request != null && client.sendRequest(request)) {
            return client.receiveResponse();
        }
        return null;
    }
}