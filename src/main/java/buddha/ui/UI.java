package buddha.ui;

import arc.ApplicationListener;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Align;
import arc.util.Log;
import arc.util.OS;
import arc.util.Threads;
import buddha.encoder.Compressor;
import buddha.utils.FileChooserFilter;

import javax.swing.*;

import static arc.Core.*;
import static buddha.encoder.Compressor.*;

public class UI implements ApplicationListener {

    public final WidgetGroup root = new WidgetGroup();

    public Fi selected;
    public Image source, result;

    @Override
    public void init() {
        input.addProcessor(scene);
        scene.add(root);

        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        root.fill(table -> {
            source = table.image(Textures.error).center().pad(48f, 48f, 48f, 48f).size(300f).get();
            result = table.image(Textures.error).center().pad(48f, 48f, 48f, 48f).size(300f).get();

            table.row();

            table.table(sliders -> {
                sliders.label(() -> "Размер блока: [yellow]" + domainBlocksSize).labelAlign(Align.center).touchable(Touchable.disabled).labelAlign(Align.left).left().row();
                sliders.slider(1, 32, 1, domainBlocksSize, value -> domainBlocksSize = (int) value).padTop(24f).width(240f);

                sliders.row();

                sliders.label(() -> "Ранговых блоков в доменном: [yellow]" + rangeBlocksPerDomain).labelAlign(Align.center).touchable(Touchable.disabled).padTop(32f).labelAlign(Align.left).left().row();
                sliders.slider(2, 16, 1, rangeBlocksPerDomain, value -> rangeBlocksPerDomain = (int) value).padTop(24f).width(240f);
            }).center();

            table.row();

            table.button("Выбрать файл", this::showFileChooser).center().width(320f).height(80f).padTop(40f);
            table.button("Сжать изображение", this::compressImage).disabled(button -> selected == null || !selected.exists()).center().width(320f).height(80f).padTop(40f);
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

    public void showFileChooser() {
        Threads.daemon(() -> {
            var chooser = new JFileChooser(OS.userHome);
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
                var result = Compressor.compress(Compressor.scale(new Pixmap(selected)));
                app.post(() -> updateResult(result));
            } catch (Exception e) {
                Log.err(e);
            }
        });
    }

    public void updateSelected(Fi selected) {
        this.selected = selected;

        this.source.setDrawable(new TextureRegion(new Texture(new Pixmap(selected))));
        this.result.setDrawable(Textures.error);
    }

    public void updateResult(Pixmap result) {
        this.result.setDrawable(new TextureRegion(new Texture(result)));
    }
}