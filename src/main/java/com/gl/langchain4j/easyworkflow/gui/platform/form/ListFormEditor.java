package com.gl.langchain4j.easyworkflow.gui.platform.form;

import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.ICON_DELETE;

public class ListFormEditor extends JPanel implements FormEditor<List<Object>> {
    private final FormPanel formPanel;
    private final ListFormElement formElement;
    private JList<Object> list = new JList<>();
    private DefaultListModel<Object> listModel;
    private Actions.ActionGroup toolbarActionGroup;
    private List<Object> value;

    public ListFormEditor(FormPanel formPanel, ListFormElement formElement) {
        super(new BorderLayout(5, 5));
        this.formPanel = formPanel;
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
        Actions.ActionGroup moveActionGroup = new Actions.ActionGroup();
        if (formElement.capabilities.contains(ListFormProperty.ListCapability.REORDER)) {
            Actions.BasicAction actionMoveUp = new Actions.BasicAction("Move Up", new UISupport.AutoIcon(ICON_UP),
                    this::moveUp,
                    a -> a.setEnabled(list.getSelectedValue() != null && list.getSelectedIndex() > 0));
            actionMoveUp.setShortDescription("Move selected %s up".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionMoveUp.setAccelerator(keyStroke);
            UISupport.bindAction(list, "moveUp", keyStroke, actionMoveUp);

            Actions.BasicAction actionMoveDown = new Actions.BasicAction("Move Down", new UISupport.AutoIcon(ICON_DOWN),
                    this::moveDown,
                    a -> a.setEnabled(list.getSelectedValue() != null && list.getSelectedIndex() < listModel.size() - 1));
            actionMoveDown.setShortDescription("Move selected %s down".formatted(elementDisplayName));
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionMoveDown.setAccelerator(keyStroke);
            UISupport.bindAction(list, "moveDown", keyStroke, actionMoveDown);

            moveActionGroup.addAction(actionMoveUp);
            moveActionGroup.addAction(actionMoveDown);
        }

        Actions.ActionGroup editActionGroup = new Actions.ActionGroup();
        if (formElement.capabilities.contains(ListFormProperty.ListCapability.ADD)) {
            Actions.BasicAction actionAdd = new Actions.BasicAction("Add", new UISupport.AutoIcon(ICON_PLUS),
                    this::add,
                    a -> a.setEnabled(true));
            actionAdd.setShortDescription("Add new %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            actionAdd.setAccelerator(keyStroke);
            UISupport.bindAction(list, "add", keyStroke, actionAdd);
            editActionGroup.addAction(actionAdd);
        }

        if (formElement.capabilities.contains(ListFormProperty.ListCapability.EDIT)) {
            Actions.BasicAction actionEdit = new Actions.BasicAction("Edit", new UISupport.AutoIcon(ICON_COMPOSE),
                    this::edit,
                    a -> a.setEnabled(list.getSelectedValue() != null));
            actionEdit.setShortDescription("Edit selected %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            actionEdit.setAccelerator(keyStroke);
            UISupport.bindAction(list, "edit", keyStroke, actionEdit);
            editActionGroup.addAction(actionEdit);
        }

        if (formElement.capabilities.contains(ListFormProperty.ListCapability.DELETE)) {
            Actions.BasicAction actionDelete = new Actions.BasicAction("Delete", new UISupport.AutoIcon(ICON_DELETE),
                    this::delete,
                    a -> a.setEnabled(list.getSelectedValue() != null));
            actionDelete.setShortDescription("Delete selected %s".formatted(elementDisplayName));
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            actionDelete.setAccelerator(keyStroke);
            UISupport.bindAction(list, "delete", keyStroke, actionDelete);
            editActionGroup.addAction(actionDelete);
        }

        toolbarActionGroup = new Actions.ActionGroup(
                editActionGroup,
                moveActionGroup
        );
        UISupport.setupToolbar(toolbar, toolbarActionGroup);
        this.add(toolbar, BorderLayout.NORTH);
    }

    private void edit(ActionEvent actionEvent) {

    }

    private void add(ActionEvent actionEvent) {

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
}
