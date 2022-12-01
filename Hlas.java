
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public abstract class Hlas {
    TargetDataLine line;
    AudioFormat format = new AudioFormat(800F,16,1,true,true);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class,
            format);
    if (!AudioSystem.isLineSupported(info)) {


    }
    try {
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        try {
            line.open(format);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    } catch (LineUnavailableException ex) {
    }
    ByteArrayOutputStream out  = new ByteArrayOutputStream();
    int numBytesRead;
    byte[] data = new byte[line.getBufferSize() / 5];
    int read(byte[] b, int offset, int length);
    line.start();
        while (!stopped) {
        numBytesRead =  line.read(data, 0, data.length);
        out.write(data, 0, numBytesRead);
    }

}
