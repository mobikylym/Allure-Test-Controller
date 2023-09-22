package kg.apc.jmeter.control.gui;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

import kg.apc.jmeter.control.AllureTestController;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.CheckBoxPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.control.gui.AbstractControllerGui;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.apache.jorphan.gui.ObjectTableModel;

import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.gui.BrowseAction;
import kg.apc.jmeter.gui.GuiBuilderHelper;

/**
 * Allure Test Controller GUI component.
 */
public class AllureTestControllerGui extends AbstractControllerGui {
    private JTextField pathToResults; //Path to results
    private JCheckBox folderOverwrite; //Overwrite folder
    private JCheckBox isCritical; //Stop test on error
    private JCheckBox isSingleStep; //Single step tests
    private JLabeledTextField testName; //Test
    private JLabeledTextField description; //Description
    private JLabeledTextField severity; //Severity
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
        init();
        initFields();
    }

    @Override
    public String getStaticLabel() {
        return "Allure Test Controller";
    }

    @Override
    public String getLabelResource() {
        return null;
    }

    @Override
    public TestElement createTestElement() {
        AllureTestController lc = new AllureTestController();
        modifyTestElement(lc);
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
        pathToResults.setText("Choose result folder");
        folderOverwrite.setSelected(false);
        isSingleStep.setSelected(false);
        isCritical.setSelected(false);
        testName.setText("");
        description.setText("");
        severity.setText("normal");
        epic.setText("");
        story.setText("");
        feature.setText("");
        tags.setText("");
        parameters.setText("");
        contentType.setText("");
        owner.setText("");
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
        severity = new JLabeledTextField("Severity (blocker, critical, normal, minor or trivial)");
        epic = new JLabeledTextField("Epic");
        story = new JLabeledTextField("Story"); 
        feature = new JLabeledTextField("Feature");
        tags = new JLabeledTextField("Tags (comma separated)");
        parameters = new JLabeledTextField("Parameters (comma separated)");
        contentType = new JLabeledTextField("Content type (default text/plain)");
        owner = new JLabeledTextField("Owner");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(folderOverwrite);
        panel.add(isCritical);
        panel.add(isSingleStep);
        panel.add(testName);
        panel.add(description);
        panel.add(severity);
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
        pathToResults.setText(el.getPropertyAsString(AllureTestController.PATH_TO_RESULTS));
        folderOverwrite.setSelected(el.getPropertyAsBoolean(AllureTestController.FOLDER_OVERWRITE));
        isCritical.setSelected(el.getPropertyAsBoolean(AllureTestController.IS_CRITICAL));
        isSingleStep.setSelected(el.getPropertyAsBoolean(AllureTestController.IS_SINGLE_STEP));
        testName.setText(el.getPropertyAsString(AllureTestController.TEST_NAME));
        description.setText(el.getPropertyAsString(AllureTestController.DESCRIPTION));
        severity.setText(el.getPropertyAsString(AllureTestController.SEVERITY));
        epic.setText(el.getPropertyAsString(AllureTestController.EPIC));
        story.setText(el.getPropertyAsString(AllureTestController.STORY));
        feature.setText(el.getPropertyAsString(AllureTestController.FEATURE));
        tags.setText(el.getPropertyAsString(AllureTestController.TAGS));
        parameters.setText(el.getPropertyAsString(AllureTestController.PARAMETERS));
        contentType.setText(el.getPropertyAsString(AllureTestController.CONTENT_TYPE));
        owner.setText(el.getPropertyAsString(AllureTestController.OWNER));
        linksPanel.configure((Arguments) atc.getObjectValue());
        issuesPanel.configure((Arguments) atc.getObjectValue());
        extraOptionsPanel.configure((Arguments) atc.getObjectValue());
        
    }

    @Override
    public void modifyTestElement(TestElement te) {
        configureTestElement(te);
        if (te instanceof AllureTestController) {
            AllureTestController atc = (AllureTestController) te;
            atc.setProperty(AllureTestController.PATH_TO_RESULTS, pathToResults.getText());
            atc.setProperty(AllureTestController.FOLDER_OVERWRITE, folderOverwrite.isSelected());
            atc.setProperty(AllureTestController.IS_CRITICAL, isCritical.isSelected());
            atc.setProperty(AllureTestController.IS_SINGLE_STEP, isSingleStep.isSelected());
            atc.setProperty(AllureTestController.TEST_NAME, testName.getText());
            atc.setProperty(AllureTestController.DESCRIPTION, description.getText());
            atc.setProperty(AllureTestController.SEVERITY, severity.getText());
            atc.setProperty(AllureTestController.EPIC, epic.getText());
            atc.setProperty(AllureTestController.STORY, story.getText());
            atc.setProperty(AllureTestController.FEATURE, feature.getText());
            atc.setProperty(AllureTestController.TAGS, tags.getText());
            atc.setProperty(AllureTestController.PARAMETERS, parameters.getText());
            atc.setProperty(AllureTestController.CONTENT_TYPE, contentType.getText());
            atc.setProperty(AllureTestController.OWNER, owner.getText());
            atc.setArguments((Arguments) linksPanel.createTestElement());
            atc.setArguments((Arguments) issuesPanel.createTestElement());
            atc.setArguments((Arguments) extraOptionsPanel.createTestElement());
        }
    }

    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);
        add(makePathToResultsPanel()); 
        add(makeMainParameterPanel());
        add(makeLinksPanel());
        add(makeIssuesPanel());
        add(makeExtraOptionsPanel(), BorderLayout.SOUTH);
    }
}