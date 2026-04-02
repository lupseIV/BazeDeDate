module TranspaletAppImplementation.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example to javafx.fxml;
    exports org.example;

    opens org.example.domain to javafx.base;
    exports org.example.domain;

    exports org.example.gui.controllers;
    opens org.example.gui.controllers to javafx.fxml;

    exports org.example.service;
}