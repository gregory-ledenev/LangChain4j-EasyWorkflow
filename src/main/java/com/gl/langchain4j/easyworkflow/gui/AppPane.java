package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class AppPane extends JPanel {
    private final JLayeredPane layeredPane = new JLayeredPane();
    private JComponent content;
    private String placeHolderText;
    private Icon placeHolderIcon;
    private JPanel pnlPlaceHolder;
    private JLabel lblPlaceHolder;
    public AppPane(JComponent content) {
        super(new BorderLayout());

        this.content = content;

        add(layeredPane, BorderLayout.CENTER);
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (Component component : layeredPane.getComponents()) {
                    component.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                }
            }
        });

        if (content != null)
            layeredPane.add(content, JLayeredPane.DEFAULT_LAYER);
    }
    public AppPane() {
        this(null);
    }

    public Icon getPlaceHolderIcon() {
        return placeHolderIcon;
    }

    public JComponent getContent() {
        return content;
    }

    public void setContent(JComponent content) {
        if (this.content != null)
            layeredPane.remove(this.content);

        this.content = content;

        if (this.content != null)
            layeredPane.add(this.content, JLayeredPane.DEFAULT_LAYER);
    }

    public void setPlaceHolderIcon(Icon placeHolderIcon) {
        this.placeHolderIcon = placeHolderIcon;
        if (isPlaceHolderVisible())
            lblPlaceHolder.setIcon(this.placeHolderIcon);
    }

    public String getPlaceHolderText() {
        return placeHolderText;
    }

    public void setPlaceHolderText(String placeHolderText) {
        this.placeHolderText = placeHolderText;
        if (isPlaceHolderVisible())
            lblPlaceHolder.setText(placeHolderText);
    }

    public boolean isPlaceHolderVisible() {
        return pnlPlaceHolder != null && pnlPlaceHolder.isVisible();
    }

    public void setPlaceHolderVisible(boolean isPlaceHolderVisible) {
        if (isPlaceHolderVisible() != isPlaceHolderVisible) {
            if (isPlaceHolderVisible) {
                if (pnlPlaceHolder == null) {
                    pnlPlaceHolder = new JPanel(new GridBagLayout());
                    pnlPlaceHolder.setOpaque(false);
                    lblPlaceHolder = new JLabel();
                    lblPlaceHolder.setForeground(Color.GRAY);
                    pnlPlaceHolder.add(lblPlaceHolder);
                    layeredPane.add(pnlPlaceHolder, JLayeredPane.PALETTE_LAYER);
                }
                lblPlaceHolder.setText(placeHolderText);
                lblPlaceHolder.setIcon(placeHolderIcon);
                pnlPlaceHolder.setVisible(true);
            }
            if (pnlPlaceHolder != null)
                pnlPlaceHolder.setVisible(isPlaceHolderVisible);
        }
    }
}
