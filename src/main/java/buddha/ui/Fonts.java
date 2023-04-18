package buddha.ui;

import arc.freetype.FreeTypeFontGenerator;
import arc.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import arc.graphics.Color;
import arc.graphics.g2d.Font;

import static arc.Core.*;

// Класс для загрузки и хранения шрифтов
public class Fonts {

    public static Font font;

    public static void load() {
        font = load("font.woff", new FreeTypeFontParameter() {{
            size = 18;
            shadowColor = Color.darkGray;
            shadowOffsetY = 2;
            incremental = true;
        }});

        font.getData().markupEnabled = true;
    }

    // Загружает шрифт по названию
    public static Font load(String name, FreeTypeFontParameter parameter) {
        var generator = new FreeTypeFontGenerator(files.internal("fonts/" + name));
        return generator.generateFont(parameter);
    }
}