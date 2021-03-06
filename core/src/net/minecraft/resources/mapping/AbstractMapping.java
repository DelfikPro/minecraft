package net.minecraft.resources.mapping;

import lombok.Getter;
import lombok.ToString;

@ToString
public abstract class AbstractMapping<T> implements Mapping{
    @Getter
    public final String address;

    /**
     * Через Mapping можно заменять существующие понятия.
     * В случае замены старое значение сохраняется в этом поле,
     * после чего его можно восстановить методом undo().
     */
    @Getter
    public final T overridden;

    @Getter
    public final T actual;

    public AbstractMapping(String address, T overridden, T actual) {
        this.address = address;
        this.overridden = overridden;
        this.actual = actual;
    }

    /**
     * Код, который должен выполняться при мапе/анмапе этого маппинга,
     * находится в реализации этого метода
     *
     * @param element Элемент, который нужно замаппить. Может быть null, тогда элемент надо удалить.
     */
    protected abstract void map(T element);

    @Override
    public void apply() {
        map(actual);
    }

    @Override
    public void revert() {
        map(overridden);
    }
}
