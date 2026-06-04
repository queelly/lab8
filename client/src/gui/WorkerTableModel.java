package gui;

import models.Worker;
import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WorkerTableModel extends AbstractTableModel {
    private List<Worker> originalData = new ArrayList<>();
    private List<Worker> viewData     = new ArrayList<>();
    private final LocaleManager lm    = LocaleManager.getInstance();

    private int currentSortCol = -1;
    private boolean isAscending = true;
    private String currentFilterQuery = ""; // Храним текущий поисковый запрос

    private static final String[] COL_KEYS = {
            "col.id", "col.name", "col.x", "col.y", "col.date",
            "col.salary", "col.position", "col.status",
            "col.turnover", "col.employees", "col.creator"
    };

    public void setData(List<Worker> workers) {
        this.originalData = (workers == null) ? new ArrayList<>() : workers;
        applyFilterAndSort();
    }

    public void updateColumnNames() {
        fireTableStructureChanged();
    }

    public void sort(int columnIndex, boolean ascending) {
        this.currentSortCol = columnIndex;
        this.isAscending = ascending;
        applyFilterAndSort();
    }

    public void filterByName(String nameQuery) {
        this.currentFilterQuery = (nameQuery == null) ? "" : nameQuery.trim();
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {

        if (currentFilterQuery.isBlank()) {
            this.viewData = new ArrayList<>(originalData);
        } else {
            String lowerQuery = currentFilterQuery.toLowerCase();
            this.viewData = originalData.stream()
                    .filter(w -> w.getName() != null &&
                            w.getName().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }

        if (currentSortCol != -1) {
            Comparator<Worker> comparator = getComparatorForColumn(currentSortCol);
            if (comparator != null) {
                if (!isAscending) {
                    comparator = comparator.reversed();
                }
                this.viewData.sort(comparator);
            }
        }

        fireTableDataChanged();
    }

    private Comparator<Worker> getComparatorForColumn(int columnIndex) {
        return switch (columnIndex) {
            case 0  -> Comparator.comparing(Worker::getId);
            case 1  -> Comparator.comparing(Worker::getName, Comparator.nullsLast(String::compareTo));
            case 2  -> Comparator.comparing(w -> w.getCoordinates() != null ? w.getCoordinates().getX() : 0.0f);
            case 3  -> Comparator.comparing(w -> w.getCoordinates() != null ? w.getCoordinates().getY() : 0.0);
            case 4  -> Comparator.comparing(Worker::getCreationDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case 5  -> Comparator.comparing(w -> w.getSalary() != null ? w.getSalary() : 0.0);
            case 6  -> Comparator.comparing(w -> w.getPosition() != null ? w.getPosition().name() : "");
            case 7  -> Comparator.comparing(w -> w.getStatus() != null ? w.getStatus().name() : "");
            case 8  -> Comparator.comparing(w -> (w.getOrganization() != null && w.getOrganization().getAnnualTurnover() != null)
                    ? w.getOrganization().getAnnualTurnover() : 0.0);
            case 9  -> Comparator.comparing(w -> (w.getOrganization() != null && w.getOrganization().getEmployeesCount() != null)
                    ? w.getOrganization().getEmployeesCount() : 0);
            case 10 -> Comparator.comparing(Worker::getCreator, Comparator.nullsLast(String::compareTo));
            default -> null;
        };
    }

    @Override public int getRowCount()    { return viewData.size(); }
    @Override public int getColumnCount() { return COL_KEYS.length; }

    @Override
    public String getColumnName(int column) {
        return lm.get(COL_KEYS[column]);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Worker w = viewData.get(rowIndex);
        return switch (columnIndex) {
            case 0  -> w.getId();
            case 1  -> w.getName();
            case 2  -> w.getCoordinates() != null ? w.getCoordinates().getX() : "";
            case 3  -> w.getCoordinates() != null ? w.getCoordinates().getY() : "";
            case 4  -> w.getCreationDate() != null
                    ? lm.formatDate(Date.from(
                    w.getCreationDate().atZone(ZoneId.systemDefault()).toInstant()))
                    : "";
            case 5  -> w.getSalary() != null ? lm.formatNumber(w.getSalary()) : "";
            case 6  -> w.getPosition();
            case 7  -> w.getStatus();
            case 8  -> (w.getOrganization() != null)
                    ? lm.formatNumber(w.getOrganization().getAnnualTurnover()) : "";
            case 9  -> (w.getOrganization() != null)
                    ? lm.formatNumber(w.getOrganization().getEmployeesCount()) : "";
            case 10 -> w.getCreator() != null ? w.getCreator() : "System";
            default -> null;
        };
    }

    public Worker getWorkerAt(int rowIndex) { return viewData.get(rowIndex); }

    public int findRowById(int id) {
        for (int i = 0; i < viewData.size(); i++) {
            if (viewData.get(i).getId() == id) return i;
        }
        return -1;
    }
}