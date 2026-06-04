import gui.MainFrame;
import manager.ClientController;
import network.TCPClient;
import javax.swing.*;

public class MainClient {
    public static void main(String[] args) {
        String host = "localhost";
        // String host = "helios.cs.ifmo.ru";
        int port = 9999;

        TCPClient tcpClient = new TCPClient(host, port);
        ClientController controller = new ClientController(tcpClient);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(controller);
            mainFrame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tcpClient.close();
            System.out.println("Работа клиента завершена.");
        }));
    }
}