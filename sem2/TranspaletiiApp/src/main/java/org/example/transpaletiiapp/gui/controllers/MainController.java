package org.example.transpaletiiapp.gui.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.transpaletiiapp.domain.Bearing;
import org.example.transpaletiiapp.domain.PalletTruck;
import org.example.transpaletiiapp.domain.Wheel;
import org.example.transpaletiiapp.domain.WheelMaterial;
import org.example.transpaletiiapp.service.BearingsService;
import org.example.transpaletiiapp.service.PalletTrucksService;
import org.example.transpaletiiapp.service.WheelMaterialsService;
import org.example.transpaletiiapp.service.WheelsService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Master–detail controller for the Wheel (parent) → PalletTruck (child) relationship.
 * One Wheel can be referenced by many PalletTrucks (via wheels_id FK).
 * Clicking a wheel in the parent table shows all pallet trucks that use that wheel.
 * Full CRUD on both parent (Wheel) and child (PalletTruck), plus search/filter,
 * refresh, column sorting, confirmation dialogs, and validation.
 */
public class MainController {

    private PalletTrucksService palletTrucksService;
    private WheelsService wheelsService;
    private BearingsService bearingsService;
    private WheelMaterialsService wheelMaterialsService;

    // Parent (Wheel) table & columns
    @FXML private TableView<Wheel> wheelsTable;
    @FXML private TableColumn<Wheel, String> wheelIdCol;
    @FXML private TableColumn<Wheel, String> wheelMaterialCol;
    @FXML private TableColumn<Wheel, String> wheelMaxWeightCol;
    @FXML private TableColumn<Wheel, String> wheelBearingDiamCol;

    // Parent (Wheel) form controls
    @FXML private ComboBox<WheelMaterial> wheelMaterialCombo;
    @FXML private ComboBox<Bearing> wheelBearingCombo;
    @FXML private TextField wheelMaxWeightField;
    @FXML private TextField wheelSearchField;

    // Child (PalletTruck) table & columns
    @FXML private TableView<PalletTruck> palletTrucksTable;
    @FXML private TableColumn<PalletTruck, String> truckSerialCol;
    @FXML private TableColumn<PalletTruck, String> truckTypeCol;
    @FXML private TableColumn<PalletTruck, String> truckModelCol;
    @FXML private TableColumn<PalletTruck, String> truckCapacityCol;
    @FXML private TableColumn<PalletTruck, String> truckStatusCol;

    // Child (PalletTruck) form controls
    @FXML private TextField truckSerialField;
    @FXML private ComboBox<String> truckTypeCombo;
    @FXML private TextField truckModelField;
    @FXML private TextField truckCapacityField;
    @FXML private ComboBox<String> truckStatusCombo;
    @FXML private TextField truckSearchField;

    @FXML private Label statusLabel;

    private final ObservableList<Wheel> wheelData = FXCollections.observableArrayList();
    private final ObservableList<PalletTruck> truckData = FXCollections.observableArrayList();

    private List<PalletTruck> allTrucks = List.of();

    private Wheel selectedWheel = null;

    public void setServices(PalletTrucksService palletTrucksService,
                            WheelsService wheelsService,
                            BearingsService bearingsService,
                            WheelMaterialsService wheelMaterialsService) {
        this.palletTrucksService = palletTrucksService;
        this.wheelsService = wheelsService;
        this.bearingsService = bearingsService;
        this.wheelMaterialsService = wheelMaterialsService;

        loadComboBoxes();
        loadWheels();
        loadAllTrucksCache();
    }

    // ======================== FXML initialize ========================

