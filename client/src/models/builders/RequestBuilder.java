package models.builders;

import manager.PrinterManager;
import manager.ScannerManager;
import models.Coordinates;
import models.Position;
import models.Status;
import models.Worker;
import network.Request;

import java.util.Set;

public class RequestBuilder {

    public static Request createRequest(String cmd, String[] args,
                                        ScannerManager scannerManager,
                                        PrinterManager printerManager,
                                        String username, String password) {
        Worker worker = null;
        Set<String> objectCommands = Set.of("add", "update", "add_if_max", "add_if_min");

        if (objectCommands.contains(cmd)) {
            if (scannerManager == null) {
                try {
                    String name = args[0];
                    float x = Float.parseFloat(args[1]);
                    Double y = Double.parseDouble(args[2]);
                    Double salary = args[3].isEmpty() ? null : Double.parseDouble(args[3]);
                    Position position = args[4].isEmpty() ? null : Position.valueOf(args[4].toUpperCase());
                    Status status = args[5].isEmpty() ? null : Status.valueOf(args[5].toUpperCase());

                    Double turnover = args[6].isEmpty() ? null : Double.parseDouble(args[6]);
                    Integer employees = args[7].isEmpty() ? null : Integer.parseInt(args[7]);
                    String creator = args[8];
                    models.Organization org = new models.Organization(turnover, employees);

                    worker = new Worker(
                            null,
                            name,
                            new Coordinates(x, y),
                            java.time.LocalDateTime.now(),
                            salary,
                            position,
                            status,
                            org
                    );
                    worker.setCreator(creator);

                    args = new String[]{};
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга данных из GUI: " + e.getMessage());
                    return null;
                }
            } else {
                worker = new WorkerBuilder(scannerManager, printerManager).build();
                if (worker == null) {
                    System.err.println("Ошибка при создании Worker. Команда отменена.");
                    return null;
                }
            }
        }

        return new Request(cmd, args, worker, username, password);
    }

    public static Request createRequest(String cmd, String[] args,
                                        String username, String password) {
        return new Request(cmd, args, null, username, password);
    }
}