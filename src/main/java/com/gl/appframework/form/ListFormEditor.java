package com.gl.appframework.form;

import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.UISupport;
import com.gl.appframework.actions.BasicAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static com.gl.appframework.ToolbarIcons.*;

public class ListFormEditor extends JPanel implements FormEditor<List<Object>> {
    private final ListFormElement formElement;
    private JList<Object> list = new JList<>();
    private DefaultListModel<Object> listModel;
    private ActionGroup toolbarActionGroup;
    private List<Object> value;

    public ListFormEditor(FormPanel formPanel, ListFormElement formElement) {
        super(new BorderLayout(5, 2));
        this.formElement = formElement;
        init();
    }

    private void init() {
        setPreferredSize(new Dimension(400, 300));

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addListSelectionListener(e -> updateActions());

        JScrollPane scrollPane = new JScrollPane(list);
        this.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        String elementDisplayName = formElement.getElementDisplayName().isEmpty() ? "item" : formElement.getElementDisplayName();
        ActionGroup moveActionGroup = new ActionGroup();
        if (formElement.capabilities.contains(ListFormProperty.ListCapability.REORDER)) {
            BasicAction actionMoveUp = new BasicAction("Move Up", new UISupport.AutoIcon(ICON_UP),
                    this::moveUp,
                    a -> a.setEnabled(list.getSelectedValue() != null && list.getSelectedIndex() > 0));
            actionMoveUp.setShortDescription("Move selected %s up".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionMoveUp.setAccelerator(keyStroke);
            UISupport.bindAction(list, "moveUp", keyStroke, actionMoveUp);

            BasicAction actionMoveDown = new BasicAction("Move Down", new UISupport.AutoIcon(ICON_DOWN),
                    this::moveDown,
                    a -> a.setEnabled(list.getSelectedValue() != null && list.getSelectedIndex() < listModel.size() - 1));
            actionMoveDown.setShortDescription("Move selected %s down".formatted(elementDisplayName));
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionMoveDown.setAccelerator(keyStroke);
            UISupport.bindAction(list, "moveDown", keyStroke, actionMoveDown);

            moveActionGroup.addAction(actionMoveUp);
            moveActionGroup.addAction(actionMoveDown);
        }

        ActionGroup editActionGroup = new ActionGroup();
        if (formElement.capabilities.contains(ListFormProperty.ListCapability.ADD)) {
            BasicAction actionAdd = new BasicAction("Add", new UISupport.AutoIcon(ICON_PLUS),
                    this::add,
                    a -> a.setEnabled(true));
            actionAdd.setShortDescription("Add new %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionAdd.setAccelerator(keyStroke);
            UISupport.bindAction(list, "add", keyStroke, actionAdd);
            editActionGroup.addAction(actionAdd);
        }

        if (formElement.capabilities.contains(ListFormProperty.ListCapability.EDIT)) {
            BasicAction actionEdit = new BasicAction("Edit", new UISupport.AutoIcon(ICON_COMPOSE),
                    this::edit,
                    a -> a.setEnabled(list.getSelectedValue() != null));
            actionEdit.setShortDescription("Edit selected %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            actionEdit.setAccelerator(keyStroke);
            UISupport.bindAction(list, "edit", keyStroke, actionEdit);
            UISupport.bindDoubleClickAction(list, actionEdit);
            editActionGroup.addAction(actionEdit);
        }

        if (formElement.capabilities.contains(ListFormProperty.ListCapability.DELETE)) {
            BasicAction actionDelete = new BasicAction("Delete", new UISupport.AutoIcon(ICON_DELETE),
                    this::delete,
                    a -> a.setEnabled(list.getSelectedValue() != null));
            actionDelete.setShortDescription("Delete selected %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            actionDelete.setAccelerator(keyStroke);
            UISupport.bindAction(list, "delete", keyStroke, actionDelete);
            editActionGroup.addAction(actionDelete);
        }

        toolbarActionGroup = new ActionGroup(
                editActionGroup,
                moveActionGroup
        );
        UISupport.setupToolbar(toolbar, toolbarActionGroup);

        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        JLabel titleLabel = new JLabel(formElement.getLabel(), formElement.getIcon(), JLabel.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(toolbar, BorderLayout.EAST);
        this.add(headerPanel, BorderLayout.NORTH);
    }

    private void edit(ActionEvent actionEvent) {
        Object selectedValue = list.getSelectedValue();
        if (selectedValue != null) {
            String title = "Edit " + formElement.getElementDisplayName();
            FormDialog<Object> dialog = createFormDialog(title);
            Object result = dialog.executeModal(selectedValue);
            if (result != null) {
                int index = list.getSelectedIndex();
                value.set(index, result);
                refreshList();
            }
        }
    }

    private void add(ActionEvent actionEvent) {
        try {
            Object newItem = formElement.getElementClass().getDeclaredConstructor().newInstance();
            FormDialog<Object> dialog = createFormDialog("Add " + formElement.getElementDisplayName());
            Object result = dialog.executeModal(newItem);
            if (result != null) {
                value.add(result);
                refreshList();
                list.setSelectedValue(result, true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new instance of " + formElement.getElementClass(), e);
        }
    }

    private FormDialog<Object> createFormDialog(String title) {
        JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(this);
        return dialog != null ?
                new FormDialog<>(dialog, title, formElement.getElementClass()) :
                new FormDialog<>((JFrame) SwingUtilities.getWindowAncestor(this), title, formElement.getElementClass());
    }

    private void updateActions() {
        toolbarActionGroup.update();
    }

    private void moveUp(ActionEvent e) {
        int index = list.getSelectedIndex();
        if (index > 0) {
            Object item = listModel.remove(index);
            listModel.add(index - 1, item);

            refreshList();
            updateActions();
        }
    }

    private void moveDown(ActionEvent e) {
        int index = list.getSelectedIndex();
        if (index != -1 && index < listModel.size() - 1) {
            Object item = value.remove(index);
            value.add(index + 1, item);

            refreshList();
            updateActions();
        }
    }

    private void delete(ActionEvent e) {
        int index = list.getSelectedIndex();
        if (index != -1) {
            value.remove(index);

            refreshList();
            updateActions();
        }
    }

    private void refreshList() {
        Object selected = list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        listModel.clear();
        for (Object prompt : value)
            listModel.addElement(prompt);

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

    @Override
    public void setValue(List<Object> value) {
        this.value = new ArrayList<>(value);
        refreshList();
    }

    @Override
    public List<Object> getValue() {
        return this.value;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        return ! formElement.isMandatory() || ! value.isEmpty() ? null : "The list should not be empty";
    }

    @Override
    public boolean needLabel() {
        return false;
    }
}
