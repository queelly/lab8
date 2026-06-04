package gui;

import manager.ClientController;
import network.Response;
import models.Worker;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * Диалог добавления / редактирования Worker.
 * Если worker == null — режим добавления (команда "add").
 * Если worker != null — режим редактирования (команда "update").
 */
public class WorkerEditDialog extends JDialog {
    private final ClientController controller;
    private final Worker existingWorker;
    private final String creator;
    private final LocaleManager lm = LocaleManager.getInstance();

    private JTextField nameField, coordXField, coordYField, salaryField;
    private JTextField annualTurnoverField, employeesCountField;
    private JComboBox<String> positionBox, statusBox;

    private JLabel nameLabel, coordXLabel, coordYLabel, salaryLabel;
    private JLabel positionLabel, statusLabel, turnoverLabel, employeesLabel;
    private JButton saveBtn, cancelBtn;

    private final PropertyChangeListener localeListener = evt -> applyLocale();

    public WorkerEditDialog(Window owner, ClientController controller, Worker workerToEdit, String creator) {
        super(owner, "", Dialog.ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.existingWorker = workerToEdit;
        this.creator = creator;
        lm.addChangeListener(localeListener);
        setSize(420, 440);
        setLocationRelativeTo(owner);
        initComponents();
        applyLocale();
        if (workerToEdit != null) prefillFields();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        positionBox = new JComboBox<>(new String[]{
                "", "DIRECTOR", "ENGINEER", "HEAD_OF_DEPARTMENT", "LEAD_DEVELOPER"
        });
        statusBox = new JComboBox<>(new String[]{
                "", "FIRED", "HIRED", "REGULAR"
        });

        nameLabel = new JLabel(); coordXLabel = new JLabel();
        coordYLabel = new JLabel(); salaryLabel = new JLabel();
        positionLabel = new JLabel(); statusLabel = new JLabel();
        turnoverLabel = new JLabel(); employeesLabel = new JLabel();

        nameField = new JTextField(); coordXField = new JTextField();
        coordYField = new JTextField(); salaryField = new JTextField();
        annualTurnoverField = new JTextField(); employeesCountField = new JTextField();

        panel.add(nameLabel);      panel.add(nameField);
        panel.add(coordXLabel);    panel.add(coordXField);
        panel.add(coordYLabel);    panel.add(coordYField);
        panel.add(salaryLabel);    panel.add(salaryField);
        panel.add(positionLabel);  panel.add(positionBox);
        panel.add(statusLabel);    panel.add(statusBox);
        panel.add(turnoverLabel);  panel.add(annualTurnoverField);
        panel.add(employeesLabel); panel.add(employeesCountField);

        saveBtn = new JButton(); cancelBtn = new JButton();
        cancelBtn.addActionListener(e -> { lm.removeChangeListener(localeListener); dispose(); });
        saveBtn.addActionListener(e -> onSave());

        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn); btnPanel.add(cancelBtn);
        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void applyLocale() {
        setTitle(existingWorker == null ? lm.get("dialog.add.title") :
                lm.get("dialog.edit.title"));
        nameLabel.setText(lm.get("label.name"));
        coordXLabel.setText(lm.get("label.coordX"));
        coordYLabel.setText(lm.get("label.coordY"));
        salaryLabel.setText(lm.get("label.salary"));
        positionLabel.setText(lm.get("label.position"));
        statusLabel.setText(lm.get("label.status"));
        turnoverLabel.setText(lm.get("label.turnover"));
        employeesLabel.setText(lm.get("label.employees"));
        saveBtn.setText(lm.get("btn.save"));
        cancelBtn.setText(lm.get("btn.cancel"));
        revalidate(); repaint();
    }

    private void prefillFields() {
        nameField.setText(existingWorker.getName());
        if (existingWorker.getCoordinates() != null) {
            coordXField.setText(String.valueOf(existingWorker.getCoordinates().getX()));
            coordYField.setText(String.valueOf(existingWorker.getCoordinates().getY()));
        }
        if (existingWorker.getSalary() != null)
            salaryField.setText(String.valueOf(existingWorker.getSalary()));
        if (existingWorker.getPosition() != null)
            positionBox.setSelectedItem(existingWorker.getPosition().name());
        if (existingWorker.getStatus() != null)
            statusBox.setSelectedItem(existingWorker.getStatus().name());
        if (existingWorker.getOrganization() != null) {
            if (existingWorker.getOrganization().getAnnualTurnover() != null)
                annualTurnoverField.setText(String.valueOf(existingWorker.getOrganization().getAnnualTurnover()));
            if (existingWorker.getOrganization().getEmployeesCount() != null)
                employeesCountField.setText(String.valueOf(existingWorker.getOrganization().getEmployeesCount()));
        }
    }

    private void onSave() {
        try {
            if (nameField.getText().trim().isEmpty())
                throw new Exception(lm.get("msg.error.empty_name"));

            float x = manager.ParserManager.parseFloat.apply(coordXField.getText().trim());
            if (x > 592) throw new Exception(lm.get("msg.error.coord_x_max"));
            if (x <= 0)  throw new Exception(lm.get("msg.error.coord_x_min"));

            double y = manager.ParserManager.parseDouble.apply(coordYField.getText().trim());
            if (y > 846) throw new Exception(lm.get("msg.error.coord_y_max"));
            if (y <= 0)  throw new Exception(lm.get("msg.error.coord_y_min"));

            if (!salaryField.getText().isEmpty()) {
                if (manager.ParserManager.parseDouble.apply(salaryField.getText().trim()) <= 0)
                    throw new Exception(lm.get("msg.error.salary"));
            }

            String position = (String) positionBox.getSelectedItem();
            String status   = (String) statusBox.getSelectedItem();
            boolean isEdit  = existingWorker != null;
            String command  = isEdit ? "update" : "add";

            String[] args = isEdit
                    ? new String[]{String.valueOf(existingWorker.getId()),
                    nameField.getText().trim(), coordXField.getText().trim(),
                    coordYField.getText().trim(), salaryField.getText().trim(),
                    position, status, annualTurnoverField.getText().trim(),
                    employeesCountField.getText().trim(), creator.trim()}
                    : new String[]{nameField.getText().trim(), coordXField.getText().trim(),
                    coordYField.getText().trim(), salaryField.getText().trim(),
                    position, status, annualTurnoverField.getText().trim(),
                    employeesCountField.getText().trim(), creator.trim()};

            saveBtn.setEnabled(false);
            new Thread(() -> {
                Response response = controller.sendCommand(command, args);
                SwingUtilities.invokeLater(() -> {
                    saveBtn.setEnabled(true);
                    if (response != null && response.isSuccess()) {
                        JOptionPane.showMessageDialog(this, lm.get("msg.success.add"));
                        lm.removeChangeListener(localeListener);
                        dispose();
                    } else {
                        String msg = (response != null) ? response.getMessage() : lm.get("msg.error.network");
                        JOptionPane.showMessageDialog(this,
                                lm.get("msg.error.label") + ": " + msg,
                                lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, lm.get("msg.error.number"),
                    lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    lm.get("msg.error.label") + ": " + ex.getMessage(),
                    lm.get("msg.error.label"), JOptionPane.ERROR_MESSAGE);
        }
    }
}