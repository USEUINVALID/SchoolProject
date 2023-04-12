package buddha.utils;

import arc.files.Fi;

import javax.swing.filechooser.FileFilter;
import java.io.File;

// Фильтр для файлов, который принимает несколько расширений вместо одного
public class FileChooserFilter extends FileFilter {
    public final String description;
    public final String[] extensions;

    public FileChooserFilter(String description, String... extensions) {
        this.description = description;
        this.extensions = extensions;
    }

    @Override
    public boolean accept(File file) {
        var fi = new Fi(file);
        if (fi.isDirectory()) return true;

        for (var extension : extensions)
            if (fi.extEquals(extension)) return true;

        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }
}