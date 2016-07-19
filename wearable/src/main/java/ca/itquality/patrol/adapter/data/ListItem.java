package ca.itquality.patrol.adapter.data;

/**
 * Immutable model class for an ListItem.
 */
public class ListItem {

    public String title;
    public int image;
    public int background;

    public ListItem(String title, int image, int background) {
        this.title = title;
        this.image = image;
        this.background = background;
    }

    public String getTitle() {
        return title;
    }

    public int getImage() {
        return image;
    }

    public int getBackground() {
        return background;
    }
}
