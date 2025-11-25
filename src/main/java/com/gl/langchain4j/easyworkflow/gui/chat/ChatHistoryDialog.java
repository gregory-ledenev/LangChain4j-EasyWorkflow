package com.gl.langchain4j.easyworkflow.gui.chat;

import com.gl.langchain4j.easyworkflow.gui.ChatHistoryStorage;
import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.AppDialog;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.KEY_SESSION_UID;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.ICON_CHAT;
import static java.text.DateFormat.SHORT;

/**
 * A dialog for displaying and managing chat history.
 * Allows users to view, open, and delete past chat sessions.
 * Extends {@link AppDialog} to provide a standard application dialog framework.
 */
public class ChatHistoryDialog extends AppDialog<List<ChatHistoryStorage.ChatHistoryItem>, ChatHistoryStorage.ChatHistoryItem> {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryDialog.class);
    private static final String ACTION_COMMAND_DELETE = "delete";
    private final ChatHistoryStorage chatHistoryStorage;

    private JList<ChatHistoryStorage.ChatHistoryItem> list;
    private DefaultListModel<ChatHistoryStorage.ChatHistoryItem> model;
    private Actions.BasicAction deleteAction;
    private boolean deletionInProgress = false;

    /**
     * Constructs a new ChatHistoryDialog.
     *
     * @param owner              The parent frame of the dialog.
     * @param chatHistoryStorage The storage for chat history items.
     */
    public ChatHistoryDialog(JFrame owner, ChatHistoryStorage chatHistoryStorage) {
        super(owner, "Chat History");
        this.chatHistoryStorage = chatHistoryStorage;
        init();
    }

    private static boolean isToday(Date date) {
        Calendar currentCalendar = Calendar.getInstance();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return currentCalendar.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH) &&
                currentCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                currentCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR);
    }

    private static String getRequestText(ChatHistoryStorage.ChatHistoryItem item, int maxLength) {
        String result = getRequestText(item);
        return result.length() > maxLength ? result.substring(0, maxLength) + "..." : result;
    }

    private static String getRequestText(ChatHistoryStorage.ChatHistoryItem item) {
        String result = null;
        ChatMessage chatMessage = null;
        for (ChatMessage message : item.chatMessages()) {
            if (message.type() == ChatMessage.Type.User) {
                chatMessage = message;
                break;
            }
        }
        if (chatMessage != null) {
            result = chatMessage.message();
            if (chatMessage.rawMessage() instanceof Map<?, ?> map) {
                map = new HashMap<>(map);
                map.remove(KEY_SESSION_UID);
                result = map.size() == 1 ? map.values().stream().findFirst().get().toString() : map.toString();
            } else if (chatMessage.rawMessage() instanceof List<?> list) {
                result = list.size() == 1 ? list.get(0).toString() : list.toString();
            }
        }
        return result;
    }

    private static String getResponseText(ChatHistoryStorage.ChatHistoryItem item) {
        ChatMessage chatMessage = null;
        for (int i = item.chatMessages().size() - 1; i >= 0; i--) {
            if (!item.chatMessages().get(i).outgoing()) {
                chatMessage = item.chatMessages().get(i);
                break;
            }
        }
        return chatMessage != null ? chatMessage.message() : null;
    }

    private void init() {
        list = new JList<>() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        list.setCellRenderer(new ChatHistoryItemRenderer());
        list.getSelectionModel().addListSelectionListener(e -> updateButtons());
        UISupport.bindDoubleClickAction(list, new Actions.BasicAction(null, null, aActionEvent -> close(ACTION_COMMAND_OK)));
        JScrollPane content = new JScrollPane(list);
        content.setPreferredSize(new Dimension(450, 300));
        setContent(content);

        deleteAction = new Actions.BasicAction("Delete Chart", null,
                e -> deleteSelectedChat(),
                a -> a.setEnabled(list.getSelectedValue() != null && !deletionInProgress));
        UISupport.bindAction(list, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), deleteAction);
        UISupport.bindAction(list, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), deleteAction);
        JButton btnDelete = new JButton(deleteAction);
        btnDelete.setActionCommand(ACTION_COMMAND_DELETE);
        btnDelete.setForeground(Color.RED);
        addButton(btnDelete, true);

        getButton(ACTION_COMMAND_OK).setText("Open Chat");
        getButton(ACTION_COMMAND_CANCEL).setText("Close");
    }

    private void updateButtons() {
        boolean e = list.getSelectedValue() != null;
        getButton(ACTION_COMMAND_OK).setEnabled(e);
        deleteAction.update();
    }

    private void deleteSelectedChat() {
        ChatHistoryStorage.ChatHistoryItem selectedValue = list.getSelectedValue();
        if (selectedValue != null &&
                question(null, "Are you sure you want to delete a '%s' chat?".formatted(getRequestText(selectedValue, 20))) == JOptionPane.YES_OPTION) {
            final JButton btnDelete = getButton(ACTION_COMMAND_DELETE);
            boolean focused = btnDelete.isFocusOwner();
            deletionInProgress = true;
            updateButtons();
            CompletableFuture.supplyAsync(() -> chatHistoryStorage.deleteChatHistoryItem(selectedValue.uid())).
                    whenComplete((result, ex) -> {
                        if (result) {
                            int selectedIndex = list.getSelectedIndex();
                            model.removeElement(selectedValue);
                            if (model.isEmpty())
                                selectedIndex = -1;
                            else if (selectedIndex >= model.size())
                                selectedIndex = model.size() - 1;
                            if (selectedIndex > -1)
                                list.setSelectedIndex(selectedIndex);
                        } else if (ex != null) {
                            logger.error("Failed to delete a chat", ex);
                        }
                        deletionInProgress = false;
                        updateButtons();
                        if (focused)
                            btnDelete.requestFocus();
                    });
        }
    }

    @Override
    protected ChatHistoryStorage.ChatHistoryItem fromForm() {
        return list.getSelectedValue();
    }

    @Override
    protected void toForm(List<ChatHistoryStorage.ChatHistoryItem> data) {
        model = new DefaultListModel<>();
        for (ChatHistoryStorage.ChatHistoryItem item : data)
            model.addElement(item);
        list.setModel(model);
        if (!model.isEmpty())
            list.setSelectedIndex(0);
    }

    static class ChatHistoryItemRenderer extends JPanel implements ListCellRenderer<ChatHistoryStorage.ChatHistoryItem> {
        private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(SHORT, SHORT);
        private static final DateFormat timeFormat = DateFormat.getTimeInstance(SHORT);
        private final JLabel lblDate;
        private final JLabel lblRequest;
        private final JLabel lblResponse;
        private final JLabel lblCounter;

        private static final Icon ICON_CHAT_DARK = UISupport.getIcon(ICON_CHAT, false);
        private static final Icon ICON_CHAT_LIGHT = UISupport.getIcon(ICON_CHAT, true);

        public ChatHistoryItemRenderer() {
            setLayout(new BorderLayout());

            Box content = new Box(BoxLayout.Y_AXIS);
            content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            add(content, BorderLayout.CENTER);

            Box header = new Box(BoxLayout.X_AXIS);
            lblDate = new JLabel();
            lblDate.setFont(lblDate.getFont().deriveFont(Font.BOLD));
            header.add(lblDate);
            header.add(Box.createHorizontalGlue());
            lblCounter = new JLabel(new UISupport.AutoIcon(ICON_CHAT));
            header.add(lblCounter);
            content.add(header);

            lblRequest = new JLabel();
            content.add(lblRequest);
            lblResponse = new JLabel();
            content.add(lblResponse);
        }

        /**
         * Returns a component that has been configured to display the specified value.
         *
         * @see ListCellRenderer#getListCellRendererComponent
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends ChatHistoryStorage.ChatHistoryItem> list, ChatHistoryStorage.ChatHistoryItem value, int index, boolean isSelected, boolean cellHasFocus) {
            lblDate.setText(isToday(value.date()) ? timeFormat.format(value.date()) : dateFormat.format(value.date()));
            lblCounter.setText(String.valueOf(value.chatMessages().size()));
            lblRequest.setText(getRequestText(value));
            lblResponse.setText(getResponseText(value));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                lblDate.setForeground(list.getSelectionForeground());
                lblCounter.setForeground(list.getSelectionForeground());
                lblCounter.setIcon(cellHasFocus ? ICON_CHAT_LIGHT : ICON_CHAT_DARK);
                lblRequest.setForeground(list.getSelectionForeground());
                lblResponse.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                lblDate.setForeground(list.getForeground());
                lblCounter.setForeground(list.getForeground());
                lblCounter.setIcon(ICON_CHAT_DARK);
                Color subTitleForeground = UISupport.isDarkAppearance() ? Color.LIGHT_GRAY : Color.GRAY;
                lblRequest.setForeground(subTitleForeground);
                lblResponse.setForeground(subTitleForeground);
            }

            return this;
        }
    }
}
