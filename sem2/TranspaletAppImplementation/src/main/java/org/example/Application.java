package org.example;

import org.example.domain.utils.validation.PalletTruckValidator;
import org.example.repository.PalletTruckRepository;
import org.example.repository.implementation.PalletTruckDbRepository;
import org.example.repository.utils.JdbcUtils;
import org.example.service.PalletTrucksService;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Application {
    static void main() {
        Properties props=new Properties();
        try (InputStream is = Application.class.getResourceAsStream("/config/db.config")) {
            if (is == null) {
                throw new RuntimeException("Cannot find bd.config in the resources folder!");
            }
            props.load(is);
            System.out.println("Properties loaded successfully!");
        } catch (Exception e) {
            System.out.println("Error loading config: " + e.getMessage());
        }
        var palletRepo=new PalletTruckDbRepository(props);
        var palletValidator=new PalletTruckValidator();

        var palletTruckService=new PalletTrucksService(palletRepo, palletValidator);
        palletTruckService.findAll().forEach(System.out::println);
    }
}
