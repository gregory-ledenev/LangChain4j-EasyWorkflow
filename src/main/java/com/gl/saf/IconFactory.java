package com.gl.saf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.gl.saf.UISupport.isDarkAppearance;

@SuppressWarnings("StringConcatenationArgumentToLogCall")
public class IconFactory {
    private static final Map<String, ImageIcon> icons = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(IconFactory.class);

    /**
     * Represents the visual style of an icon.
     */
    public enum IconStyle {
        /** Light theme style. */
        Light,
        /** Dark theme style. */
        Dark,
        /** Style applicable to any theme. */
        Auto;

        /**
         * Returns a list of all concrete styles, excluding the Auto style.
         *
         * @return A list of {@link IconStyle} containing Light and Dark.
         */
        public static List<IconStyle> getStyles() {
            return Arrays.stream(values()).filter(s -> s != Auto).toList();
        }
    }

    /**
     * Represents the physical size category of an icon.
     */
    public enum IconSize {
        /** Standard small icon size (e.g., 16x16). */
        Small,
        /** Standard large icon size (e.g., 32x32). */
        Large,
        /** Size-agnostic or default size. */
        Auto;

        /**
         * Returns a list of all concrete sizes, excluding the Auto size.
         *
         * @return A list of {@link IconSize} containing Small and Large.
         */
        public static List<IconSize> getSizes() {
            return Arrays.stream(values()).filter(s -> s != Auto).toList();
        }
    }

    private static String getIconKey(String iconKey, IconStyle style, IconSize size, boolean selected, boolean rollover) {
        StringBuilder sb = new StringBuilder(iconKey);
        if (style == IconStyle.Dark)
            sb.append("-dark");
        if (size == IconSize.Large)
            sb.append("-large");
        if (selected)
            sb.append("-selected");
        if (rollover)
            sb.append("-rollover");
        return sb.toString();
    }
    /**
     * Loads an icon from the specified class's resources. This method loads two variants of the icon: a standard
     * version and a high-resolution (@2x) version if available. The loaded icons are then stored internally, with a
     * lighter version for light themes and an inverted version for dark themes.
     *
     * @param clazz   The class used to load the resource. This is typically the class where the icon is being used.
     * @param iconKey A unique string key to identify the icon. This key will be used later to retrieve the icon.
     *                Key is encoded as baseName-style-size-selected-rollover. Values style=Style.LIGHT,
     *                size=Size.SMALL, not selected and not rollover must be omitted. Sample is
     *                "home-large-selected-rollover"
     */
    public static void loadIcon(Class<?> clazz, String iconKey) {
        loadIcon(clazz, iconKey, false);
    }

