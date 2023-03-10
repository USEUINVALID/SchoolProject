package buddha.ui;

import arc.graphics.Color;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextButton.TextButtonStyle;

import static arc.Core.scene;

public class Styles {

    public static void load() {
        scene.addStyle(ButtonStyle.class, new ButtonStyle());

        scene.addStyle(TextButtonStyle.class, new TextButtonStyle() {{
            font = Fonts.font;
            fontColor = Color.white;
            disabledFontColor = Color.gray;

            down = Textures.button_down;
            up = Textures.button;
            over = Textures.button_over;
            disabled = Textures.button_disabled;
        }});

        scene.addStyle(ImageButtonStyle.class, new ImageButtonStyle() {{
            imageDownColor = Color.green;
            imageOverColor = Color.lightGray;
            imageDisabledColor = Color.gray;

            down = Textures.button_down;
            up = Textures.button;
            over = Textures.button_over;
            disabled = Textures.button_disabled;
        }});

        scene.addStyle(LabelStyle.class, new LabelStyle() {{
            font = Fonts.font;
            fontColor = Color.white;
        }});
    }
}