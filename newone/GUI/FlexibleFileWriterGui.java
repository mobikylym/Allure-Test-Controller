package kg.apc.jmeter.reporters;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.gui.BrowseAction;
import kg.apc.jmeter.gui.GuiBuilderHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.gui.AbstractListenerGui;

public class FlexibleFileWriterGui extends AbstractListenerGui implements ClipboardOwner {

    @Override
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);
        if (te instanceof FlexibleFileWriter) {
            FlexibleFileWriter fw = (FlexibleFileWriter) te;
            fw.setFilename(filename.getText());
            fw.setColumns(columns.getText());
            fw.setOverwrite(overwrite.isSelected());
            fw.setFileHeader(header.getText());
            fw.setFileFooter(footer.getText());
        }
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        FlexibleFileWriter fw = (FlexibleFileWriter) element;
        filename.setText(fw.getFilename());
        columns.setText(fw.getColumns());
        overwrite.setSelected(fw.isOverwrite());
        header.setText(fw.getFileHeader());
        footer.setText(fw.getFileFooter());
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        add(JMeterPluginsUtils.addHelpLinkToPanel(makeTitlePanel(), WIKIPAGE), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.FIRST_LINE_END;

        GridBagConstraints editConstraints = new GridBagConstraints();
        editConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        editConstraints.weightx = 1.0;
        editConstraints.fill = GridBagConstraints.HORIZONTAL;

        addToPanel(mainPanel, labelConstraints, 0, 1, new JLabel("Filename: ", JLabel.RIGHT));
        addToPanel(mainPanel, editConstraints, 1, 1, filename = new JTextField(20));
        JButton browseButton = new JButton("Browse...");
        addToPanel(mainPanel, labelConstraints, 2, 1, browseButton);
        GuiBuilderHelper.strechItemToComponent(filename, browseButton);
        browseButton.addActionListener(new BrowseAction(filename));

        addToPanel(mainPanel, labelConstraints, 0, 2, new JLabel("Overwrite existing file: ", JLabel.RIGHT));
        addToPanel(mainPanel, editConstraints, 1, 2, overwrite = new JCheckBox());

        addToPanel(mainPanel, labelConstraints, 0, 3, new JLabel("Write File Header: ", JLabel.RIGHT));
        header = new JTextArea();
        header.setLineWrap(true);
        addToPanel(mainPanel, editConstraints, 1, 3, GuiBuilderHelper.getTextAreaScrollPaneContainer(header, 3));

        editConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        labelConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        addToPanel(mainPanel, labelConstraints, 0, 4, new JLabel("Record each sample as: ", JLabel.RIGHT));
        addToPanel(mainPanel, editConstraints, 1, 4, columns = new JTextField(20));

        editConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        labelConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        addToPanel(mainPanel, labelConstraints, 0, 5, new JLabel("Write File Footer: ", JLabel.RIGHT));
        footer = new JTextArea();
        footer.setLineWrap(true);
        addToPanel(mainPanel, editConstraints, 1, 5, GuiBuilderHelper.getTextAreaScrollPaneContainer(footer, 3));

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);
        add(container, BorderLayout.CENTER);

        add(createHelperPanel(), BorderLayout.SOUTH);
    }
}
