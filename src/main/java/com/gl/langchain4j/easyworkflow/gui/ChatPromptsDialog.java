package com.gl.langchain4j.easyworkflow.gui;

import com.gl.appframework.IconFactory;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.AppDialog;
import com.gl.appframework.UISupport;
import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.actions.StateAction;
import com.gl.appframework.comp.ActionToolBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.gl.appframework.IconFactory.*;
import static com.gl.appframework.ToolbarIcons.*;

public class ChatPromptsDialog extends AppDialog<ChatPromptsStorage, ChatPromptsStorage.ChatPrompt> {
    private ChatPromptsStorage chatPromptsStorage;
    private JList<ChatPromptsStorage.ChatPrompt> list;
    private DefaultListModel<ChatPromptsStorage.ChatPrompt> listModel;
    private ActionGroup toolbarActionGroup;
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

        JScrollPane scrollPane = new JScrollPane(list);
        content.add(scrollPane, BorderLayout.CENTER);

        ActionToolBar toolbar = new ActionToolBar();
        toolbar.setFloatable(false);

        BasicAction actionPin = new StateAction("Pin", new AutoIcon(ICON_PIN), null,
                this::togglePinned,
                a -> {
                    ChatPromptsStorage.ChatPrompt selectedValue = list.getSelectedValue();
                    a.setSelected(selectedValue != null && selectedValue.isPinned());
                    a.setEnabled(selectedValue != null);
                });
        actionPin.setRetainName(true);
        actionPin.setShortDescription("Toggle pinned prompt");
        String disableReason = "Disabled because no prompt is selected";
        actionPin.setDisableReason(disableReason);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        actionPin.setAccelerator(keyStroke);
        UISupport.bindAction(list, "pin", keyStroke, actionPin);

        BasicAction actionMoveUp = new BasicAction("Move Up", new AutoIcon(ICON_UP),
                this::moveUp,
                a -> a.setEnabled(list.getSelectedValue() != null && chatPromptsStorage.canMoveUp(list.getSelectedValue())));
        actionMoveUp.setRetainName(true);
        actionMoveUp.setShortDescription("Move selected prompt up");
        actionMoveUp.setDisableReason(disableReason + " or prompt can't be moved up");
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        actionMoveUp.setAccelerator(keyStroke);
        UISupport.bindAction(list, "moveUp", keyStroke, actionMoveUp);

        BasicAction actionMoveDown = new BasicAction("Move Down", new AutoIcon(ICON_DOWN),
                this::moveDown,
                a -> a.setEnabled(list.getSelectedValue() != null && chatPromptsStorage.canMoveDown(list.getSelectedValue())));
        actionMoveDown.setRetainName(true);
        actionMoveDown.setShortDescription("Move selected prompt down");
        actionMoveDown.setDisableReason(disableReason + " or prompt can't be moved down");
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        actionMoveDown.setAccelerator(keyStroke);
        UISupport.bindAction(list, "moveDown", keyStroke, actionMoveDown);

        BasicAction actionDelete = new BasicAction("Delete", new AutoIcon(ICON_DELETE),
                this::delete,
                a -> a.setEnabled(list.getSelectedValue() != null));
        actionDelete.setRetainName(true);
        actionDelete.setShortDescription("Delete selected prompt");
        actionDelete.setDisableReason(disableReason);
        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        actionDelete.setAccelerator(keyStroke);
        UISupport.bindAction(list, "delete", keyStroke, actionDelete);

        toolbarActionGroup = new ActionGroup(
                new ActionGroup(actionPin),
                new ActionGroup(actionMoveUp, actionMoveDown),
                new ActionGroup(actionDelete)
        );
        toolbar.setActionGroup(toolbarActionGroup);
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
    }

    @Override
    public void update() {
        super.update();
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
            chatPromptsStorage.removeChatPrompt(selected);
            refreshList();
        }
    }

    @Override
    protected void toForm(ChatPromptsStorage chatPromptsStorage) {
        this.originalChatPromptsStorage = chatPromptsStorage;
        this.chatPromptsStorage = chatPromptsStorage.clone();

        refreshList();
    }

    @Override
    protected ChatPromptsStorage.ChatPrompt fromForm() {
        originalChatPromptsStorage.replaceChatPrompts(chatPromptsStorage.getChatPrompts());
        return list.getSelectedValue();
    }
}
