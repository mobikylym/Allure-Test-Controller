package org.apache.jmeter.control.gui;

import java.util.List;
import java.io.File;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;

//import org.apache.jmeter.control.AllureTestController;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.CheckBoxPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractListenerGui;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.apache.jorphan.gui.ObjectTableModel;

import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.gui.BrowseAction;
import kg.apc.jmeter.gui.GuiBuilderHelper;

/**
 * A Allure test controller component.
 */
public class AllureTestControllerGui extends AbstractControllerGui {

    private JRadioButton blocker; //Severity
    private JRadioButton critical; //Severity
    private JRadioButton normal; //Severity
    private JRadioButton minor; //Severity
    private JRadioButton trivial; //Severity
    private ButtonGroup group;
    private JTextField pathToResults; //Path to results
    private JCheckBox folderOverwrite; //Overwrite folder
    private JCheckBox isCritical; //Stop test on error
    private JCheckBox isSingleStep; //Single step tests
    private JLabeledTextField testName; //Test
    private JLabeledTextField description; //Description
    private JLabeledTextField epic; //Epic
    private JLabeledTextField story; //Story
    private JLabeledTextField feature; //Feature
    private JLabeledTextField tags; //Tags
    private JLabeledTextField parameters; //Parameters
    private JLabeledTextField contentType; //Content type
    private JLabeledTextField owner; //Owner
    private ArgumentsPanel linksPanel; //Links
    private ArgumentsPanel issuesPanel; //Issues (Allure Test Management System only)
    private ArgumentsPanel extraOptionsPanel; //Extra labels (Allure Test Management System only)

