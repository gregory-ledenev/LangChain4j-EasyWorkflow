/*
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gl.saf.actions;

import com.gl.saf.*;
import com.gl.saf.IconFactory.AutoIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static com.gl.saf.Appearance.applyAppearance;
import static com.gl.saf.ApplicationPreferences.getApplicationPreferences;
import static com.gl.saf.Icons.*;

/**
 * Provides factory methods for creating standard UI actions such as Cut, Copy, Paste, etc.
 */
@SuppressWarnings("MagicConstant")
public class StandardActions {
    private final static int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    /**
     * Creates a standard "Cut" delegate action with the appropriate icon, mnemonic, and accelerator key.
     *
     * @return an {@link Action} configured for cutting content.
     */
    public static Action createCutAction() {
        BasicAction result = new DelegateAction("cut", "Cut", new AutoIcon(ICON_CUT));
        result.setMnemonic('x');
        result.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask));
        return result;
    }

    /**
     * Creates a standard "Copy" delegate action with the appropriate icon, mnemonic, and accelerator key.
     *
     * @return an {@link Action} configured for copying content.
     */
    public static Action createCopyAction() {
        BasicAction result = new DelegateAction("copy", "Copy", new AutoIcon(ICON_COPY));
        result.setMnemonic('c');
        result.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask));
        return result;
    }

    /**
     * Creates a standard "Paste" delegate action with the appropriate icon, mnemonic, and accelerator key.
     *
     * @return an {@link Action} configured for pasting content.
     */
    public static Action createPasteAction() {
        BasicAction result = new DelegateAction("paste", "Paste", new AutoIcon(ICON_PASTE));
        result.setMnemonic('v');
        result.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask));
        return result;
    }

    /**
     * Creates a standard "Delete" delegate action with a spacer icon, mnemonic, and the Delete key accelerator.
     *
     * @return an {@link Action} configured for deleting content.
     */
    public static Action createDeleteAction() {
        BasicAction result = new DelegateAction("delete", "Delete", new AutoIcon(ICON_SPACER));
        result.setMnemonic('d');
        result.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        return result;
    }

    /**
     * Creates a standard "Exit" action that terminates the application.
     *
     * @return an {@link Action} configured to exit the application.
     */
    public static Action createExitAction() {
        BasicAction result = new BasicAction("Exit", null, e -> Application.getSharedApplication().exit(false));
        result.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
        return result;
    }

    /**
     * Creates a standard "About" action that displays application information.
     *
     * @return an {@link Action} configured to show the About dialog.
     */
    public static Action createAboutAction() {
        return new BasicAction("About...", new AutoIcon(ICON_HELP),
                e -> {
                    AppFrame activeAppFrame = AppFrame.getActiveAppFrame();
                    if (Application.getSharedApplication().getAboutProvider() != null)
                        Application.getSharedApplication().getAboutProvider().showAbout(activeAppFrame);
                },
                basicAction -> basicAction.setEnabled(Application.getSharedApplication().getAboutProvider() != null));
    }

    /**
     * Creates a standard "Visit Site" action that opens the application's website.
     *
     * @return an {@link Action} configured to visit the application's website.
     */
    public static Action createVisitLinkAction(AboutProvider.AboutLink link) {
        return new BasicAction("Visit '%s'...".formatted(link.title()),
                new AutoIcon(ICON_GLOBE),
                e -> {
                    if (Application.getSharedApplication().getAboutProvider() != null)
                        Application.getSharedApplication().getAboutProvider().openLink(link);
                },
                basicAction -> basicAction.setEnabled(Application.getSharedApplication().getAboutProvider() != null));
    }

    /**
     * Creates an action group for switching between Light, Dark, and Auto appearance modes.
     *
     * @return an {@link ActionGroup} containing appearance state actions.
     */
    public static ActionGroup createAppearanceActionGroup() {
        String exclusiveGroup = "appearance";
        return new ActionGroup("Appearance", new AutoIcon(ICON_SPACER), true,
                new StateAction("Light", null, exclusiveGroup,
                        e -> applyAppearance(Appearance.Type.Light),
                        a -> a.setSelected(getApplicationPreferences().getAppearance() == Appearance.Type.Light)),
                new StateAction("Dark", null, exclusiveGroup,
                        e -> applyAppearance(Appearance.Type.Dark),
                        a -> a.setSelected(getApplicationPreferences().getAppearance() == Appearance.Type.Dark)),
                new StateAction("Auto", null, exclusiveGroup,
                        e -> applyAppearance(Appearance.Type.Auto),
                        a -> a.setSelected(getApplicationPreferences().getAppearance() == Appearance.Type.Auto))
        );
    }
}
