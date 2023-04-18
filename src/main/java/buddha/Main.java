package buddha;

import arc.ApplicationCore;
import arc.Files.FileType;
import arc.backend.sdl.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.Scene;
import arc.util.*;
import buddha.ui.*;

import javax.swing.*;

import static arc.Core.*;

public final class Main extends ApplicationCore {

    public static final Color active = Color.valueOf("#007fff");
    public static final UI ui = new UI();

    public static void main(String[] args) {
        // Создаем новое SDL приложение
        new SdlApplication(new Main(), new SdlConfig() {{
            // Настраиваем заголовок окна
            title = "Image Compressor v0.1-pre-alpha";

            // Настраиваем размеры окна
            width = 800;
            height = 800;

            // Выключаем изменение размера окна
            resizable = false;
            // Выключаем аудио-модуль, он нам не понадобится
            disableAudio = true;

            // Задаем иконку окна
            setWindowIcon(FileType.internal, "textures/error.png");
        }});
    }

    @Override
    public void setup() {
        Time.mark();

        // Создаем пакет отсортированных спрайтов, сцену и атлас
        batch = new SortedSpriteBatch();
        scene = new Scene();
        atlas = TextureAtlas.blankAtlas();

        // Загружаем текстуры, слои и стили
        Textures.load();
        Fonts.load();
        Styles.load();

        // Добавляем UI как один из модулей приложения: он будет инициализироваться, обновляться и выключаться
        add(ui);

        // Устанавливаем внешний вид swing-компонентов на системный
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Log.info("Total time to load: @ms", Time.elapsed());
    }
}