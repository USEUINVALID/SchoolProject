package buddha.utils;

import arc.files.Fi;

import javax.swing.*;
import java.awt.*;

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

        dialog.setTitle(title);
        dialog.setIconImage(new ImageIcon(files.internal(icon).path()).getImage());

        return dialog;
    }
}