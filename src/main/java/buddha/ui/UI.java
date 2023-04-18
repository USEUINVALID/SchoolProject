package buddha.ui;

import arc.ApplicationListener;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.*;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.*;
import arc.scene.ui.ProgressBar.ProgressBarStyle;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import buddha.compressor.*;
import buddha.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static arc.Core.*;

// Класс, хранящий, загружающий, обновляющий и выключающий все элементы интерфейса
public class UI implements ApplicationListener {

    public final WidgetGroup root = new WidgetGroup();      // Корневой элемент интерфейса
    public final Fi temp = Fi.tempFile("compressor"); // Временный файл для конвертации изображений

    public JFileChooser load; // Окно для выбора изображения для загрузки
    public JFileChooser save; // Окно для выбора файла для сохранения

    public Fi source;     // Файл, из которого было загружено последнее изображение
    public Pixmap result; // Последнее сжатое изображение

    // Все доступные алгоритмы сжатия
    public final Seq<Compressor> compressors = Seq.with(new AverageColorCompressor(), new FractalCompressor());
    public Compressor current = compressors.first(); // Выбранный алгорим сжатия

    // Элементы, нужные для правильного отображения надписей, изображений и слайдеров
    public Table currentTable;
    public Image sourceImage, resultImage;
    public Label sourceSize, resultSize;

    // Вызывается при запуске приложения
    @Override
    public void init() {
        // Создаем окно для выбора изображения для загрузки
        load = new FileChooser(OS.userHome, "Выберите изображение для сжатия...", "textures/error.png");
        load.setAcceptAllFileFilterUsed(false);

        // Создаем окно для выбора файла для сохранения
        save = new FileChooser(OS.userHome, "Выберите файл для сохранения...", "textures/error.png");
        save.setAcceptAllFileFilterUsed(false);

        input.addProcessor(scene); // Делаем, чтобы сцена принимала нажатия клавиш
        scene.add(root);           // Добавляем корневой элемент на сцену

        // Задаем некоторые параметры корневому элементу
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        // Добавляем панель с изображениями и их размером
        root.fill(table -> {
            table.top();

            sourceImage = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();
            resultImage = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();

            table.row();

            sourceSize = table.add("").get();
            resultSize = table.add("").visible(() -> !current.compressing).get();
        });

        // Добавляем панель с прогрессом сжатия
        root.fill(table -> {
            table.center();
            table.add(new ProgressBar(0f, 100f, 0.1f, false, scene.getStyle(ProgressBarStyle.class)))
                    .visible(() -> current.compressing)
                    .update(bar -> bar.setValue(current.progress()))
                    .width(330f)
                    .padLeft(385f)
                    .row();

            table.label(() -> Strings.autoFixed(current.progress(), 2) + "%").visible(() -> current.compressing).padLeft(385f).get();
        });

        // Добавляем панель с элементами выбранного алгоритма сжатия
        root.fill(table -> {
            table.top().left();
            table.margin(488f, 80f, 0f, 0f);

            current.build(currentTable = table);
        });

        // Добавляем панель с кнопками для выбора алгоритма сжатия
        root.fill(table -> {
            table.center().left();
            table.margin(320f, 410f, 0f, 0f);

            table.label(() -> "Алгоритм сжатия: [yellow]" + current.name).labelAlign(Align.left).padBottom(16f).left().row();

            compressors.each(compressor -> table.button(compressor.name, Styles.checkTextButton, () -> {
                current = compressor;

                currentTable.clear();
                current.build(currentTable);
            }).checked(button -> current == compressor).disabled(button -> current.compressing).width(320f).padTop(8f).left().row());
        });

        // Добавляем панель с основными кнопками
        root.fill(table -> {
            table.bottom();

            table.button("Выбрать файл", this::showLoadDialog).disabled(button -> load.isShowing()).center().width(220f).height(80f).pad(24f, 0f, 24f, 4f);
            table.button("Сжать изображение", this::compressImage).disabled(button -> source == null || !source.exists() || current.compressing).center().width(220f).height(80f).pad(24f, 4f, 24f, 4f);
            table.button("Сохранить результат", this::showSaveDialog).disabled(button -> result == null || save.isShowing()).center().width(220f).height(80f).pad(24f, 4f, 24f, 0f);
        });
    }

    // Вызывается при обновлении приложения
    @Override
    public void update() {
        // Заливаем фон приложения серым цветом
        graphics.clear(Color.gray);

        // Обновляем сцену и все ее элементы
        scene.act();

        // Отрисовываем сцену и все ее элементы
        scene.draw();
    }

