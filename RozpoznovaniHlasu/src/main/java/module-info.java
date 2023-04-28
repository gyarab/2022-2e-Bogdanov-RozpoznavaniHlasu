module com.example.rozpoznovanihlasu {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires fft4j;
    requires commons.math3;


    opens com.example.rozpoznovanihlasu to javafx.fxml;
    exports com.example.rozpoznovanihlasu;
}