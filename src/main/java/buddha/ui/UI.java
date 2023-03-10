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
import arc.util.OS;
import buddha.compressor.FractalCompression;
import buddha.compressor.RasterImage;
import buddha.utils.FileChooserFilter;

import javax.swing.*;
import java.io.File;

import static arc.Core.*;

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
            source = table.image(Textures.error).center().pad(64f).size(300f).get();
            result = table.image(Textures.error).center().pad(64f).size(300f).get();

            table.row();

            table.button("Выберите файл...", () -> {
                var chooser = new JFileChooser(OS.userHome);
                chooser.setDialogTitle("Выберите изображение для сжатия...");

                chooser.setAcceptAllFileFilterUsed(false);
                chooser.addChoosableFileFilter(new FileChooserFilter("Compressible image (png, jpg, jpeg)", "png", "jpg", "jpeg"));
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int option = chooser.showOpenDialog(null);
                if (option != JFileChooser.APPROVE_OPTION) return;

                updateSelected(new Fi(chooser.getSelectedFile()));
            }).center().width(192f).height(120f).grow();

            table.button("Сжать", () -> {
                try {
                    var image = FractalCompression.encode(new RasterImage(selected));
                    updateResult(image.toPixmap());
                } catch (Exception e) {
                    // TODO выводить ошибку
                }
            }).disabled(button -> selected == null || !selected.exists()).center().width(192f).height(120f).grow();;
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

    public void updateSelected(Fi selected) {
        this.selected = selected;

        this.source.setDrawable(new TextureRegion(new Texture(new Pixmap(selected))));
        this.result.setDrawable(Textures.error);
    }

    public void updateResult(Pixmap result) {
        this.result.setDrawable(new TextureRegion(new Texture(result)));
    }
}