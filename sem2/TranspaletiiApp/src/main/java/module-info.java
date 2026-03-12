module org.example.transpaletiiapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;

    opens org.example.transpaletiiapp to javafx.fxml;
    exports org.example.transpaletiiapp;

    opens org.example.transpaletiiapp.domain to javafx.base;
    exports org.example.transpaletiiapp.domain;

    exports org.example.transpaletiiapp.gui.controllers;
    opens org.example.transpaletiiapp.gui.controllers to javafx.fxml;

    exports org.example.transpaletiiapp.service;
}