    @FXML
    private void initialize() {
        // --- Parent (Wheel) table column setup ---
        wheelIdCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getId().toString()));
        wheelMaterialCol.setCellValueFactory(cd -> {
            WheelMaterial m = cd.getValue().getMaterial();
            return new SimpleStringProperty(m != null ? m.getType() : "—");
        });
        wheelMaxWeightCol.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getMaxWeight())));
        wheelBearingDiamCol.setCellValueFactory(cd -> {
            Bearing b = cd.getValue().getBearing();
            return new SimpleStringProperty(b != null ? String.valueOf(b.getDiameter()) : "—");
        });

        // --- Child (PalletTruck) table column setup ---
        truckSerialCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getSerialNumber()));
        truckTypeCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getType()));
        truckModelCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getModel()));
        truckCapacityCol.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getCapacityKg())));
        truckStatusCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus()));

        // --- Populate combo boxes for truck type & status ---
        truckTypeCombo.setItems(FXCollections.observableArrayList("Manual", "Electric"));
        truckStatusCombo.setItems(FXCollections.observableArrayList(
                "Available", "Rented", "In Maintenance", "Retired"));

        // --- Parent selection listener → loads child trucks for the selected wheel ---
        wheelsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onParentSelectionChanged(newVal));

        // --- Child selection listener → populates child form fields for editing ---
        palletTrucksTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onChildSelectionChanged(newVal));

        // --- Search / filter listeners (bonus) ---
        wheelSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyWheelFilter());
        truckSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyTruckFilter());
    }

    // ======================== Data loading ========================

    /** Loads all wheels from the database into the parent table. */
    private void loadWheels() {
        try {
            wheelData.setAll(wheelsService.findAll());
            applyWheelFilter();
            statusLabel.setText("Loaded " + wheelData.size() + " wheels.");
        } catch (Exception e) {
            showError("Error loading wheels", e.getMessage());
        }
    }

    /** Caches all pallet trucks from the DB so we can filter by wheel ID client-side. */
    private void loadAllTrucksCache() {
        try {
            allTrucks = palletTrucksService.findAll();
        } catch (Exception e) {
            showError("Error loading pallet trucks", e.getMessage());
        }
    }

    /**
     * Loads the pallet trucks that reference the selected wheel into the child table.
     * Filters allTrucks where truck.getWheel().getId() == selectedWheel.getId().
     */
    private void loadTrucksForSelectedWheel() {
        truckData.clear();
        if (selectedWheel != null) {
            List<PalletTruck> children = allTrucks.stream()
                    .filter(t -> t.getWheel() != null
                            && t.getWheel().getId().equals(selectedWheel.getId()))
                    .collect(Collectors.toList());
            truckData.setAll(children);
        }
        applyTruckFilter();
    }

    /** Loads all pallet trucks (unfiltered by parent) into the child table. */
    private void loadAllTrucks() {
        truckData.setAll(allTrucks);
        applyTruckFilter();
    }

    /** Populates combo boxes that require DB data: materials and bearings. */
    private void loadComboBoxes() {
        try {
            // Wheel material combo
            wheelMaterialCombo.setItems(FXCollections.observableArrayList(
                    wheelMaterialsService.findAll()));
            wheelMaterialCombo.setCellFactory(lv -> materialCell());
            wheelMaterialCombo.setButtonCell(materialCell());

            // Bearing combo
            wheelBearingCombo.setItems(FXCollections.observableArrayList(
                    bearingsService.findAll()));
            wheelBearingCombo.setCellFactory(lv -> bearingCell());
            wheelBearingCombo.setButtonCell(bearingCell());
        } catch (Exception e) {
            showError("Error loading combo-box data", e.getMessage());
        }
    }

    // ======================== Selection listeners ========================

    /**
     * Triggered when a parent (Wheel) row is selected.
     * Populates the parent form fields and loads child pallet trucks for this wheel.
     */
    private void onParentSelectionChanged(Wheel wheel) {
        selectedWheel = wheel;
        if (wheel != null) {
            wheelMaterialCombo.setValue(wheel.getMaterial());
            wheelBearingCombo.setValue(wheel.getBearing());
            wheelMaxWeightField.setText(String.valueOf(wheel.getMaxWeight()));

            loadTrucksForSelectedWheel();
            statusLabel.setText("Selected wheel: " + wheel.getId().toString().substring(0, 8)
                    + "… — showing " + truckData.size() + " truck(s).");
        } else {
            loadAllTrucks();
        }
    }

    /**
     * Triggered when a child (PalletTruck) row is selected.
     * Populates child form fields for editing.
     */
    private void onChildSelectionChanged(PalletTruck truck) {
        if (truck != null) {
            truckSerialField.setText(truck.getSerialNumber());
            truckTypeCombo.setValue(truck.getType());
            truckModelField.setText(truck.getModel());
            truckCapacityField.setText(String.valueOf(truck.getCapacityKg()));
            truckStatusCombo.setValue(truck.getStatus());
        }
    }

    // ======================== Parent (Wheel) CRUD actions ========================

    @FXML
    private void onAddWheel() {
        try {
            Wheel wheel = buildWheelFromForm();
            wheel.setId(null); // force new insert
            wheelsService.save(wheel);
            loadWheels();
            statusLabel.setText("Wheel added successfully.");
        } catch (Exception e) {
            showError("Error adding wheel", e.getMessage());
        }
    }

    @FXML
    private void onUpdateWheel() {
        if (selectedWheel == null) {
            showWarning("No wheel selected", "Please select a wheel to update.");
            return;
        }
        try {
            Wheel wheel = buildWheelFromForm();
            wheel.setId(selectedWheel.getId());
            wheelsService.save(wheel);
            loadWheels();
            loadAllTrucksCache(); // trucks embed wheel data, refresh cache
            if (selectedWheel != null) {
                loadTrucksForSelectedWheel();
            }
            statusLabel.setText("Wheel updated successfully.");
        } catch (Exception e) {
            showError("Error updating wheel", e.getMessage());
        }
    }

    @FXML
    private void onDeleteWheel() {
        if (selectedWheel == null) {
            showWarning("No wheel selected", "Please select a wheel to delete.");
            return;
        }
        if (!confirmAction("Delete Wheel",
                "Are you sure you want to delete this wheel?\n" +
                        "All pallet trucks referencing it must be removed first.")) {
            return;
        }
        try {
            wheelsService.deleteById(selectedWheel.getId());
            selectedWheel = null;
            loadWheels();
            loadAllTrucksCache();
            onClearWheelFields();
            statusLabel.setText("Wheel deleted successfully.");
        } catch (Exception e) {
            showError("Error deleting wheel",
                    e.getMessage() + "\nHint: pallet trucks may still reference this wheel (FK constraint).");
        }
    }

    @FXML
    private void onClearWheelFields() {
        wheelMaterialCombo.setValue(null);
        wheelBearingCombo.setValue(null);
        wheelMaxWeightField.clear();
        wheelsTable.getSelectionModel().clearSelection();
        selectedWheel = null;
        loadAllTrucks();
    }

    @FXML
    private void onRefreshWheels() {
        loadWheels();
        loadAllTrucksCache();
        if (selectedWheel != null) {
            loadTrucksForSelectedWheel();
        }
        statusLabel.setText("Wheels refreshed.");
    }

    // ======================== Child (PalletTruck) CRUD actions ========================

    @FXML
    private void onAddTruck() {
        if (selectedWheel == null) {
            showWarning("No wheel selected",
                    "Please select a parent wheel first before adding a truck.");
            return;
        }
        try {
            PalletTruck truck = buildTruckFromForm();
            truck.setId(null);
            truck.setWheel(selectedWheel);
            palletTrucksService.save(truck);
            loadAllTrucksCache();
            loadTrucksForSelectedWheel();
            statusLabel.setText("Truck added successfully for wheel "
                    + selectedWheel.getId().toString().substring(0, 8) + "…");
        } catch (Exception e) {
            showError("Error adding truck", e.getMessage());
        }
    }

    @FXML
    private void onUpdateTruck() {
        PalletTruck selectedTruck = palletTrucksTable.getSelectionModel().getSelectedItem();
        if (selectedTruck == null) {
            showWarning("No truck selected", "Please select a pallet truck to update.");
            return;
        }
        if (selectedWheel == null) {
            showWarning("No wheel selected",
                    "Please select a parent wheel. The truck will be assigned to this wheel.");
            return;
        }
        try {
            PalletTruck truck = buildTruckFromForm();
            truck.setId(selectedTruck.getId());
            truck.setWheel(selectedWheel);
            palletTrucksService.save(truck);
            loadAllTrucksCache();
            loadTrucksForSelectedWheel();
            statusLabel.setText("Truck updated successfully.");
        } catch (Exception e) {
            showError("Error updating truck", e.getMessage());
        }
    }

    @FXML
    private void onDeleteTruck() {
        PalletTruck selectedTruck = palletTrucksTable.getSelectionModel().getSelectedItem();
        if (selectedTruck == null) {
            showWarning("No truck selected", "Please select a pallet truck to delete.");
            return;
        }
        if (!confirmAction("Delete Truck",
                "Are you sure you want to delete truck " + selectedTruck.getSerialNumber() + "?")) {
            return;
        }
        try {
            palletTrucksService.deleteById(selectedTruck.getId());
            loadAllTrucksCache();
            loadTrucksForSelectedWheel();
            onClearTruckFields();
            statusLabel.setText("Truck deleted successfully.");
        } catch (Exception e) {
            showError("Error deleting truck",
                    e.getMessage() + "\nHint: other records may reference this truck (FK constraint).");
        }
    }

    @FXML
    private void onClearTruckFields() {
        truckSerialField.clear();
        truckTypeCombo.setValue(null);
        truckModelField.clear();
        truckCapacityField.clear();
        truckStatusCombo.setValue(null);
        palletTrucksTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onRefreshTrucks() {
        loadAllTrucksCache();
        if (selectedWheel != null) {
            loadTrucksForSelectedWheel();
        } else {
            loadAllTrucks();
        }
        statusLabel.setText("Pallet trucks refreshed.");
    }

    // ======================== Form → entity builders ========================

    /**
     * Builds a Wheel from the parent form fields. Validates required fields.
     */
    private Wheel buildWheelFromForm() {
        WheelMaterial material = wheelMaterialCombo.getValue();
        Bearing bearing = wheelBearingCombo.getValue();
        String maxWeightText = wheelMaxWeightField.getText().trim();

        StringBuilder errors = new StringBuilder();
        if (material == null) errors.append("Material is required.\n");
        if (bearing == null) errors.append("Bearing is required.\n");

        long maxWeight = 0;
        try {
            maxWeight = Long.parseLong(maxWeightText);
            if (maxWeight <= 0) errors.append("Max weight must be a positive number.\n");
        } catch (NumberFormatException e) {
            errors.append("Max weight must be a valid number.\n");
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.toString());
        }

        Wheel wheel = new Wheel();
        wheel.setMaterial(material);
        wheel.setBearing(bearing);
        wheel.setMaxWeight(maxWeight);
        return wheel;
    }

    /**
     * Builds a PalletTruck from the child form fields. Validates required fields.
     * Note: the Wheel (parent) is NOT set here — it is assigned by the caller.
     */
    private PalletTruck buildTruckFromForm() {
        String serial = truckSerialField.getText().trim();
        String type = truckTypeCombo.getValue();
        String model = truckModelField.getText().trim();
        String capText = truckCapacityField.getText().trim();
        String status = truckStatusCombo.getValue();

        StringBuilder errors = new StringBuilder();
        if (serial.isEmpty()) errors.append("Serial number is required.\n");
        if (type == null || type.isEmpty()) errors.append("Type is required.\n");
        if (model.isEmpty()) errors.append("Model is required.\n");
        if (status == null || status.isEmpty()) errors.append("Status is required.\n");

        long capacity = 0;
        try {
            capacity = Long.parseLong(capText);
            if (capacity <= 0) errors.append("Capacity must be a positive number.\n");
        } catch (NumberFormatException e) {
            errors.append("Capacity must be a valid number.\n");
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.toString());
        }

        PalletTruck truck = new PalletTruck();
        truck.setSerialNumber(serial);
        truck.setType(type);
        truck.setModel(model);
        truck.setCapacityKg(capacity);
        truck.setStatus(status);
        return truck;
    }

    // ======================== Search / filter (bonus) ========================

    /**
     * Applies a text filter on the parent (Wheel) table.
     * Matches material type or max weight (case-insensitive).
     */
    private void applyWheelFilter() {
        String text = wheelSearchField.getText();
        String filter = (text == null) ? "" : text.toLowerCase().trim();

        FilteredList<Wheel> filtered = new FilteredList<>(wheelData, w -> {
            if (filter.isEmpty()) return true;
            String matType = w.getMaterial() != null ? w.getMaterial().getType().toLowerCase() : "";
            String weight = String.valueOf(w.getMaxWeight());
            return matType.contains(filter) || weight.contains(filter);
        });

        // wrap in SortedList so column sorting still works (bonus)
        SortedList<Wheel> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(wheelsTable.comparatorProperty());
        wheelsTable.setItems(sorted);
    }

    /**
     * Applies a text filter on the child (PalletTruck) table.
     * Matches serial number, type, model, or status (case-insensitive).
     */
    private void applyTruckFilter() {
        String text = truckSearchField.getText();
        String filter = (text == null) ? "" : text.toLowerCase().trim();

        FilteredList<PalletTruck> filtered = new FilteredList<>(truckData, t -> {
            if (filter.isEmpty()) return true;
            return t.getSerialNumber().toLowerCase().contains(filter)
                    || t.getType().toLowerCase().contains(filter)
                    || t.getModel().toLowerCase().contains(filter)
                    || t.getStatus().toLowerCase().contains(filter);
        });

        SortedList<PalletTruck> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(palletTrucksTable.comparatorProperty());
        palletTrucksTable.setItems(sorted);
    }

    // ======================== Helpers ========================

    // --- Custom ListCell factories for combo boxes ---

    private ListCell<WheelMaterial> materialCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(WheelMaterial item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : item.getType() + " (max " + item.getMaxWeight() + " kg)");
            }
        };
    }

    private ListCell<Bearing> bearingCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Bearing item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : "⌀ " + item.getDiameter() + " mm");
            }
        };
    }


    // --- Dialogs ---

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog and returns true if the user pressed OK.
     */
    private boolean confirmAction(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
