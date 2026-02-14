package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.AppDialog;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ChatPromptsDialog extends AppDialog<ChatPromptsStorage, ChatPromptsStorage.ChatPrompt> {
    private ChatPromptsStorage chatPromptsStorage;
    private JList<ChatPromptsStorage.ChatPrompt> list;
    private DefaultListModel<ChatPromptsStorage.ChatPrompt> listModel;
    private Actions.BasicAction actionPin;
    private Actions.BasicAction actionMoveUp;
    private Actions.BasicAction actionMoveDown;
    private Actions.BasicAction actionDelete;

    /**
     * Constructs a new AppDialog.
     *
     * @param owner The {@link JFrame} from which the dialog is displayed.
     */
    public ChatPromptsDialog(JFrame owner) {
        super(owner, "Chat Prompts");

        setContent(createContent());
    }

    private JComponent createContent() {
        setMinimumSize(new Dimension(400, 300));
        setMaximumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(600, 400));
        JPanel panel = new JPanel(new BorderLayout());

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChatPromptsStorage.ChatPrompt prompt) {
                    setText(prompt.toHtmlString());
                }
                return this;
            }
        });

        list.addListSelectionListener(e -> updateActions());
        UISupport.bindDoubleClickAction(list, new Actions.BasicAction("Select", null, e -> close(ACTION_COMMAND_OK)));

        JScrollPane scrollPane = UISupport.createScrollPane(list, true);
        panel.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        actionPin = new Actions.BasicAction("Pin", null, this::pinUnpin);
        actionMoveUp = new Actions.BasicAction("Move Up", null, this::moveUp);
        actionMoveDown = new Actions.BasicAction("Move Down", null, this::moveDown);
        actionDelete = new Actions.BasicAction("Delete", null, this::delete);

        toolbar.add(UISupport.createToolbarButton(actionPin));
        toolbar.add(UISupport.createToolbarButton(actionMoveUp));
        toolbar.add(UISupport.createToolbarButton(actionMoveDown));
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(UISupport.createToolbarButton(actionDelete));

        panel.add(toolbar, BorderLayout.NORTH);

        return panel;
    }

    private void refreshList() {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        listModel.clear();
        for (ChatPromptsStorage.ChatPrompt prompt : chatPromptsStorage.getChatPrompts()) {
            listModel.addElement(prompt);
        }
        if (selected != null && listModel.contains(selected)) {
            list.setSelectedValue(selected, true);
        } else if (!listModel.isEmpty() && list.getSelectedIndex() == -1) {
            list.setSelectedIndex(0);
        }
        updateActions();
    }

    private void updateActions() {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        boolean hasSelection = selected != null;

        actionPin.setEnabled(hasSelection);
        actionDelete.setEnabled(hasSelection);

        if (hasSelection) {
            actionMoveUp.setEnabled(chatPromptsStorage.canMoveUp(selected));
            actionMoveDown.setEnabled(chatPromptsStorage.canMoveDown(selected));
            actionPin.putValue(Action.NAME, selected.isPinned() ? "Unpin" : "Pin");
        } else {
            actionMoveUp.setEnabled(false);
            actionMoveDown.setEnabled(false);
            actionPin.putValue(Action.NAME, "Pin");
        }
    }

    private void pinUnpin(ActionEvent e) {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        if (selected != null) {
            chatPromptsStorage.setPinned(selected, !selected.isPinned());
            refreshList();
        }
    }

    private void moveUp(ActionEvent e) {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        if (selected != null) {
            chatPromptsStorage.moveUp(selected);
            refreshList();
            list.setSelectedValue(selected, true);
        }
    }

    private void moveDown(ActionEvent e) {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        if (selected != null) {
            chatPromptsStorage.moveDown(selected);
            refreshList();
            list.setSelectedValue(selected, true);
        }
    }

    private void delete(ActionEvent e) {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        if (selected != null) {
            if (question("Delete Prompt", "Are you sure you want to delete this prompt?") == JOptionPane.YES_OPTION) {
                chatPromptsStorage.removeChatPrompt(selected);
                refreshList();
            }
        }
    }

    @Override
    protected void toForm(ChatPromptsStorage chatPromptsStorage) {
        this.chatPromptsStorage = chatPromptsStorage;
        refreshList();
    }

    @Override
    protected ChatPromptsStorage.ChatPrompt fromForm() {
        return list.getSelectedValue();
    }
}
