/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow.gui.platform;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A custom JPanel that displays a notification with a title, text, and an optional icon.
 * It also supports a close button and a click action for the entire notification panel.
 */
public class NotificationPanel extends JPanel {
    /**
     * The preferred width of the notification panel.
     */
    public static final int NOTIFICATION_WIDTH = 350;

    private final JButton closeButton;
    private final NotificationCenter.Notification notification;
    private final JLabel lblTitle;

    /**
     * Constructs a new {@code NotificationPanel}.
     * @param notification The {@link NotificationCenter.Notification} object containing the details to display.
     * @param closeAction An {@link ActionListener} to be executed when the close button is clicked.
     */
    public NotificationPanel(NotificationCenter.Notification notification, ActionListener closeAction) {
        super(new BorderLayout());

        this.notification = notification;
        notification.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(NotificationCenter.Notification.PROP_COUNTER)) {
                updateTitle();
            }
        });

        JTextArea textArea = new JTextArea(notification.getText());
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setText("1\n2\n3");
        Dimension textSize = textArea.getPreferredSize();
        textArea.setText(compactText(notification.getText()));
        textArea.setCaretPosition(0);
        UISupport.setupPopupMenu(textArea);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(NOTIFICATION_WIDTH, textSize.height + 10));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        closeButton = new JButton("✖");
        closeButton.setToolTipText("Close");
        closeButton.setForeground(Color.GRAY);
        closeButton.setFocusable(false);
        closeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        closeButton.putClientProperty("JButton.squareSize", true);
        closeButton.setMargin(new Insets(2, 2, 2, 2));
        closeButton.addActionListener(closeAction);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 5));
        lblTitle = new JLabel(composeTitle(notification));
        lblTitle.setIcon(notification.getIcon());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        header.add(lblTitle, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        if (notification.getClickAction() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter clickAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (closeAction != null)
                            closeAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                        notification.getClickAction().actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                    }
                }
            };
            addRecursiveClickListener(this, clickAdapter);
        }

        revalidate();
        Dimension size = getPreferredSize();
        setSize(new Dimension(Math.min(NOTIFICATION_WIDTH + 20, size.width), size.height));
    }

    private String compactText(String text) {
        if (text == null || text.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        text.lines().forEach(line -> sb.append(line.trim()).append("\n"));
        // Remove the last newline character if present
        return sb.toString().stripTrailing();
    }

    private static String composeTitle(NotificationCenter.Notification notification) {
        String result = notification.getTitle() != null && !notification.getTitle().isEmpty() ? notification.getTitle() : null;
        if (result == null) {
            result = switch (notification.getType()) {
                case SUCCESS -> "Success";
                case INFORMATION -> "Information";
                case WARNING -> "Warning";
                case ERROR -> "Error";
            };
        }
        if (notification.getCounter() > 1)
            result = "<html>%s <span style= \"color: gray;\">(%s)</span></html>".formatted(result, notification.getCounter());

        return result;
    }

    /**
     * Updates the title of the notification panel based on the current notification's details.
     */
    public void updateTitle() {
        lblTitle.setText(composeTitle(notification));
    }

    private void addRecursiveClickListener(Component component, MouseAdapter adapter) {
        // Don't add the listener to the close button itself
        if (component == closeButton) {
            return;
        }

        component.addMouseListener(adapter);

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addRecursiveClickListener(child, adapter);
            }
        }
    }
}
