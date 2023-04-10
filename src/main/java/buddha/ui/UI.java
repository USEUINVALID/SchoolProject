package buddha.ui;

import arc.ApplicationListener;
import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.ui.*;
import arc.scene.ui.ProgressBar.ProgressBarStyle;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import buddha.compressor.*;
import buddha.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static arc.Core.*;

public class UI implements ApplicationListener {

    public final WidgetGroup root = new WidgetGroup();
    public final Fi temp = Fi.tempFile("compressor");

    public Fi source;
    public Pixmap result;

    public JFileChooser load, save;

    public Compressor average = new AverageColorCompressor();
    public Compressor fractal = new FractalCompressor();

    public final Seq<Compressor> compressors = Seq.with(average, fractal);
    public Compressor current = average;

    public Table currentTable;

    public Image sourceImage, resultImage;
    public Label sourceSize, resultSize;

    @Override
    public void init() {
        input.addProcessor(scene);
        scene.add(root);

        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        root.fill(table -> {
            table.top();

            sourceImage = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();
            resultImage = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();

            table.row();

            sourceSize = table.add("").get();
            resultSize = table.add("").visible(() -> !current.compressing).get();
        });

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

        root.fill(table -> {
            table.top().left();
            table.margin(488f, 80f, 0f, 0f);

            current.build(table);
            currentTable = table;
        });

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

        root.fill(table -> {
            table.bottom();

            table.button("Выбрать файл", this::showLoadDialog).disabled(button -> load != null && load.isShowing()).center().width(220f).height(80f).pad(24f, 0f, 24f, 4f);
            table.button("Сжать изображение", this::compressImage).disabled(button -> source == null || !source.exists() || current.compressing).center().width(220f).height(80f).pad(24f, 4f, 24f, 4f);
            table.button("Сохранить результат", this::showSaveDialog).disabled(button -> result == null || (save != null && save.isShowing())).center().width(220f).height(80f).pad(24f, 4f, 24f, 0f);
        });
    }

    @Override
    public void update() {
        graphics.clear(Color.gray);

        scene.act();
        scene.draw();
    }

    @Override
    public void resize(int width, int height) {
        scene.resize(width, height);
    }

    @Override
    public void dispose() {
        if (load != null)
            load.cancelSelection();

        if (save != null)
            save.cancelSelection();
    }

    public void showLoadDialog() {
        Threads.daemon(() -> {
            load = new FileChooser(OS.userHome, "Выберите изображение для сжатия...", "textures/error.png");

            load.setAcceptAllFileFilterUsed(false);
            load.addChoosableFileFilter(new FileChooserFilter("Image to load (png, jpg, jpeg)", "png", "jpg", "jpeg"));
            load.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = load.showOpenDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return;

            app.post(() -> updateSelected(new Fi(load.getSelectedFile())));
        });
    }

    public void compressImage() {
        Threads.daemon(() -> {
            try {
                var result = current.compress(new Pixmap(source));
                app.post(() -> updateResult(result));
            } catch (Exception e) {
                Log.err(e);
            }
        });
    }

    public void showSaveDialog() {
        Threads.daemon(() -> {
            save = new FileChooser(OS.userHome, "Выберите файл для сохранения...", "textures/error.png");

            save.setAcceptAllFileFilterUsed(false);
            save.addChoosableFileFilter(new FileChooserFilter("Image to save (" + source.extension() + ")", source.extension()));
            save.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = save.showSaveDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return;

            app.post(() -> saveResult(new Fi(save.getSelectedFile())));
        });
    }

    public void updateSelected(Fi selected) {
        this.source = selected;
        updateImage(sourceImage, sourceSize, "Размер до: [yellow]", selected);

        this.resultImage.setDrawable(Textures.error);
        this.resultSize.setText("");
    }

    public void updateResult(Pixmap result) {
        this.result = result;
        updateImage(resultImage, resultSize, "Размер после: [yellow]", result);
    }

    public void saveResult(Fi selected) {
        try {
            var file = selected.exists() ? selected : selected.sibling(selected.nameWithoutExtension() + "." + source.extension());
            saveImage(result, file, source.extension());

            app.openFolder(file.absolutePath());
        } catch (Exception e) {
            Log.err(e);
        }
    }

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

    public long saveImage(Pixmap pixmap, Fi file, String extension) throws IOException {
        var image = new BufferedImage(pixmap.width, pixmap.height, BufferedImage.TYPE_INT_RGB);
        pixmap.each((x, y) -> image.setRGB(x, y, Tmp.c1.set(pixmap.get(x, y)).rgb888()));

        ImageIO.write(image, extension, file.file());
        return file.length();
    }

    public String getSize(Pixmap pixmap) throws IOException {
        return getSize(saveImage(pixmap, temp, source.extension()));
    }

    public String getSize(long size) {
        return size > 1024 * 1024 ? Strings.autoFixed(size / 1024f / 1024f, 2) + " мбайт" : Strings.autoFixed(size / 1024f, 2) + " кбайт";
    }
}