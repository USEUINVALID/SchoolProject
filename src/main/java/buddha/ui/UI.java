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
import arc.scene.ui.layout.WidgetGroup;
import arc.util.*;
import buddha.compressor.AverageColorCompressor;
import buddha.compressor.FractalCompressor;
import buddha.utils.FileChooserFilter;

import javax.swing.*;

import static arc.Core.*;
import static buddha.compressor.AverageColorCompressor.blockSize;

public class UI implements ApplicationListener {

    public final WidgetGroup root = new WidgetGroup();
    public final Fi tempFile = Fi.tempFile("compressor");

    public Fi selected;
    public JFileChooser chooser;

    public Image source, result;
    public Label sourceSize, resultSize;

    @Override
    public void init() {
        input.addProcessor(scene);
        scene.add(root);

        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        root.fill(table -> {
            table.top();

            source = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();
            result = table.image(Textures.error).scaling(Scaling.fit).pad(32f, 32f, 32f, 32f).size(320f).get();

            table.row();

            sourceSize = table.add("").get();
            resultSize = table.add("").get();
        });

        root.fill(table -> {
            table.center();

            // TODO
        });

        root.fill(table -> {
            table.bottom();

            table.label(() -> "Размер блока: [yellow]" + blockSize).labelAlign(Align.center).touchable(Touchable.disabled).labelAlign(Align.left).left().row();
            table.slider(1, 128, 1, blockSize, value -> blockSize = (int) value).padTop(24f).width(240f).align(Align.left);


            table.row();

            table.label(() -> "Уровень сжатия: [yellow]ничего не делает").labelAlign(Align.center).touchable(Touchable.disabled).padTop(32f).labelAlign(Align.left).left().row();
            table.slider(2, 16, 1, 0, value -> {}).padTop(24f).width(240f).align(Align.left);

            table.row();

            table.button("Выбрать файл", this::showFileChooser).disabled(button -> chooser != null && chooser.isShowing()).center().width(320f).height(80f).pad(24f, 0f, 24f, 6f);
            table.button("Сжать изображение", this::compressImage).disabled(button -> selected == null || !selected.exists()).center().width(320f).height(80f).pad(24f, 6f, 24f, 0f);
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
        chooser.cancelSelection();
    }

    public void showFileChooser() {
        Threads.daemon(() -> {
            chooser = new JFileChooser(OS.userHome);
            chooser.setDialogTitle("Выберите изображение для сжатия...");

            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new FileChooserFilter("Compressible image (png, jpg, jpeg)", "png", "jpg", "jpeg"));
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int option = chooser.showOpenDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return;

            app.post(() -> updateSelected(new Fi(chooser.getSelectedFile())));
        });
    }

    public void compressImage() {
        Threads.daemon(() -> {
            try {
                var result = FractalCompressor.compress(new Pixmap(selected));
                app.post(() -> updateResult(result));
            } catch (Exception e) {
                Log.err(e);
            }
        });
    }

    public void updateSelected(Fi selected) {
        this.selected = selected;

        updateImage(source, sourceSize, "Размер до:[yellow]", new Pixmap(selected));

        this.result.setDrawable(Textures.error);
        this.resultSize.setText("");
    }

    public void updateResult(Pixmap pixmap) {
        updateImage(result, resultSize, "Размер после:[yellow]", pixmap);
    }

    public void updateImage(Image image, Label label, String text, Pixmap pixmap) {
        try {
            image.setDrawable(new TextureRegion(new Texture(pixmap)));
            label.setText(text + " " + getSize(pixmap));
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