    /**
     * Loads an icon from the specified class's resources. This method loads two variants of the icon: a standard
     * version and a high-resolution (@2x) version if available. The loaded icons are then stored internally, with a
     * optional lighter version for light themes and an inverted version for dark themes.
     *
     * @param clazz            The class used to load the resource. This is typically the class where the icon is being
     *                         used.
     * @param baseIconKey      A unique string key to identify the icon. This key will be used later to retrieve the
     *                         icon.
     * @param preserveOriginal If true, the original icon will be stored without applying theme-based filters
     *                         (lighter/inverted). This is useful for icons that should retain their exact colors
     *                         regardless of the theme.
     */
    public static void loadIcon(Class<?> clazz, String baseIconKey, boolean preserveOriginal) {
        for (IconStyle style : IconStyle.getStyles()) {
            for (IconSize size : IconSize.getSizes()) {
                for (boolean selected : new boolean[] {true, false}) {
                    for (boolean rollover : new boolean[] {true, false}) {
                        String iconKey = getIconKey(baseIconKey, style, size, selected, rollover);
                        List<Image> images = loadImageVariants(clazz, iconKey);
                        if (! images.isEmpty()) {
                            if (preserveOriginal || style == IconStyle.Dark)
                                icons.put(iconKey, new ImageIcon(new BaseMultiResolutionImage(images.toArray(new Image[0]))));
                            else
                                icons.put(iconKey, loadImageIcon(images, UISupport.ImageFilter.Lighter));
                        } else if (! preserveOriginal && style == IconStyle.Dark) {
                            List<Image> lightImages = loadImageVariants(clazz, getIconKey(baseIconKey, IconStyle.Light, size, selected, rollover));
                            if (! lightImages.isEmpty())
                                icons.put(iconKey, loadImageIcon(lightImages, UISupport.ImageFilter.Inverted));
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves an icon using default settings (Auto style, Auto size, unselected, and no rollover).
     *
     * @param iconKey The unique string key for the icon.
     * @return The {@link ImageIcon} matching the criteria, or {@code null} if not found.
     */
    public static ImageIcon getIcon(String iconKey) {
        return getIcon(iconKey, IconStyle.Auto, IconSize.Auto, false, false);
    }

    /**
     * Retrieves a previously loaded icon based on its key and visual properties.
     *
     * @param iconKey  The unique string key used when the icon was loaded.
     * @param style    The visual style (Light, Dark, or Any).
     * @param size     The physical size category (Small, Large, or Any).
     * @param selected Whether the icon is in a selected state.
     * @param rollover Whether the icon is in a rollover (hover) state.
     * @return The {@link ImageIcon} matching the criteria, or {@code null} if not found.
     */
    public static ImageIcon getIcon(String iconKey, IconStyle style, IconSize size, boolean selected, boolean rollover) {
        // 1. Determine the preferred order of styles
        List<IconStyle> styleOrder = new ArrayList<>();
        if (style == IconStyle.Auto) {
            styleOrder.add(isDarkAppearance() ? IconStyle.Dark : IconStyle.Light);
            styleOrder.add(isDarkAppearance() ? IconStyle.Light : IconStyle.Dark);
        } else {
            styleOrder.add(style);
        }

        // 2. Determine the preferred order of sizes
        List<IconSize> sizeOrder = new ArrayList<>();
        if (size == IconSize.Auto) {
            sizeOrder.add(IconSize.Small);
            sizeOrder.add(IconSize.Large);
        } else {
            sizeOrder.add(size);
        }

        // 3. Iterate through Styles first (Highest Priority)
        for (IconStyle currentStyle : styleOrder) {
            // 4. Try exact state match with Size fallback
            for (IconSize currentSize : sizeOrder) {
                ImageIcon result = icons.get(getIconKey(iconKey, currentStyle, currentSize, selected, rollover));
                if (result != null) return result;
            }

            // 5. Try relaxed state (Unselected) with Size fallback
            if (selected) {
                for (IconSize currentSize : sizeOrder) {
                    ImageIcon result = icons.get(getIconKey(iconKey, currentStyle, currentSize, false, rollover));
                    if (result != null) return result;
                }
            }

            // 6. Try relaxed state (Unrollover) with Size fallback
            if (rollover) {
                for (IconSize currentSize : sizeOrder) {
                    // Note: We keep 'selected' as passed, or should we also relax selected here?
                    // Usually rollover implies a hover state. If we relax rollover, we might still want selected.
                    ImageIcon result = icons.get(getIconKey(iconKey, currentStyle, currentSize, selected, false));
                    if (result != null) return result;
                }
            }

            // 7. Try fully relaxed state (Unselected AND Unrollover)
            if (selected && rollover) {
                for (IconSize currentSize : sizeOrder) {
                    ImageIcon result = icons.get(getIconKey(iconKey, currentStyle, currentSize, false, false));
                    if (result != null) return result;
                }
            }
        }

        return null;
    }

    private static ImageIcon loadImageIcon(List<Image> imageVariants, UISupport.ImageFilter imageFilter) {
        List<Image> images = imageVariants;
        switch (imageFilter) {
            case Lighter -> images = imageVariants.stream()
                    .map(image -> createFilteredImage(image, new GrayFilter(true, 45)))
                    .map(image -> new ImageIcon(image).getImage())
                    .toList();
            case Inverted -> images = imageVariants.stream()
                    .map(image -> createFilteredImage(image, new UISupport.InvertFilter()))
                    .map(image -> new ImageIcon(image).getImage())
                    .toList();
        }

        return new ImageIcon(new BaseMultiResolutionImage(images.toArray(new Image[0])));
    }

    private static List<Image> loadImageVariants(Class<?> clazz, String name) {
        Image image1 = null;
        Image image2 = null;

        URL resource1 = clazz.getResource(name + ".png");
        if (resource1 != null)
            image1 = new ImageIcon(resource1).getImage();
        else
            logger.debug("No image resource: %s.png".formatted(name));

        if (! (image1 instanceof MultiResolutionImage)) {
            URL resource2 = clazz.getResource(name + "@2x.png");
            if (resource2 != null)
                image2 = new ImageIcon(resource2).getImage();
            else
                logger.debug("No image resource: %s@2x.png".formatted(name));
        }

        if (image1 == null && image2 == null)
            return List.of();

        List<Image> images;
        if (image1 instanceof MultiResolutionImage multi) {
            images = multi.getResolutionVariants();
        } else {
            images = new ArrayList<>();
            if (image1 != null)
                images.add(image1);
            if (image2 != null)
                images.add(image2);
        }
        return images;
    }

    private static Image createFilteredImage(Image i, RGBImageFilter filter) {
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

    /**
     * An Icon implementation that automatically loads the correct icon based on the current theme (light/dark).
     * It delegates all visual properties to the underlying {@link ImageIcon} retrieved via {@link IconFactory#getIcon(String)}.
     */
    public static class AutoIcon extends ImageIcon {
        private final String key;
        private final IconSize size;
        private final boolean selected;
        private final boolean rollover;
        private boolean darkAppearance;
        private ImageIcon icon;

        /**
         * Creates an AutoIcon with default settings (Auto size, unselected, no rollover).
         *
         * @param aKey The base key for the icon.
         */
        public AutoIcon(String aKey) {
            this(aKey, IconSize.Auto, false, false);
        }

        /**
         * Creates an AutoIcon with specified size and state.
         *
         * @param aKey The base key for the icon.
         * @param size The desired {@link IconSize}.
         */
        public AutoIcon(String aKey, IconSize size) {
            this(aKey, size, false, false);
        }

        /**
         * Creates an AutoIcon with specified size and state.
         *
         * @param aKey     The base key for the icon.
         * @param size     The desired {@link IconSize}.
         * @param selected Whether the icon is in a selected state.
         * @param rollover Whether the icon is in a rollover state.
         */
        public AutoIcon(String aKey, IconSize size, boolean selected, boolean rollover) {
            this.key = aKey;
            this.size = size;
            this.selected = selected;
            this.rollover = rollover;
        }

        /**
         * Returns the key associated with this icon.
         *
         * @return The icon key.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the image of the icon currently matching the theme.
         *
         * @return The {@link Image} for the current theme.
         */
        @Override
        public Image getImage() {
            return getIcon().getImage();
        }

        /**
         * Retrieves the actual {@link ImageIcon} from the factory based on the current UI state.
         *
         * @return The resolved {@link ImageIcon}.
         */
        public ImageIcon getIcon() {
            if (this.darkAppearance != UISupport.isDarkAppearance() || this.icon == null) {
                this.darkAppearance = UISupport.isDarkAppearance();
                this.icon = IconFactory.getIcon(key, IconStyle.Auto, size, selected, rollover);
                if (this.icon == null)
                    logger.error("No icon for key: %s".formatted(key));
            }
            return icon;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            getIcon().paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return getIcon().getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return getIcon().getIconHeight();
        }
    }
}