    // Вызывается при изменении размеров окна
    @Override
    public void resize(int width, int height) {
        scene.resize(width, height);
    }

    // Вызывается при выключении приложения
    @Override
    public void dispose() {
        // Отменяем выбор файлов
        load.cancelSelection();
        save.cancelSelection();
    }

    // Показывает окно для выбора изображения для загрузки
    public void showLoadDialog() {
        // Создаем отдельный поток
        Threads.daemon(() -> {
            load.addChoosableFileFilter(new FileChooserFilter("Image to load (png, jpg, jpeg)", "png", "jpg", "jpeg"));
            load.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = load.showOpenDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return; // Проверяем, выбрал ли пользователь файл

            app.post(() -> updateSelected(new Fi(load.getSelectedFile())));
        });
    }

    // Сжимает изображение
    public void compressImage() {
        Threads.daemon(() -> {
            var result = current.compress(new Pixmap(source));
            app.post(() -> updateResult(result));
        });
    }

    // Показывает окно для выбора файла для сохранения
    public void showSaveDialog() {
        // Создаем отдельный поток
        Threads.daemon(() -> {
            save.addChoosableFileFilter(new FileChooserFilter("Image to save (" + source.extension() + ")", source.extension()));
            save.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = save.showSaveDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return; // Проверяем, выбрал ли пользователь файл

            app.post(() -> {
                var file = new Fi(save.getSelectedFile());
                saveResult(file.sibling(file.nameWithoutExtension() + "." + source.extension()));
            });
        });
    }

    // Обновляет выбранное изображение и его размер
    public void updateSelected(Fi selected) {
        this.source = selected;
        updateImage(sourceImage, sourceSize, "Размер до: [yellow]", selected);

        this.resultImage.setDrawable(Textures.error);
        this.resultSize.setText("");
    }

    // Обновляет сжатое изображение и его размер
    public void updateResult(Pixmap result) {
        this.result = result;
        updateImage(resultImage, resultSize, "Размер после: [yellow]", result);
    }

    // Сохраняет сжатое изображение в выбранный файл
    public void saveResult(Fi selected) {
        try {
            var file = selected.exists() ? selected : selected.sibling(selected.nameWithoutExtension() + "." + source.extension());
            saveImage(result, file, source.extension());

            // Открываем изображение в системном редакторе
            Desktop.getDesktop().open(selected.file());
        } catch (Exception e) {
            Log.err(e);
        }
    }

    // Обновляет нужное изображение и его размер
    public void updateImage(Image image, Label label, String text, Fi file) {
        try {
            var texture = new Texture(file);
            texture.setFilter(TextureFilter.linear);

            image.setDrawable(new TextureRegion(texture));
            label.setText(text + getSize(file.length()));
        } catch (Exception e) {
            image.setDrawable(Textures.error);
            label.setText("[scarlet]Ошибка получения данных файла");
        }
    }

    // Обновляет нужное изображение и его размер
    public void updateImage(Image image, Label label, String text, Pixmap pixmap) {
        try {
            var texture = new Texture(pixmap);
            texture.setFilter(TextureFilter.linear);

            image.setDrawable(new TextureRegion(texture));
            label.setText(text + getSize(pixmap));
        } catch (Exception e) {
            image.setDrawable(Textures.error);
            label.setText("[scarlet]Ошибка получения данных файла");
        }
    }

    // Сохраняет изображение в выбранный файл с выбранным расширением и возвращает размер файла в байтах
    public long saveImage(Pixmap pixmap, Fi file, String extension) throws IOException {
        var image = new BufferedImage(pixmap.width, pixmap.height, BufferedImage.TYPE_INT_RGB);
        pixmap.each((x, y) -> image.setRGB(x, y, Tmp.c1.set(pixmap.get(x, y)).rgb888()));

        ImageIO.write(image, extension, file.file());
        return file.length();
    }

    // Возвращает читабельный размер изображения как строку
    public String getSize(Pixmap pixmap) throws IOException {
        return getSize(saveImage(pixmap, temp, source.extension()));
    }

    // Возвращает читабельный размер изображения как строку
    public String getSize(long size) {
        return size > 1024 * 1024 ? Strings.autoFixed(size / 1024f / 1024f, 2) + " мбайт" : Strings.autoFixed(size / 1024f, 2) + " кбайт";
    }
}
