//import javax.imageio.ImageIO;
//import javax.swing.*;
//import java.awt.*;
//import java.io.File;
//import java.io.IOException;
//
//public class Main {
//
//    public static <test> void main(String[] args) throws IOException
//    {
//        var file = new File("test.png");
//        var image = ImageIO.read(file);
//        var Height = image.getHeight();
//        var Widht = image.getWidth();
//
////JFileChooser(File currentDirectory)
//        var frame = new JFrame();
//        frame.setTitle("Школьный проект");
//        frame.setSize(Widht, Height);
//        var label = new JLabel();
//        label.setIcon(new ImageIcon(image));
//
//        frame.getContentPane().add(label);
//        frame.setVisible(true);
//        //    var input = new File("test.png");
//        //       System.out.println(input.getAbsoluteFile());
//    }
//
//}

import javax.swing.*;
import java.awt.event.*;
class FileFilterExt extends javax.swing.filechooser.FileFilter
{
    String extension  ;  // расширение файла
    String description;  // описание типа файлов

    FileFilterExt(String extension, String descr)
    {
        this.extension = extension;
        this.description = descr;
    }
    @Override
    public boolean accept(java.io.File file)
    {
        if(file != null) {
            if (file.isDirectory())
                return true;
            if( extension == null )
                return (extension.length() == 0);
            return file.getName().endsWith(extension);
        }
        return false;
    }
    // Функция описания типов файлов
    @Override
    public String getDescription() {
        return description;
    }
}

public class Main extends JFrame
{
    //    var Image = file.getName();
    private  JButton  btnSaveFile   = null;
    private  JButton  btnOpenDir    = null;
    private  JButton  btnFileFilter = null;

    private  JFileChooser fileChooser = null;

    private final String[][] FILTERS = {{"docx", "Файлы Word (*.docx)"},
            {"png" , "Paint(*.png)"}};
    public Main() {
        super("Пример FileChooser");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        btnOpenDir = new JButton("Открыть директорию");
        btnSaveFile = new JButton("Сохранить файл");
        btnFileFilter = new JButton("Фильтрация файлов");

        fileChooser = new JFileChooser();
        addFileChooserListeners();

        JPanel contents = new JPanel();
        contents.add(btnOpenDir);
        contents.add(btnSaveFile);
        contents.add(btnFileFilter);
        setContentPane(contents);
        setSize(360, 110);
        setVisible(true);
        btnSaveFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setDialogTitle("Сохранение файла");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showSaveDialog(Main.this);
                if (result == JFileChooser.APPROVE_OPTION )
                    JOptionPane.showMessageDialog(Main.this,
                            "Файл '" + fileChooser.getSelectedFile() +
                                    " ) сохранен");
            }
        });


        btnOpenDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.setDialogTitle("Выбор директории");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(Main.this);
                if (result == JFileChooser.APPROVE_OPTION )
                    JOptionPane.showMessageDialog(Main.this,
                            fileChooser.getSelectedFile());
            }
        });


        btnFileFilter.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                fileChooser.setDialogTitle("Выберите файл");
                // Определяем фильтры типов файлов
                for (int i = 0; i < FILTERS[0].length; i++) {
                    FileFilterExt eff = new FileFilterExt(FILTERS[i][0],
                            FILTERS[i][1]);
                    fileChooser.addChoosableFileFilter(eff);
                }
                // Определение режима - только файл
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showSaveDialog(Main.this);
                // Если файл выбран, покажем его в сообщении
                if (result == JFileChooser.APPROVE_OPTION )
                    JOptionPane.showMessageDialog(Main.this,
                            "Выбран файл ( " +
                                    fileChooser.getSelectedFile() + " )");
            }
        });




    }



    private void addFileChooserListeners() {
    }

    public static void main(String[] args)
    {
        // Локализация компонентов окна JFileChooser
        UIManager.put(
                "FileChooser.saveButtonText", "Сохранить");
        UIManager.put(
                "FileChooser.cancelButtonText", "Отмена");
        UIManager.put(
                "FileChooser.fileNameLabelText", "Наименование файла");
        UIManager.put(
                "FileChooser.filesOfTypeLabelText", "Типы файлов");
        UIManager.put(
                "FileChooser.lookInLabelText", "Директория");
        UIManager.put(
                "FileChooser.saveInLabelText", "Сохранить в директории");
        UIManager.put(
                "FileChooser.folderNameLabelText", "Путь директории");

        new Main();
    }

}
