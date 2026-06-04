package commands;

import auth.UserManager;
import database.WorkerDatabaseManager;
import manager.CollectionManager;
import models.Worker;
import network.Response;

public class ShowCommand implements Executable {

    private CollectionManager collection;

    public ShowCommand(CollectionManager collection) {
        this.collection = collection;
    }

    @Override
    public Response execute(String[] args, Worker worker, String username, WorkerDatabaseManager workerDatabaseManager,
                            UserManager userManager) {
        if (username == null || username.isEmpty()) {
            return new Response("Ошибка: Пользователь не авторизован!", false);
        }

        if (args.length != 0) {
            return new Response("Command does not accept args!", false);
        }

        String message = collection.getCollectionAsString();
        java.util.ArrayDeque<Worker> workers = collection.getCollection();

        return new Response(message, workers, true);
    }

    @Override
    public String toString() {
        return ": show all Collection's elements";
    }
}