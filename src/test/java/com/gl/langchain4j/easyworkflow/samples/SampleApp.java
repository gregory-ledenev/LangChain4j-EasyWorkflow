package com.gl.langchain4j.easyworkflow.samples;

import com.gl.appframework.*;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.comp.ActionMenuBar;
import com.gl.langchain4j.easyworkflow.gui.Icons;
import com.gl.langchain4j.easyworkflow.gui.ToolbarIcons;

import javax.swing.*;
import java.awt.*;

import static com.gl.appframework.UISupport.applyAppearance;

public class SampleApp {
    static {
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    public static void main(String[] args) {
        Icons.loadIcons();
        ToolbarIcons.loadIcons();
        com.gl.appframework.ToolbarIcons.loadIcons();

        Application.getSharedApplication().launch(new SampleFrame());
    }

    static class SampleFrame extends AppFrame {

        public SampleFrame() throws HeadlessException {
            super("SampleFrame");

            setTitle("Sample App");
            setSize(800, 600);
            setLocationRelativeTo(null);

            installAppModule(new WorkflowScreen());
            installAppModule(new ExecutionScreen());

            getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).
                    addAction(getAppScreenManager().map(AppScreenManager::getAppScreenManagerActionGroup).orElse(null));

            installAppModule(new MenuBarModule<SampleFrame>(new ActionGroup(
//                    getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_FILE),
//                    getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_EDIT),
//                    getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW),
                    getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_OPTIONS)
//                    getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_HELP)
            )));
        }
    }

    static class WorkflowScreen extends BasicAppScreen<SampleFrame> {
        private final ActionGroup actionGroup = new ActionGroup(
                new  BasicAction("Workflow command", null, e -> System.out.println("Workflow command"))
        );

        public WorkflowScreen() {
            super("workflow", "Workflow", new UISupport.AutoIcon(ToolbarIcons.ICON_WORKFLOW), "Shows Workflow");

            add(new JLabel("Some workflow content"));
        }

        @Override
        public void passivate() {
            super.passivate();
            getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).removeAction(actionGroup);
        }

        @Override
        public void activate() {
            super.activate();

            getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).addAction(actionGroup);
        }
    }

    static class ExecutionScreen extends BasicAppScreen<SampleFrame> {
        private final ActionGroup actionGroup = new ActionGroup(
                new  BasicAction("Execution command", null, e -> System.out.println("Execution command"))
        );

        public ExecutionScreen() {
            super("execution", "Execution", new UISupport.AutoIcon(ToolbarIcons.ICON_EXECUTION_FLOW), "Shows Execution");

            add(new JLabel("Some execution content"));
        }

        @Override
        public void passivate() {
            super.passivate();
            getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).removeAction(actionGroup);
        }

        @Override
        public void activate() {
            super.activate();

            getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).addAction(actionGroup);
        }
    }
}
