package buddha.ui;

import arc.ApplicationListener;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ProgressBar;
import arc.scene.ui.ProgressBar.ProgressBarStyle;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.Seq;
import arc.util.*;
import buddha.compressor.AverageColorCompressor;
import buddha.compressor.Compressor;
import buddha.compressor.FractalCompressor;
import buddha.utils.FileChooser;
import buddha.utils.FileChooserFilter;

import javax.swing.*;

import static arc.Core.*;

public class UI implements ApplicationListener {

    public final WidgetGroup root = new WidgetGroup();
    public final Fi tempFile = Fi.tempFile("compressor");

    public Fi source;
    public JFileChooser load;

    public Pixmap result;
    public JFileChooser save;

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
                    .get();

            table.row();
            table.label(() -> Strings.autoFixed(current.progress(), 2) + "%").visible(() -> current.compressing).padLeft(385f);
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
            load.addChoosableFileFilter(new FileChooserFilter("Compressible image (png, jpg, jpeg)", "png", "jpg", "jpeg"));
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
            save.addChoosableFileFilter(new FileChooserFilter("PNG image (png)", "png"));
            save.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = save.showSaveDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return;

            app.post(() -> saveResult(new Fi(save.getSelectedFile())));
        });
    }

    public void updateSelected(Fi selected) {
        this.source = selected;
        updateImage(sourceImage, sourceSize, "Размер до: [yellow]", new Pixmap(selected));

        this.resultImage.setDrawable(Textures.error);
        this.resultSize.setText("");
    }

    public void updateResult(Pixmap result) {
        this.result = result;
        updateImage(resultImage, resultSize, "Размер после: [yellow]", result);
    }

    public void saveResult(Fi selected) {
        PixmapIO.writePng(selected, result);
        app.openFolder(selected.absolutePath()); // TODO
    }

    public void updateImage(Image image, Label label, String text, Pixmap pixmap) {
        try {
            image.setDrawable(new TextureRegion(new Texture(pixmap)));
            label.setText(text + getSize(pixmap));
        } catch (Exception e) {
            image.setDrawable(Textures.error);
            label.setText("[scarlet]Ошибка получения данных файла");
        }
    }

    public String getSize(Pixmap pixmap) {
        PixmapIO.writePng(tempFile, pixmap);
        long size = tempFile.length();

        return size > 1024 * 1024 ? Strings.autoFixed(size / 1024f / 1024f, 2) + " мбайт" : Strings.autoFixed(size / 1024f, 2) + " кбайт";
    }
}