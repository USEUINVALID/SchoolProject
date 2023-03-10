package buddha.ui;

import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.scene.style.Drawable;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import static arc.Core.atlas;
import static arc.Core.files;

public class Textures {

    public static JsonValue splits;
    public static Drawable white, error, button, button_over, button_down, button_disabled;

    public static void load() {
        splits = new JsonReader().parse(files.internal("textures/splits.json"));

        white = load("whiteui");
        error = load("error");

        button = load("button");
        button_over = load("button-over");
        button_down = load("button-down");
        button_disabled = load("button-disabled");

        atlas.setErrorRegion("error");
    }

    public static Drawable load(String name) {
        var texture = new Texture("textures/" + name + ".png");
        texture.setFilter(TextureFilter.linear); // for better experience

        var region = atlas.addRegion(name, texture, 0, 0, texture.width, texture.height);
        if (splits.has(name)) region.splits = splits.get(name).asIntArray();
        return atlas.drawable(name);
    }
}