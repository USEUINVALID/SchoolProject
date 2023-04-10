package buddha.utils;

import arc.util.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static arc.Core.files;

public class FileChooser extends JFileChooser {

    public final String title;
    public final String icon;

    public FileChooser(String directory, String title, String icon) {
        super(directory);

        this.title = title;
        this.icon = icon;
    }

    @Override
    public JDialog createDialog(Component parent) throws HeadlessException {
        var dialog = super.createDialog(parent);

        try {
            dialog.setTitle(title);
            dialog.setIconImage(ImageIO.read(files.internal(icon).read()));
        } catch (IOException ignored) {}

        return dialog;
    }
}