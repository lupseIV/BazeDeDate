package org.example;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.domain.*;
import org.example.domain.utils.validation.*;
import org.example.gui.controllers.MainController;
import org.example.repository.*;
import org.example.repository.jpa.*;
import org.example.repository.utils.JdbcHikariUtils;
import org.example.repository.utils.JdbcUtils;
import org.example.repository.utils.JpaUtils;
import org.example.service.*;

import java.io.IOException;
import java.net.URL;

public class TranspaletiiApp extends Application {

    private PalletTrucksService palletTrucksService;
    private WheelsService wheelsService;
    private BearingsService bearingsService;
    private WheelMaterialsService wheelMaterialsService;
    private RentalsService rentalsService;

    @Override
    public void init() throws Exception {
        super.init();

        // validators
        Validator<PalletTruck> palletTruckValidator = new PalletTruckValidator();
        Validator<Wheel> wheelValidator = new WheelValidator();
        Validator<Bearing> bearingValidator = new BearingValidator();
        Validator<WheelMaterial> wheelMaterialValidator = new WheelMaterialValidator();
        Validator<Rental> rentalValidator = new RentalValidator();
        EntityManagerFactory factory = JpaUtils.getEntityManagerFactory();

        // repositories
        WheelsRepository wheelsRepository = new WheelJpaRepository(factory);
        PalletTruckRepository palletTruckRepository = new PalletTruckJpaRepository(factory);
        BearingRepository bearingRepository = new BearingJpaRepository(factory);
        WheelMaterialRepository wheelMaterialRepository = new WheelMaterilJpaRepository(factory);
        RentalRepository rentalRepository = new RentalJpaRepository(factory);
        // services
        this.palletTrucksService = new PalletTrucksService(palletTruckRepository, palletTruckValidator);
        this.wheelsService = new WheelsService(wheelsRepository, wheelValidator);
        this.bearingsService = new BearingsService(bearingRepository, bearingValidator);
        this.wheelMaterialsService = new WheelMaterialsService(wheelMaterialRepository, wheelMaterialValidator);
        this.rentalsService = new RentalsService(rentalRepository, rentalValidator);
    }

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlUrl = TranspaletiiApp.class.getResource("index.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Cannot find index.fxml on classpath.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Parent root = fxmlLoader.load();

        // inject services into controller
        MainController controller = fxmlLoader.getController();
        controller.setServices(palletTrucksService, wheelsService, bearingsService, wheelMaterialsService, rentalsService);

        Scene scene = new Scene(root);
        stage.setTitle("TranspaletiiApp – Pallet Trucks & Wheels");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        JdbcHikariUtils.closePool();

        JdbcUtils.closeConnection();
    }
}