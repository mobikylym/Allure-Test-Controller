package mobikylym.jmeter.control.gui;

import mobikylym.jmeter.control.AllureTestController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import org.apache.jmeter.control.gui.AbstractControllerGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * An Allure Test Controller component.
 */

public class AllureTestControllerGui extends AbstractControllerGui {

    private static final long serialVersionUID = 2001L;

    private JTextField pathToResults = new JTextField("Choose result folder"); //Path to results
    private JCheckBox folderOverwrite = new JCheckBox("Overwrite folder", false); //Overwrite folder
    private JCheckBox isCritical = new JCheckBox("Stop test on error", false); //Stop test on error
    private JCheckBox isSingleStep = new JCheckBox("Single step tests", false); //Single step tests
    private JTextField testName = new JTextField(); //Test
    private JTextField description = new JTextField(); //Description
    private JComboBox<String> severity = new JComboBox<>(new String[] {"blocker", "critical", "normal", "minor", "trivial"}); //Severity
    private JTextField epic = new JTextField(); //Epic
    private JTextField story = new JTextField(); //Story
    private JTextField feature = new JTextField(); //Feature
    private JTextField tags = new JTextField(); //Tags
    private JTextField parameters = new JTextField(); //Parameters
    private JTextField contentType = new JTextField("text/plain"); //Content type
    private JTextField owner = new JTextField(); //Owner
    private JTextArea links = new JTextArea(); //Links
    private JTextArea issues = new JTextArea(); //Issues
    private JTextArea extraLabels = new JTextArea(); //Extra Labels

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
        initFields();
    }

    private void initFields() { 
        pathToResults.setText("Choose result folder");
        folderOverwrite.setSelected(false);
        isSingleStep.setSelected(false);
        isCritical.setSelected(false);
        testName.setText("");
        description.setText("");
        description.setText("");
        severity.setSelectedItem("normal");
        severity.setEditable(true);
        epic.setText("");
        story.setText("");
        feature.setText("");
        tags.setText("");
        parameters.setText("");
        contentType.setText("text/plain");
        owner.setText("");
        links.setText("");
        issues.setText("");
        extraLabels.setText("");
    }

    @Override
    public void modifyTestElement(TestElement element) {
        configureTestElement(element);
        if (element instanceof AllureTestController) {
            AllureTestController obj = (AllureTestController) element;
            obj.setProperty(AllureTestController.ATC_PATH_TO_RESULTS, pathToResults.getText());
            obj.setProperty(AllureTestController.ATC_FOLDER_OVERWRITE, folderOverwrite.isSelected());
            obj.setProperty(AllureTestController.ATC_IS_CRITICAL, isCritical.isSelected());
            obj.setProperty(AllureTestController.ATC_IS_SINGLE_STEP, isSingleStep.isSelected());
            obj.setProperty(AllureTestController.ATC_TEST_NAME, testName.getText());
            obj.setProperty(AllureTestController.ATC_DESCRIPTION, description.getText());
            obj.setProperty(AllureTestController.ATC_SEVERITY, (String) severity.getSelectedItem());            
            obj.setProperty(AllureTestController.ATC_EPIC, epic.getText());
            obj.setProperty(AllureTestController.ATC_STORY, story.getText());
            obj.setProperty(AllureTestController.ATC_FEATURE, feature.getText());
            obj.setProperty(AllureTestController.ATC_TAGS, tags.getText());
            obj.setProperty(AllureTestController.ATC_PARAMETERS, parameters.getText());
            obj.setProperty(AllureTestController.ATC_CONTENT_TYPE, contentType.getText());
            obj.setProperty(AllureTestController.ATC_OWNER, owner.getText());
            obj.setProperty(AllureTestController.ATC_LINKS, links.getText());
            obj.setProperty(AllureTestController.ATC_ISSUES, issues.getText());
            obj.setProperty(AllureTestController.ATC_EXTRA_LABELS, extraLabels.getText());
        }
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        pathToResults.setText(element.getPropertyAsString(AllureTestController.ATC_PATH_TO_RESULTS));
        folderOverwrite.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_FOLDER_OVERWRITE));
        isCritical.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_IS_CRITICAL));
        isSingleStep.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_IS_SINGLE_STEP));
        testName.setText(element.getPropertyAsString(AllureTestController.ATC_TEST_NAME));
        description.setText(element.getPropertyAsString(AllureTestController.ATC_DESCRIPTION));
        severity.setSelectedItem(element.getPropertyAsString(AllureTestController.ATC_SEVERITY));
        epic.setText(element.getPropertyAsString(AllureTestController.ATC_EPIC));
        story.setText(element.getPropertyAsString(AllureTestController.ATC_STORY));
        feature.setText(element.getPropertyAsString(AllureTestController.ATC_FEATURE));
        tags.setText(element.getPropertyAsString(AllureTestController.ATC_TAGS));
        parameters.setText(element.getPropertyAsString(AllureTestController.ATC_PARAMETERS));
        contentType.setText(element.getPropertyAsString(AllureTestController.ATC_CONTENT_TYPE));
        owner.setText(element.getPropertyAsString(AllureTestController.ATC_OWNER));
        links.setText(element.getPropertyAsString(AllureTestController.ATC_LINKS));
        issues.setText(element.getPropertyAsString(AllureTestController.ATC_ISSUES));
        extraLabels.setText(element.getPropertyAsString(AllureTestController.ATC_EXTRA_LABELS));
    }

    public AllureTestControllerGui() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
    
        add(makeTitlePanel(), BorderLayout.NORTH);
        JPanel mainPanel = new JPanel(new GridBagLayout());

        GridBagConstraints separatorConstraints = new GridBagConstraints();
        separatorConstraints.weightx = 1.0;
        separatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        separatorConstraints.gridwidth = GridBagConstraints.REMAINDER;
        separatorConstraints.insets = new Insets(0, 0, 3, 0);

        mainPanel.add(new JSeparator(), separatorConstraints);
    
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.EAST;
    
        GridBagConstraints editConstraints = new GridBagConstraints();
        editConstraints.anchor = GridBagConstraints.CENTER;
        editConstraints.weightx = 1.0;
        editConstraints.fill = GridBagConstraints.HORIZONTAL;
    
        JLabel label = new JLabel("Path to results: ", JLabel.RIGHT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        mainPanel.add(label, labelConstraints);
    
        editConstraints.gridx = 1;
        editConstraints.gridy = 1;
        editConstraints.gridwidth = 3;
        mainPanel.add(pathToResults, editConstraints);

        JButton browseButton = new JButton("Browse...");
		labelConstraints.gridx = 4;

		mainPanel.add(browseButton, labelConstraints);

        browseButton.addActionListener(e -> {
            JFileChooser j = new JFileChooser();
            j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int opt = j.showSaveDialog(mainPanel);
            if (opt == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = j.getSelectedFile();
                pathToResults.setText(selectedFolder.getAbsolutePath());
            }
        });

        GridBagConstraints checkBoxConstraints = new GridBagConstraints();
        checkBoxConstraints.anchor = GridBagConstraints.WEST;

        checkBoxConstraints.gridx = 1;
        checkBoxConstraints.gridy = 2;
        mainPanel.add(folderOverwrite, checkBoxConstraints);

        checkBoxConstraints.gridx = 2;
        checkBoxConstraints.gridy = 2;
        mainPanel.add(isCritical, checkBoxConstraints);

        checkBoxConstraints.gridx = 3;
        checkBoxConstraints.gridy = 2;
        mainPanel.add(isSingleStep, checkBoxConstraints);

        JLabel nameLabel = new JLabel("Test name: ", JLabel.RIGHT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 3;
        mainPanel.add(nameLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 3;
        editConstraints.gridwidth = 4;
        mainPanel.add(testName, editConstraints);

        JLabel descLabel = new JLabel("Description: ", JLabel.RIGHT);
        labelConstraints.gridy = 4;
        mainPanel.add(descLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 4;
        mainPanel.add(description, editConstraints);

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

        JLabel severityLabel = new JLabel("Severity: ", JLabel.RIGHT);
        labelConstraints.gridy = 5;
        mainPanel.add(severityLabel, labelConstraints);

        severity.setEditable(true);
        editConstraints.gridx = 1;
        editConstraints.gridy = 5;
        mainPanel.add(severity, editConstraints);

        JLabel epicLabel = new JLabel("Epic: ", JLabel.RIGHT);
        labelConstraints.gridy = 6;
        mainPanel.add(epicLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 6;
        mainPanel.add(epic, editConstraints);

        JLabel storyLabel = new JLabel("Story: ", JLabel.RIGHT);
        labelConstraints.gridy = 7;
        mainPanel.add(storyLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 7;
        mainPanel.add(story, editConstraints);

        JLabel featureLabel = new JLabel("Feature: ", JLabel.RIGHT);
        labelConstraints.gridy = 8;
        mainPanel.add(featureLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 8;
        mainPanel.add(feature, editConstraints);

        JLabel tagsLabel = new JLabel("Tags (comma-delimited): ", JLabel.RIGHT);
        labelConstraints.gridy = 9;
        mainPanel.add(tagsLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 9;
        mainPanel.add(tags, editConstraints);

        JLabel parametersLabel = new JLabel("Params (comma-delimited): ", JLabel.RIGHT);
        labelConstraints.gridy = 10;
        mainPanel.add(parametersLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 10;
        mainPanel.add(parameters, editConstraints);

        JLabel contentTypeLabel = new JLabel("Content type: ", JLabel.RIGHT);
        labelConstraints.gridy = 11;
        mainPanel.add(contentTypeLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 11;
        mainPanel.add(contentType, editConstraints);

        JLabel ownerLabel = new JLabel("Owner: ", JLabel.RIGHT);
        labelConstraints.gridy = 12;
        mainPanel.add(ownerLabel, labelConstraints);

        editConstraints.gridx = 1;
        editConstraints.gridy = 12;
        mainPanel.add(owner, editConstraints);

        separatorConstraints.insets = new Insets(3, 0, 0, 0);
        separatorConstraints.gridy = 13;
        mainPanel.add(new JSeparator(), separatorConstraints);

        JLabel linksLabel = new JLabel("Links", JLabel.CENTER);
        labelConstraints.anchor = GridBagConstraints.CENTER;
        labelConstraints.insets = new Insets(3, 0, 0, 0);
        labelConstraints.gridy = 14;
        labelConstraints.gridwidth = 5;
        mainPanel.add(linksLabel, labelConstraints);

        editConstraints.gridx = 0;
        editConstraints.gridy = 15;
        editConstraints.gridwidth = 5;
        JScrollPane scrollPane1 = new JScrollPane(links);
        scrollPane1.setPreferredSize(new Dimension(200, 77));
        mainPanel.add(scrollPane1, editConstraints);

        JLabel issuesLabel = new JLabel("Issues (Allure Test Management System only)", JLabel.CENTER);
        labelConstraints.insets = new Insets(5, 0, 0, 0);
        labelConstraints.gridy = 16;
        mainPanel.add(issuesLabel, labelConstraints);

        editConstraints.gridy = 17;
        JScrollPane scrollPane2 = new JScrollPane(issues);
        scrollPane2.setPreferredSize(new Dimension(200, 77));
        mainPanel.add(scrollPane2, editConstraints);

        JLabel extraLabelsLabel = new JLabel("Extra labels (Allure Test Management System only)", JLabel.CENTER);
        labelConstraints.gridy = 18;
        mainPanel.add(extraLabelsLabel, labelConstraints);

        editConstraints.gridy = 19;
        JScrollPane scrollPane3 = new JScrollPane(extraLabels);
        scrollPane3.setPreferredSize(new Dimension(200, 77));
        mainPanel.add(scrollPane3, editConstraints);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);

        add(container, BorderLayout.CENTER);
    }
}
