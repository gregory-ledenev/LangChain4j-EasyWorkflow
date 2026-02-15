package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.AppDialog;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;

public class ChatPromptsDialog extends AppDialog<ChatPromptsStorage, ChatPromptsStorage.ChatPrompt> {
    private ChatPromptsStorage chatPromptsStorage;
    private JList<ChatPromptsStorage.ChatPrompt> list;
    private DefaultListModel<ChatPromptsStorage.ChatPrompt> listModel;
    private Actions.ActionGroup toolbarActionGroup;
    private ChatPromptsStorage originalChatPromptsStorage;

    /**
     * Constructs a new AppDialog.
     *
     * @param owner The {@link JFrame} from which the dialog is displayed.
     */
    public ChatPromptsDialog(JFrame owner) {
        super(owner, "Chat Prompts");

        init();
    }

    private void init() {
        JPanel content = new JPanel(new BorderLayout());
        content.setPreferredSize(new Dimension(450, 300));
        setMinimumSize(new Dimension(400, 300));
        setMaximumSize(new Dimension(600, 400));

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

        JScrollPane scrollPane = new JScrollPane(list);
        content.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        Actions.BasicAction actionPin = new Actions.StateAction("Pin", new UISupport.AutoIcon(ICON_PIN), null,
                this::togglePinned,
                a -> {
                    ChatPromptsStorage.ChatPrompt selectedValue = list.getSelectedValue();
                    a.setSelected(selectedValue != null && selectedValue.isPinned());
                    a.setEnabled(selectedValue != null);
                });
        actionPin.setCopyName(true);
        actionPin.setShortDescription("Toggle pinned prompt");
        String disableReason = "Disabled because no prompt is selected";
        actionPin.setDisableReason(disableReason);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        actionPin.setAccelerator(keyStroke);
        UISupport.bindAction(list, "pin", keyStroke, actionPin);

        Actions.BasicAction actionMoveUp = new Actions.BasicAction("Move Up", new UISupport.AutoIcon(ICON_UP),
                this::moveUp,
                a -> a.setEnabled(list.getSelectedValue() != null && chatPromptsStorage.canMoveUp(list.getSelectedValue())));
        actionMoveUp.setCopyName(true);
        actionMoveUp.setShortDescription("Move selected prompt up");
        actionMoveUp.setDisableReason(disableReason + " or prompt can't be moved up");
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        actionMoveUp.setAccelerator(keyStroke);
        UISupport.bindAction(list, "moveUp", keyStroke, actionMoveUp);

        Actions.BasicAction actionMoveDown = new Actions.BasicAction("Move Down", new UISupport.AutoIcon(ICON_DOWN),
                this::moveDown,
                a -> a.setEnabled(list.getSelectedValue() != null && chatPromptsStorage.canMoveDown(list.getSelectedValue())));
        actionMoveDown.setCopyName(true);
        actionMoveDown.setShortDescription("Move selected prompt down");
        actionMoveDown.setDisableReason(disableReason + " or prompt can't be moved down");
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        actionMoveDown.setAccelerator(keyStroke);
        UISupport.bindAction(list, "moveDown", keyStroke, actionMoveDown);

        Actions.BasicAction actionDelete = new Actions.BasicAction("Delete", new UISupport.AutoIcon(ICON_DELETE),
                this::delete,
                a -> a.setEnabled(list.getSelectedValue() != null));
        actionDelete.setCopyName(true);
        actionDelete.setShortDescription("Delete selected prompt");
        actionDelete.setDisableReason(disableReason);
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        actionDelete.setAccelerator(keyStroke);
        UISupport.bindAction(list, "delete", keyStroke, actionDelete);

        toolbarActionGroup = new Actions.ActionGroup(
                new Actions.ActionGroup(actionPin),
                new Actions.ActionGroup(actionMoveUp, actionMoveDown),
                new Actions.ActionGroup(actionDelete)
        );
        UISupport.setupToolbar(toolbar, toolbarActionGroup);
        content.add(toolbar, BorderLayout.NORTH);

        setContent(content);
    }

    private void refreshList() {
        ChatPromptsStorage.ChatPrompt selected = list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        listModel.clear();
        for (ChatPromptsStorage.ChatPrompt prompt : chatPromptsStorage.getChatPrompts()) {
            listModel.addElement(prompt);
        }

        if (!listModel.isEmpty()) {
            if (selected != null && listModel.contains(selected)) {
                list.setSelectedValue(selected, true);
            } else if (selectedIndex == -1){
                list.setSelectedIndex(0);
            } else {
                if (selectedIndex >= listModel.size())
                    selectedIndex = listModel.size() - 1;
                list.setSelectedIndex(selectedIndex);
            }
        }

        updateActions();
    }

    private void updateActions() {
        toolbarActionGroup.update();
    }

    private void togglePinned(ActionEvent e) {
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
        this.originalChatPromptsStorage = chatPromptsStorage;

        this.chatPromptsStorage = new ChatPromptsStorage(chatPromptsStorage.getAgentClassName());
        this.chatPromptsStorage.setAutocommit(false);
        this.chatPromptsStorage.replaceChatPrompts(chatPromptsStorage.getChatPrompts()
                .stream()
                .map(ChatPromptsStorage.ChatPrompt::clone)
                .toList());

        refreshList();
    }

    @Override
    protected ChatPromptsStorage.ChatPrompt fromForm() {
        originalChatPromptsStorage.replaceChatPrompts(chatPromptsStorage.getChatPrompts());
        return list.getSelectedValue();
    }
}
