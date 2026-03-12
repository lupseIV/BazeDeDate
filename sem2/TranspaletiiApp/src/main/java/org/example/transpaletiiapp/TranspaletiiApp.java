package org.example.transpaletiiapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.transpaletiiapp.domain.Bearing;
import org.example.transpaletiiapp.domain.PalletTruck;
import org.example.transpaletiiapp.domain.Wheel;
import org.example.transpaletiiapp.domain.WheelMaterial;
import org.example.transpaletiiapp.domain.utils.validation.*;
import org.example.transpaletiiapp.gui.controllers.MainController;
import org.example.transpaletiiapp.repository.BearingRepository;
import org.example.transpaletiiapp.repository.PalletTruckRepository;
import org.example.transpaletiiapp.repository.WheelMaterialRepository;
import org.example.transpaletiiapp.repository.WheelsRepository;
import org.example.transpaletiiapp.repository.implementation.*;
import org.example.transpaletiiapp.service.BearingsService;
import org.example.transpaletiiapp.service.PalletTrucksService;
import org.example.transpaletiiapp.service.WheelMaterialsService;
import org.example.transpaletiiapp.service.WheelsService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class TranspaletiiApp extends Application {

    private PalletTrucksService palletTrucksService;
    private WheelsService wheelsService;
    private BearingsService bearingsService;
    private WheelMaterialsService wheelMaterialsService;

    private Properties appConfig;

    /**
     * Loads the database configuration from the classpath resource /config/db.config.
     */
    private void initAppConfig() {
        appConfig = new Properties();
        try (InputStream is = TranspaletiiApp.class.getResourceAsStream("/config/db.config")) {
            if (is == null) {
                throw new RuntimeException("Cannot find db.config in the resources folder!");
            }
            appConfig.load(is);
            System.out.println("Properties loaded successfully!");
        } catch (Exception e) {
            System.out.println("Error loading config: " + e.getMessage());
        }
    }

    @Override
    public void init() throws Exception {
        super.init();

        initAppConfig();
        if (appConfig.isEmpty()) {
            throw new RuntimeException("App config is empty. Cannot initialize services.");
        }

        // validators
        Validator<PalletTruck> palletTruckValidator = new PalletTruckValidator();
        Validator<Wheel> wheelValidator = new WheelValidator();
        Validator<Bearing> bearingValidator = new BearingValidator();
        Validator<WheelMaterial> wheelMaterialValidator = new WheelMaterialValidator();

        // repositories
        PalletTruckRepository palletTruckRepository = new PalletTruckDbRepository(appConfig);
        WheelsRepository wheelsRepository = new WheelDbRepository(appConfig);
        BearingRepository bearingRepository = new BearingDbRepository(appConfig);
        WheelMaterialRepository wheelMaterialRepository = new WheelMaterialDbRepository(appConfig);

        // services
        this.palletTrucksService = new PalletTrucksService(palletTruckRepository, palletTruckValidator);
        this.wheelsService = new WheelsService(wheelsRepository, wheelValidator);
        this.bearingsService = new BearingsService(bearingRepository, bearingValidator);
        this.wheelMaterialsService = new WheelMaterialsService(wheelMaterialRepository, wheelMaterialValidator);
    }

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlUrl = TranspaletiiApp.class.getResource("hello-view.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Cannot find hello-view.fxml on classpath.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Parent root = fxmlLoader.load();

        // inject services into controller
        MainController controller = fxmlLoader.getController();
        controller.setServices(palletTrucksService, wheelsService, bearingsService, wheelMaterialsService);

        Scene scene = new Scene(root);
        stage.setTitle("TranspaletiiApp – Pallet Trucks & Wheels");
        stage.setScene(scene);
        stage.show();
    }

}