    public AllureTestControllerGui() {
        super();
        init();
        initFields();
    }

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("Allure Test Controller");
    }

    @Override
    public String getLabelResource() {
        return getClass().getCanonicalName();
    }

    @Override
    public TestElement createTestElement() {
        AllureTestController lc = new AllureTestController();
        configureTestElement(lc);
        return lc;
    }

    @Override
    public void clearGui() {
        super.clearGui();
        linksPanel.clearGui();
        issuesPanel.clearGui();
        extraOptionsPanel.clearGui();
        initFields();
    }

    private void initFields() { 
        normal.setSelected(true);
        pathToResults.setText("Choose result folder");
        folderOverwrite.setSelected(false);
        isSingleStep.setSelected(false);
        isCritical.setSelected(false);
        testName.setText("");
        description.setText("");
        epic.setText("");
        story.setText("");
        feature.setText("");
        tags.setText("");
        parameters.setText("");
        contentType.setText("");
        owner.setText("");
    }

    private JPanel makeSeverityPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Choose severity:"));

        blocker = new JRadioButton("Blocker");
        critical = new JRadioButton("Critical");
        normal = new JRadioButton("Normal");
        minor = new JRadioButton("Minor");
        trivial = new JRadioButton("Trivial");

        group = new ButtonGroup();
        group.add(blocker);
        group.add(critical);
        group.add(normal);
        group.add(minor);
        group.add(trivial);

        panel.add(blocker);
        panel.add(critical);
        panel.add(normal);
        panel.add(minor);
        panel.add(trivial);

        normal.setSelected(true);

        // So we know which button is selected
		/*
        blocker.setActionCommand(RegexExtractor.USE_BLOCKER);
        critical.setActionCommand(RegexExtractor.USE_CRITICAL);
        normal.setActionCommand(RegexExtractor.USE_NORMAL);
        minor.setActionCommand(RegexExtractor.USE_MINOR);
        trivial.setActionCommand(RegexExtractor.USE_TRIVIAL);
		*/
        return panel;
    }

    private JPanel makePathToResultsPanel() {
        JPanel pathPanel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Path to results: ", JLabel.RIGHT);
		constraints.gridx = 0;
		constraints.gridy = 1;
		pathPanel.add(label, constraints);

		JTextField pathToResults = new JTextField(20);
		constraints.gridx = 1;
		pathPanel.add(pathToResults, constraints);

		JButton browseButton = new JButton("Browse...");
		constraints.gridx = 2;
		pathPanel.add(browseButton, constraints);
        
        GuiBuilderHelper.strechItemToComponent(pathToResults, browseButton);
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser j = new JFileChooser();
                j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                Integer opt = j.showSaveDialog(pathPanel);
                if (opt == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = j.getSelectedFile();
                    pathToResults.setText(selectedFolder.getAbsolutePath());
                }
            }
        });

        return pathPanel;
    }

    private JPanel makeMainParameterPanel() {
        folderOverwrite = new JCheckBox("Overwrite folder");
        isCritical = new JCheckBox("Stop test on error");
        isSingleStep = new JCheckBox("Single step tests");
        testName = new JLabeledTextField("Test");
        description = new JLabeledTextField("Description");
        epic = new JLabeledTextField("Epic");
        story = new JLabeledTextField("Story"); 
        feature = new JLabeledTextField("Feature");
        tags = new JLabeledTextField("Tags");
        parameters = new JLabeledTextField("Parameters");
        contentType = new JLabeledTextField("Content type");
        owner = new JLabeledTextField("Owner");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(folderOverwrite);
        panel.add(isCritical);
        panel.add(isSingleStep);
        panel.add(testName);
        panel.add(description);
        panel.add(epic);
        panel.add(story);
        panel.add(feature);
        panel.add(tags);
        panel.add(parameters);
        panel.add(contentType);
        panel.add(owner);
        
        isSingleStep.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {

                testName.setEnabled(false);
                testName.setText("");
                description.setEnabled(false);
                description.setText("");

            } else {

                testName.setEnabled(true);
                description.setEnabled(true);

            }
        });

        return panel;
    }

    private JPanel makeLinksPanel() {
        linksPanel = new ArgumentsPanel("Links",null, true, true);
        return linksPanel;
    }

    private JPanel makeIssuesPanel() {
        issuesPanel = new ArgumentsPanel("Issues (Allure Test Management System only)",null, true, true);
        return issuesPanel;
    }

    private JPanel makeExtraOptionsPanel() {
        extraOptionsPanel = new ArgumentsPanel("Extra labels (Allure Test Management System only)",null, true, true);
        return extraOptionsPanel;
    }

    @Override
    public void configure(TestElement el) {
        super.configure(el);
        if (el instanceof AllureTestController) {
            AllureTestController atc = (AllureTestController) el;
            blocker.setSelected(atc.isBlocker());
            critical.setSelected(atc.isCritical());
            normal.setSelected(atc.isNormal());
            minor.setSelected(atc.isMinor());
            trivial.setSelected(atc.isTrivial());
            pathToResults.setText(atc.getPathToResults());
            folderOverwrite.setSelected(atc.isFolderOverwrite());
            isCritical.setSelected(atc.isCriticalTest());
            isSingleStep.setSelected(atc.isSingleStepTest());
            testName.setText(atc.getTestNameField());
            description.setText(atc.getDescriptionField());
            epic.setText(atc.getEpicField());
            story.setText(atc.getStoryField());
            feature.setText(atc.getFeatureField());
            tags.setText(atc.getTagsField());
            parameters.setText(atc.getParametersField());
            contentType.setText(atc.getContentTypeField());
            owner.setText(atc.getOwnerField());
            linksPanel.configure((Arguments) atc.getObjectValue());
            issuesPanel.configure((Arguments) atc.getObjectValue());
            extraOptionsPanel.configure((Arguments) atc.getObjectValue());
        }
    }

    @Override
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);
        if (te instanceof AllureTestController) {
            AllureTestController atc = (AllureTestController) te;
            atc.setSeverityGroup(group.getSelection().getActionCommand());
            atc.setPathToResults(pathToResults.getText());
            atc.setFolderOverwrite(folderOverwrite.isSelected());
            atc.setIsCritical(isCritical.isSelected());
            atc.setIsSingleStep(isSingleStep.isSelected());
            atc.setTestNameField(testName.getText());
            atc.setDescriptionField(description.getText());
            atc.setEpicField(epic.getText());
            atc.setStoryField(story.getText());
            atc.setFeatureField(feature.getText());
            atc.setTagsField(tags.getText());
            atc.setParametersField(parameters.getText());
            atc.setContentTypeField(contentType.getText());
            atc.setOwnerField(owner.getText());
            atc.setArguments((Arguments) linksPanel.createTestElement());
            atc.setArguments((Arguments) issuesPanel.createTestElement());
            atc.setArguments((Arguments) extraOptionsPanel.createTestElement());
        }
    }

    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        Box box = Box.createVerticalBox();
        box.add(makeTitlePanel());
        box.add(makeSeverityPanel());
        box.add(makePathToResultsPanel()); 
        box.add(makeMainParameterPanel());
        box.add(makeLinksPanel());
        box.add(makeIssuesPanel());
        box.add(makeExtraOptionsPanel()); 
        add(box, BorderLayout.NORTH);               
    }
}