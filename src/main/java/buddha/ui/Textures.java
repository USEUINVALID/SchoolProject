package buddha.ui;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.scene.style.Drawable;
import arc.scene.style.NinePatchDrawable;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import buddha.Main;

import static arc.Core.atlas;
import static arc.Core.files;

// Класс для загрузки и хранения текстур
public class Textures {

    public static JsonValue splits;
    public static Drawable white, white_knob, white_rounded, empty, error, button, button_over, button_down, button_disabled, slider_before, slider_after, slider_knob, slider_knob_over, slider_knob_down;

    public static void load() {
        splits = new JsonReader().parse(files.internal("textures/splits.json"));

        white = load("whiteui");
        white_knob = load("whiteui-knob");
        white_rounded = load("whiteui-rounded");

        empty = load("empty");
        error = load("error");

        button = load("button");
        button_over = load("button-over");
        button_down = load("button-down");
        button_disabled = load("button-disabled");

        slider_before = ((NinePatchDrawable) white_knob).tint(Main.active);
        slider_after = ((NinePatchDrawable) white_knob).tint(Color.darkGray);

        slider_knob = ((NinePatchDrawable) white_rounded).tint(Main.active);
        slider_knob_over = ((NinePatchDrawable) white_rounded).tint(Main.active);
        slider_knob_down = ((NinePatchDrawable) white_rounded).tint(Main.active);

        atlas.setErrorRegion("error");
    }

    // Загружает текстуру по названию
    public static Drawable load(String name) {
        var texture = new Texture("textures/" + name + ".png");
        texture.setFilter(TextureFilter.linear); // Настраиваем фильтрацию текстуры

        var region = atlas.addRegion(name, texture, 0, 0, texture.width, texture.height);
        if (splits.has(name)) region.splits = splits.get(name).asIntArray();
        return atlas.drawable(name);
    }
}