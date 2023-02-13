import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException
    {
        var file = new File("test.png");
        var image = ImageIO.read(file);
        var Height = image.getHeight();
        var Widht = image.getWidth();


        var frame = new JFrame();
        frame.setTitle("Пошел нахуй");
        frame.setSize(Widht, Height);
        var label = new JLabel();
        label.setIcon(new ImageIcon(image));

        frame.getContentPane().add(label);
        frame.setVisible(true);
        //    var input = new File("test.png");
        //       System.out.println(input.getAbsoluteFile());
    }

}