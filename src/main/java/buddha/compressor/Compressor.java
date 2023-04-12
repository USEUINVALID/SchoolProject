package buddha.compressor;

import arc.graphics.Pixmap;
import arc.scene.ui.layout.Table;

// Абстрактный компрессор, сам по себе ничего не делает
public abstract class Compressor {
    public final String name;

    public boolean compressing;
    public float current, total;

    // Создает новый компрессор с заданным именем
    public Compressor(String name) {
        this.name = name;
    }

    // Сжимает изображение
    public abstract Pixmap compress(Pixmap pixmap);

    // Визуально начинает сжатие
    public void start(float total) {
        this.compressing = true;
        this.current = 0f;
        this.total = total;
    }

    // Возвращает прогресс сжатия в процентах
    public float progress() {
        return compressing ? current / total * 100f : 0f;
    }

    // Визуально заканчивает сжатие
    public void end() {
        this.compressing = false;
        this.current = 0f;
        this.total = 0f;
    }

    // Создает нужные для этого компрессора надписи и слайдеры
    public abstract void build(Table table);
}