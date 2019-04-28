package net.minecraft.client.gui.settings.tabs.element;

public interface Element {

	void render(float mx, float my);

	default void mouseDown(int mx, int my, int button) {}
	default void mouseUp(int mx, int my, int button) {}
	default void mouseDrag(int mx, int my, int button, long timeSinceLastClick) {}

}