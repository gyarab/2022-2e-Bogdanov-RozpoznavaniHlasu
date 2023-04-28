package com.example.rozpoznovanihlasu;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RozpoznavaniHlasu extends Application {

    //vytváření proměnných
    public HBox lineNextTo = new HBox();

    public Label label =  new Label("vitejte v mem programu zmacknete !!jednou!! tlacitko spustit"+"\n"+
            "a mluvte 10 sekund a cekejte dokud tlacitko nezmizi");

    public VBox vertical = new VBox();

    public Pane pane = new Pane();

    public AnchorPane anchorPane = new AnchorPane();

    public TextField text = new TextField();

    //proměnná pro frekvenci
    public List <Double> frequency = new ArrayList<>();
    public List <Double> magnitude = new ArrayList<>();

    //zjistí, co za soubory se nachází v souboru, kde jsou uložené osoby s jejich frekvencí
    public File infoFiles = new File("src\\main\\resources\\files\\");
    public File infoFilesMag = new File("src\\main\\resources\\mag\\");
    //seznam jmen všech souborů končící na .dat v souboru files
    public File[] listOfFiles = infoFiles.listFiles();
    public File[] listOfFilesMag = infoFilesMag.listFiles();

    public List<Double> loadedFreq = new ArrayList<>();
    public List<Double> loadedMag = new ArrayList<>();

    public double[] percentage = new double[listOfFiles.length];

    public  Button btn = new Button("Nekdo jiny");

    //metoda vytvoří tlačítko "spustit" a jeho funkce je začít nahrávat zvuk a smazat se
    public void ovladaciPrvky(){
        Button play = new Button("Spustit");


        play.setStyle("-fx-background-color: Green");

        vertical.getChildren().addAll(label, play);

        anchorPane.getChildren().addAll(lineNextTo);

        play.setPrefWidth(75);

        lineNextTo.getChildren().addAll(vertical);
        lineNextTo.relocate(110,110);

        pane.getChildren().addAll(anchorPane);

        play.setOnAction((e)->{
            vertical.getChildren().clear();
            lineNextTo.getChildren().clear();
            try {
                hlas();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }


        });
    }

    //metoda, která pmáhá získat data z hlasu
    private static long extendSign(long temp, int bitsPerSample) {
        int extensionBits = 64 - bitsPerSample;
        return (temp << extensionBits) >> extensionBits;
    }



    //metoda nahraje zvuk z mikrofonu následně převede do FFT a zavolá metodu ulozit()
    public void hlas() throws LineUnavailableException, InterruptedException, IOException, ClassNotFoundException {

        //informace k mikrofonu, zalezi operačním systému (toto je pro windows)
        AudioFormat format = new AudioFormat(16000, 8, 2, true, true);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("ne podpora");
        }
        TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);

        targetDataLine.open();

        targetDataLine.open(format);
        targetDataLine.start();

        //mluvit po dobu 10s
        //čeká se 10 sekund na nahrání zvuku
        Thread.sleep(10000);



        vypocet(targetDataLine);

        //zavolá metodu ulozit
        ulozit();

        targetDataLine.stop();
        targetDataLine.close();
    }

    //metoda spočítá data z hlasu a převede do FFT z toho spočítá frekvenci a její magnitudu.
    public void vypocet(TargetDataLine targetDataLine){
        float sampleRate = 8000;
        byte[] data = new byte[(int) sampleRate * 10];
        int read = targetDataLine.read(data, 0, (int) sampleRate * 10);
        if (read > 0) {

            int paddedLength = Integer.highestOneBit(read) << 1;
            if (paddedLength != read) {
                byte[] paddedData = new byte[paddedLength];
                Arrays.fill(paddedData, read, paddedLength, (byte) 0);
                System.arraycopy(data, 0, paddedData, 0, read);
                data = paddedData;
                read = paddedLength;
            }

            double[] fftData = new double[read];
            for (int i = 0; i < read - 1; i = i + 2) {
                long val = ((data[i] & 0xffL) << 8L) | (data[i + 1] & 0xffL);
                long valf = extendSign(val, 16);
                fftData[i] = (double) valf;
            }
            Complex[] fftComplex = new Complex[read / 2];
            for (int i = 0; i < read / 2; ++i) {
                fftComplex[i] = new Complex(fftData[2 * i], fftData[2 * i + 1]);
            }
            FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] fftResult = transformer.transform(fftComplex, TransformType.FORWARD);

            // Print the frequency and magnitude
            for (int i = 0; i < fftResult.length; i++) {
                frequency.add((double) (sampleRate * i / read));
                magnitude.add(fftResult[i].abs());
            }

        }
    }

    //metoda načte soubor a porovná hodnoty, podle podmínek uloží do souboru
    public void ulozit() throws IOException, ClassNotFoundException {

        //proměnná pro zjištění kdo mluví, tedy kolik lidí se s hlasem neshoduje
        int unknown = 0;

        for (int indexFiles = 0; indexFiles < listOfFiles.length && indexFiles < listOfFilesMag.length; indexFiles++) {
            //pokud existují soubory, v souboru files/mag, končící na .dat
            if(listOfFiles[indexFiles].getName().endsWith(".dat") == true) {
                //tak vem první název souboru a jeho hodnoty dej do arraylistu
                if (listOfFiles[indexFiles].isFile() && listOfFiles[indexFiles].getName().endsWith(".dat")) {
                    FileInputStream f = new FileInputStream("src\\main\\resources\\files\\" + listOfFiles[indexFiles].getName());
                    ObjectInputStream in = new ObjectInputStream(f);
                    loadedFreq = (ArrayList<Double>) in.readObject();
                }
                if (listOfFilesMag[indexFiles].isFile() && listOfFilesMag[indexFiles].getName().endsWith(".dat")) {
                    FileInputStream f = new FileInputStream("src\\main\\resources\\mag\\" + listOfFiles[indexFiles].getName());
                    ObjectInputStream in = new ObjectInputStream(f);
                    loadedMag = (ArrayList<Double>) in.readObject();
                }



                //proměnná pro počet čísel se shodou
                int found = 0;
                //porovnávání každé číslo s každým
                for(int index = 0; index < frequency.size(); index++){
                    //jinak napsano if(listUniqueLoad.contains(frequency.get(index))-> jestli je rovno frekvenci(porovná každou hodnotu s každou)
                    // pokud ano přičti found jedna, pokud ne přičti nula
                    found += (loadedFreq.contains(frequency.get(index)) && loadedMag.contains(magnitude.get(index)) ) ? 1 : 0;
                }
                //v kolika procentech se soubor shoduje s hlasem
                percentage[indexFiles] = (found * 100) / (loadedFreq.size());

            }
            //pokud méně než 70%, tak přičti k unknown jedna a vymaž hodnoty načteného souboru
            if (percentage[indexFiles] < 70) {
                unknown++;
                loadedMag.clear();
                loadedFreq.clear();

                //pokud unknown je rovno počtu souboru v souboru files
                if (unknown == listOfFiles.length) {
                    //zavolej metodu souhlas() ->  uživatel zadá jméno nové osoby
                    souhlas();
                }
            //pokud je to více než 70% uživatel následně vybere jméno osoby a nové data se přidají k jeho souboru
            } else if(percentage[indexFiles] >= 70) {


                FileOutputStream fileOut = new FileOutputStream("src\\main\\resources\\files\\" + listOfFiles[indexFiles].getName());
                ObjectOutputStream writer = new ObjectOutputStream(fileOut);

                FileOutputStream fileOutMag = new FileOutputStream("src\\main\\resources\\mag\\" + listOfFiles[indexFiles].getName());
                ObjectOutputStream writerMag = new ObjectOutputStream(fileOutMag);

                ArrayList<Double> loadedFreqFromLF = new ArrayList<>();
                ArrayList<Double> loadedMagFromLM = new ArrayList<>();

                for (int index = 0; index < loadedFreq.size(); index++) {
                    loadedFreqFromLF.add(loadedFreq.get(index));
                    loadedMagFromLM.add(loadedMag.get(index));
                }
                writer.writeObject(loadedFreq);
                writerMag.writeObject(loadedMag);
                loadedFreq.clear();
                loadedMag.clear();

                Label l = new Label();
                l.setText(listOfFiles[indexFiles].getName() + " " + percentage[indexFiles] + "%");

                CheckBox check = new CheckBox();

                HBox line = new HBox();
                VBox col = new VBox();


                col.setSpacing(10);
                col.getChildren().addAll(l, btn);

                line.getChildren().addAll(col, check);

                vertical.getChildren().add(line);
                vertical.relocate(110,110);


                check.setOnAction((e) -> {

                    //vybrané jméno uloží
                    for (int i = 0; i < frequency.size(); i++) {
                        loadedFreqFromLF.add(frequency.get(i));
                        loadedMagFromLM.add(magnitude.get(i));
                    }
                    try {
                        writer.writeObject(loadedFreqFromLF);
                        writerMag.writeObject(loadedMagFromLM);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    //ukonci se program
                    System.exit(0);
                });

                btn.setOnAction((e) -> {
                    vertical.getChildren().clear();
                    souhlas();
                });
            }

        }
        anchorPane.getChildren().add(vertical);
        //pokud nejsou žadné soubory v souboru files, tak jeden vytvoř
        if(listOfFiles.length == 0) {
            souhlas();
        }
    }

    //metoda souhlas slouží k tomu, aby uživatel zadal jméno nové osoby
    public void souhlas()  {
        Button btn = new Button("Potvrdit");

        label.setText("neznama osoba zadejte jmeno nove osoby");
        vertical.getChildren().addAll(label,text, btn);
        text.setPromptText("Jmeno osoby");

        lineNextTo.getChildren().addAll(vertical);


        btn.setOnAction((e)->{
            try {
                FileOutputStream fOut = new FileOutputStream("src\\main\\resources\\files\\" + text.getText() + ".dat");
                ObjectOutputStream out = new ObjectOutputStream(fOut);

                FileOutputStream fOutMag = new FileOutputStream("src\\main\\resources\\mag\\" + text.getText() + ".dat");
                ObjectOutputStream outMag = new ObjectOutputStream(fOutMag);

                out.writeObject(frequency);
                outMag.writeObject(magnitude);
                vertical.getChildren().clear();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

    }


    //spustí se program
    @Override
    public void start(Stage stage) {

        ovladaciPrvky();
        Scene scene = new Scene(pane, 450, 480);
        stage.setScene(scene);
        stage.setTitle("hlas");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
