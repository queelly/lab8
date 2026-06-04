package gui;

import manager.ClientController;
import network.Response;
import models.Worker;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

public class MainAppPanel extends JPanel {
    private final ClientController controller;
    private final String currentUser;
    private final LocaleManager lm = LocaleManager.getInstance();

    private JTable table;
    private WorkerTableModel tableModel;
    private WorkerCanvas canvas;

    private JLabel filterLabel;
    private JLabel userLabel;
    private JButton filterButton, refreshBtn, addBtn, editBtn, removeBtn;

    private int selectedWorkerId = -1;
    private boolean isRefreshing = false;
    private JTabbedPane tabbedPane;
    private JTextField filterField;
    private JComboBox<String> langCombo;

    private final PropertyChangeListener localeListener = evt -> applyLocale();

    public MainAppPanel(ClientController controller, String currentUser) {
        this.controller = controller;
        this.currentUser = currentUser;
        lm.addChangeListener(localeListener);
        setLayout(new BorderLayout());
        initComponents();
        startAutoRefresh();
        refreshData();
    }

    private void initComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout());

        userLabel = new JLabel(lm.get("label.user") + " " + currentUser);
        userLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        headerPanel.add(userLabel, BorderLayout.WEST);

        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        langPanel.add(new JLabel(lm.get("label.language")));
        langCombo = new JComboBox<>(LocaleManager.LOCALE_LABELS);
        for (int i = 0; i < LocaleManager.SUPPORTED_LOCALES.length; i++) {
            if (LocaleManager.SUPPORTED_LOCALES[i].equals(lm.getCurrentLocale())) {
                langCombo.setSelectedIndex(i);
                break;
            }
        }
        langCombo.addActionListener(e -> lm.setLocale(LocaleManager.SUPPORTED_LOCALES[
                langCombo.getSelectedIndex()]));
        langPanel.add(langCombo);
        headerPanel.add(langPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();

        JPanel tableContainer = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterLabel = new JLabel(lm.get("label.filter"));
        filterField = new JTextField(15);
        filterButton = new JButton(lm.get("btn.filter"));
        filterButton.addActionListener(e -> tableModel.filterByName(
                filterField.getText()));
        topPanel.add(filterLabel);
        topPanel.add(filterField);
        topPanel.add(filterButton);
        tableContainer.add(topPanel, BorderLayout.NORTH);

        tableModel = new WorkerTableModel();
        table = new JTable(tableModel);
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            private boolean ascending = true;
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                tableModel.sort(col, ascending);
                ascending = !ascending;
            }
        });
        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn = new JButton(lm.get("btn.refresh"));
        addBtn     = new JButton(lm.get("btn.add"));
        editBtn    = new JButton(lm.get("btn.edit"));
        removeBtn  = new JButton(lm.get("btn.remove"));
        editBtn.setEnabled(false);
        removeBtn.setEnabled(false);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            if (isRefreshing) return;
            int row = table.getSelectedRow();
            if (row == -1) {
                selectedWorkerId = -1;
                editBtn.setEnabled(false);
                removeBtn.setEnabled(false);
            } else {
                Worker w = tableModel.getWorkerAt(row);
                selectedWorkerId = Math.toIntExact(w.getId());
                boolean isOwner = currentUser.equals(w.getCreator());
                editBtn.setEnabled(isOwner);
                removeBtn.setEnabled(isOwner);
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout());

        refreshBtn.addActionListener(e -> refreshData());

        addBtn.addActionListener(e -> {
            WorkerEditDialog dialog = new WorkerEditDialog(
                    SwingUtilities.getWindowAncestor(this),
                    controller, null, currentUser);
            dialog.setVisible(true);
            refreshData();
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            Worker worker = tableModel.getWorkerAt(row);
            showEditDialog(worker);
        });

        removeBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, lm.get("msg.error.select"));
                return;
            }
            Worker worker = tableModel.getWorkerAt(selectedRow);
            sendCommandAsync("remove_by_id",
                    new String[]{String.valueOf(worker.getId())}, true);
        });

        bottomPanel.add(refreshBtn);
        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(removeBtn);
        tableContainer.add(bottomPanel, BorderLayout.SOUTH);

        tabbedPane.addTab(lm.get("tab.table"), tableContainer);

        canvas = new WorkerCanvas();
        tabbedPane.addTab(lm.get("tab.visual"), canvas);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void applyLocale() {
        filterLabel.setText(lm.get("label.filter"));
        filterButton.setText(lm.get("btn.filter"));
        refreshBtn.setText(lm.get("btn.refresh"));
        addBtn.setText(lm.get("btn.add"));
        editBtn.setText(lm.get("btn.edit"));
        removeBtn.setText(lm.get("btn.remove"));
        userLabel.setText(lm.get("label.user") + " " + currentUser);
        tabbedPane.setTitleAt(0, lm.get("tab.table"));
        tabbedPane.setTitleAt(1, lm.get("tab.visual"));
        tableModel.updateColumnNames(); // обновляем заголовки колонок
        revalidate();
        repaint();
    }

    private void startAutoRefresh() {
        new Timer(5000, e -> refreshData()).start();
    }

    private void refreshData() {
        new Thread(() -> {
            Response response = controller.sendCommand("show", new String[]{});
            SwingUtilities.invokeLater(() -> {
                if (response != null && response.isSuccess()) {
                    java.util.ArrayDeque<Worker> workers = response.getCollection();
                    if (workers != null) {
                        isRefreshing = true;
                        tableModel.setData(new java.util.ArrayList<>(workers));
                        canvas.setWorkers(new java.util.ArrayList<>(workers));
                        // Восстанавливаем выделение по сохранённому ID
                        if (selectedWorkerId != -1) {
                            int row = tableModel.findRowById(selectedWorkerId);
                            if (row != -1) {
                                table.setRowSelectionInterval(row, row);
                                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                            } else {
                                selectedWorkerId = -1;
                                editBtn.setEnabled(false);
                                removeBtn.setEnabled(false);
                            }
                        }
                        isRefreshing = false;
                    }
                } else {
                    String msg = (response != null) ? response.getMessage() : lm.get("msg.error.network");
                    JOptionPane.showMessageDialog(this,
                            lm.get("msg.error.refresh", msg),
                            lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void showEditDialog(Worker worker) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                lm.get("dialog.edit.title"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 440);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField     = new JTextField(worker.getName());
        JTextField coordXField   = new JTextField(worker.getCoordinates() != null ? String.valueOf(worker.getCoordinates().getX()) : "");
        JTextField coordYField   = new JTextField(worker.getCoordinates() != null ? String.valueOf(worker.getCoordinates().getY()) : "");
        JTextField salaryField   = new JTextField(worker.getSalary() != null ? String.valueOf(worker.getSalary()) : "");
        JTextField turnoverField = new JTextField(worker.getOrganization() != null && worker.getOrganization().getAnnualTurnover() != null ? String.valueOf(worker.getOrganization().getAnnualTurnover()) : "");
        JTextField empField      = new JTextField(worker.getOrganization() != null && worker.getOrganization().getEmployeesCount() != null ? String.valueOf(worker.getOrganization().getEmployeesCount()) : "");

        JComboBox<String> positionBox = new JComboBox<>(new String[]{"", "DIRECTOR", "ENGINEER", "HEAD_OF_DEPARTMENT", "LEAD_DEVELOPER"});
        JComboBox<String> statusBox   = new JComboBox<>(new String[]{"", "FIRED", "HIRED", "REGULAR"});
        if (worker.getPosition() != null) positionBox.setSelectedItem(worker.getPosition().name());
        if (worker.getStatus()   != null) statusBox.setSelectedItem(worker.getStatus().name());

        panel.add(new JLabel(lm.get("label.name")));       panel.add(nameField);
        panel.add(new JLabel(lm.get("label.coordX")));     panel.add(coordXField);
        panel.add(new JLabel(lm.get("label.coordY")));     panel.add(coordYField);
        panel.add(new JLabel(lm.get("label.salary")));     panel.add(salaryField);
        panel.add(new JLabel(lm.get("label.position")));   panel.add(positionBox);
        panel.add(new JLabel(lm.get("label.status")));     panel.add(statusBox);
        panel.add(new JLabel(lm.get("label.turnover")));   panel.add(turnoverField);
        panel.add(new JLabel(lm.get("label.employees")));  panel.add(empField);

        JButton saveBtn   = new JButton(lm.get("btn.save"));
        JButton cancelBtn = new JButton(lm.get("btn.cancel"));
        cancelBtn.addActionListener(ev -> dialog.dispose());

        saveBtn.addActionListener(ev -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new Exception(lm.get("msg.error.empty_name"));

                float x = manager.ParserManager.parseFloat.apply(coordXField.getText().trim());
                if (x > 592) throw new Exception(lm.get("msg.error.coord_x_max"));
                if (x <= 0)  throw new Exception(lm.get("msg.error.coord_x_min"));

                double y = manager.ParserManager.parseDouble.apply(coordYField.getText().trim());
                if (y > 846) throw new Exception(lm.get("msg.error.coord_y_max"));
                if (y <= 0)  throw new Exception(lm.get("msg.error.coord_y_min"));

                if (!salaryField.getText().isEmpty() &&
                        manager.ParserManager.parseDouble.apply(salaryField.getText().trim()) <= 0)
                    throw new Exception(lm.get("msg.error.salary"));

                String position = (String) positionBox.getSelectedItem();
                String status   = (String) statusBox.getSelectedItem();
                String[] addArgs = new String[]{
                        name, coordXField.getText().trim(), coordYField.getText().trim(),
                        salaryField.getText().trim(), position, status,
                        turnoverField.getText().trim(), empField.getText().trim(), currentUser
                };

                saveBtn.setEnabled(false);
                new Thread(() -> {
                    controller.sendCommand("remove_by_id",
                            new String[]{String.valueOf(worker.getId())});
                    Response response = controller.sendCommand("add", addArgs);
                    SwingUtilities.invokeLater(() -> {
                        saveBtn.setEnabled(true);
                        if (response != null && response.isSuccess()) {
                            JOptionPane.showMessageDialog(dialog, lm.get("msg.success.add"));
                            dialog.dispose();
                            refreshData();
                        } else {
                            String msg = (response != null) ? response.getMessage() :
                                    lm.get("msg.error.network");
                            JOptionPane.showMessageDialog(dialog,
                                    lm.get("msg.error.label") + ": " + msg,
                                    lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }).start();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, lm.get("msg.error.number"),
                        lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        lm.get("msg.error.label") + ": " + ex.getMessage(),
                        lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void sendCommandAsync(String command, String[] args, boolean refreshAfter) {
        new Thread(() -> {
            Response response = controller.sendCommand(command, args);
            SwingUtilities.invokeLater(() -> {
                if (response != null) {
                    JOptionPane.showMessageDialog(this, response.getMessage());
                    if (response.isSuccess() && refreshAfter) refreshData();
                } else {
                    JOptionPane.showMessageDialog(this,
                            lm.get("msg.error.server"),
                            lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }
}