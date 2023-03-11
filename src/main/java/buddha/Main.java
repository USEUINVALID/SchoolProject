package buddha;

import arc.ApplicationCore;
import arc.Files.FileType;
import arc.backend.sdl.SdlApplication;
import arc.backend.sdl.SdlConfig;
import arc.graphics.g2d.SortedSpriteBatch;
import arc.graphics.g2d.TextureAtlas;
import arc.scene.Scene;
import arc.util.Log;
import arc.util.Time;
import buddha.ui.Fonts;
import buddha.ui.Styles;
import buddha.ui.Textures;
import buddha.ui.UI;

import javax.swing.*;

import static arc.Core.*;

public final class Main extends ApplicationCore {

    public static void main(String[] args) {
        new SdlApplication(new Main(), new SdlConfig() {{
            title = "Image Compressor v0.1-pre-alpha";

            width = 800;
            height = 800;

            resizable = false;
            disableAudio = true;

            setWindowIcon(FileType.internal, "test.png");
        }});
    }


    @Override
    public void setup() {
        Time.mark();

        batch = new SortedSpriteBatch();
        scene = new Scene();
        atlas = TextureAtlas.blankAtlas();

        Textures.load();
        Fonts.load();
        Styles.load();

        add(new UI());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Log.info("Total time to load: @ms", Time.elapsed());
    }
}