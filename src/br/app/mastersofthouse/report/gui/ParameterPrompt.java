
package br.app.mastersofthouse.report.gui;

import net.sf.jasperreports.engine.JRParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParameterPrompt {

    Component parent;
    JRParameter[] jrParameters;
    Map params;
    JScrollPane scrollPane;
    String reportName;
    AtomicBoolean valid = new AtomicBoolean();

    public ParameterPrompt(Component parent, JRParameter[] jrParameters,
            Map<String, Object> params, String reportName, boolean isForPromptingOnly,
            boolean isUserDefinedOnly, boolean emptyOnly) {

        this.valid.set(true);
        this.parent = parent;
        this.jrParameters = jrParameters;
        this.params = params;
        this.reportName = reportName;

        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(600, 250));

        for (JRParameter param : jrParameters) {
            if (!param.isSystemDefined() || !isUserDefinedOnly) {
                if (param.isForPrompting() || !isForPromptingOnly) {
                    if (params.get(param.getName()) == null || !emptyOnly) {
                        panel.add(new ParameterPanel(param, params, this.valid));
                    }
                }
            }
        }
        panel.add(new javax.swing.JSeparator());

        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addPropertyChangeListener(
                "focusOwner", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!(evt.getNewValue() instanceof JComponent)) {
                    return;
                }
                JComponent focused = (JComponent) evt.getNewValue();
                if (panel.isAncestorOf(focused)) {
                    JComponent myComponent = (JComponent) focused.getParent().getParent();
                    myComponent.scrollRectToVisible(new Rectangle(0, 0, 0, 80));
                }
            }
        });
    }

    /**
     * <p>show.</p>
     *
     * @return a int.
     */
    public int show() {

        final JOptionPane optionPane = new JOptionPane(scrollPane,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        final JDialog dialog;
        JFrame frame = new JFrame();
        if (parent == null) {
            frame.setTitle("Report - Parameter Prompt: " + reportName);
            frame.setUndecorated(true);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            dialog = new JDialog(frame);
        } else if (parent instanceof Window) {
            dialog = new JDialog((Window) parent);
        } else if (parent instanceof Frame) {
            dialog = new JDialog((Frame) parent);
        } else if (parent instanceof Dialog) {
            dialog = new JDialog((Dialog) parent);
        } else {
            dialog = new JDialog();
        }
        dialog.setTitle("Report - Parameter Prompt: " + reportName);
        dialog.setContentPane(optionPane);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(636, 344);
        dialog.setLocationRelativeTo(parent);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {

                optionPane.setValue(new Integer(
                        JOptionPane.CANCEL_OPTION));
            }
        });

        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if (dialog.isVisible()
                        && (e.getSource() == optionPane)
                        && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                    if (valid.get()
                            || ((Integer) optionPane.getValue())
                            .intValue() == JOptionPane.CANCEL_OPTION) {

                        dialog.setVisible(false);
                    } else {
                        optionPane.setValue(new Integer(JOptionPane.NO_OPTION));
                    }
                }
            }
        });

        dialog.pack();
        dialog.setVisible(true);
        int retval = ((Integer) optionPane.getValue()).intValue();
        dialog.dispose();
        frame.dispose();
        return retval;
    }
}
