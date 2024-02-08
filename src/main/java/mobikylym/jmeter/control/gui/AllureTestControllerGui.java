package mobikylym.jmeter.control.gui;

import mobikylym.jmeter.control.AllureTestController;

import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.control.gui.AbstractControllerGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An Allure Test Controller component.
 */

public class AllureTestControllerGui extends AbstractControllerGui {

    private static final long serialVersionUID = 2001L;

    private static final String helperLink = "https://github.com/mobikylym/Allure-Test-Controller";

    private JTextField pathToResults = new JTextField("Choose result folder"); //Path to results
    private JCheckBox folderOverwrite = new JCheckBox("Overwrite folder  ", false); //Overwrite folder
    private JCheckBox isCritical = new JCheckBox("Stop test on error  ", false); //Stop test on error
    private JCheckBox isSingleStep = new JCheckBox("Single step tests  ", false); //Single step tests
    private JCheckBox withoutContent = new JCheckBox("Without content  ", false); //Without content
    private JCheckBox withoutNonHTTP = new JCheckBox("Without non-HTTP steps  ", false); //Without non-HTTP steps
    private JCheckBox debugMode = new JCheckBox("Debug mode  ", false); //Debug mode
    private JTextField testName = new JTextField(); //Test
    private JTextField description = new JTextField(); //Description
    private JComboBox<String> severity = new JComboBox<>(new String[] {"blocker", "critical", "normal", "minor", "trivial"}); //Severity
    private JTextField epic = new JTextField(); //Epic
    private JTextField story = new JTextField(); //Story
    private JTextField feature = new JTextField(); //Feature
    private JTextField tags = new JTextField(); //Tags
    private JTextField parameters = new JTextField(); //Parameters
    private JTextField owner = new JTextField(); //Owner
    private JTextArea links = new JTextArea(); //Links
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
        withoutContent.setSelected(false);
        withoutNonHTTP.setSelected(false);
        debugMode.setSelected(false);
        testName.setText("");
        description.setText("");
        severity.setSelectedItem("normal");
        severity.setEditable(true);
        epic.setText("");
        story.setText("");
        feature.setText("");
        tags.setText("");
        parameters.setText("");
        owner.setText("");
        links.setText("");
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
            obj.setProperty(AllureTestController.ATC_WITHOUT_CONTENT, withoutContent.isSelected());
            obj.setProperty(AllureTestController.ATC_WITHOUT_NON_HTTP, withoutNonHTTP.isSelected());
            obj.setProperty(AllureTestController.ATC_DEBUG_MODE, debugMode.isSelected());
            obj.setProperty(AllureTestController.ATC_TEST_NAME, testName.getText());
            obj.setProperty(AllureTestController.ATC_DESCRIPTION, description.getText());
            obj.setProperty(AllureTestController.ATC_SEVERITY, (String) severity.getEditor().getItem());            
            obj.setProperty(AllureTestController.ATC_EPIC, epic.getText());
            obj.setProperty(AllureTestController.ATC_STORY, story.getText());
            obj.setProperty(AllureTestController.ATC_FEATURE, feature.getText());
            obj.setProperty(AllureTestController.ATC_TAGS, tags.getText());
            obj.setProperty(AllureTestController.ATC_PARAMETERS, parameters.getText());
            obj.setProperty(AllureTestController.ATC_OWNER, owner.getText());
            obj.setProperty(AllureTestController.ATC_LINKS, links.getText());
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
        withoutContent.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_WITHOUT_CONTENT));
        withoutNonHTTP.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_WITHOUT_NON_HTTP));
        debugMode.setSelected(element.getPropertyAsBoolean(AllureTestController.ATC_DEBUG_MODE));
        testName.setText(element.getPropertyAsString(AllureTestController.ATC_TEST_NAME));
        description.setText(element.getPropertyAsString(AllureTestController.ATC_DESCRIPTION));
        severity.setSelectedItem(element.getPropertyAsString(AllureTestController.ATC_SEVERITY));
        epic.setText(element.getPropertyAsString(AllureTestController.ATC_EPIC));
        story.setText(element.getPropertyAsString(AllureTestController.ATC_STORY));
        feature.setText(element.getPropertyAsString(AllureTestController.ATC_FEATURE));
        tags.setText(element.getPropertyAsString(AllureTestController.ATC_TAGS));
        parameters.setText(element.getPropertyAsString(AllureTestController.ATC_PARAMETERS));
        owner.setText(element.getPropertyAsString(AllureTestController.ATC_OWNER));
        links.setText(element.getPropertyAsString(AllureTestController.ATC_LINKS));
        extraLabels.setText(element.getPropertyAsString(AllureTestController.ATC_EXTRA_LABELS));
    }

    public AllureTestControllerGui() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
    
        add(addHelpLinkToPanel(makeTitlePanel(), helperLink), BorderLayout.NORTH);
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
    
        JLabel label = new JLabel("Results folder path: ", JLabel.RIGHT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        mainPanel.add(label, labelConstraints);
    
        editConstraints.gridx = 1;
        editConstraints.gridy = 1;
        editConstraints.gridwidth = 7;
        mainPanel.add(pathToResults, editConstraints);

        JButton browseButton = new JButton("Browse...");
		labelConstraints.gridx = 8;

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
        mainPanel.add(isCritical, checkBoxConstraints);

        checkBoxConstraints.gridx = 3;
        mainPanel.add(isSingleStep, checkBoxConstraints);

        checkBoxConstraints.gridx = 4;
        mainPanel.add(withoutContent, checkBoxConstraints);

        checkBoxConstraints.gridx = 5;
        mainPanel.add(withoutNonHTTP, checkBoxConstraints);

        checkBoxConstraints.gridx = 6;
        mainPanel.add(debugMode, checkBoxConstraints);

        JLabel nameLabel = new JLabel("Test name: ", JLabel.RIGHT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy += 2;
        mainPanel.add(nameLabel, labelConstraints);

        editConstraints.gridy += 2;
        editConstraints.gridwidth = 8;
        mainPanel.add(testName, editConstraints);

        JLabel descLabel = new JLabel("Description: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(descLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(description, editConstraints);

        isSingleStep.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {

                testName.setEnabled(false);
                testName.setText("Same as sampler name");
                description.setEnabled(false);
                description.setText("Same as sampler comment");

            } else {

                testName.setEnabled(true);
                testName.setText("");
                description.setEnabled(true);
                description.setText("");

            }
        });

        JLabel severityLabel = new JLabel("Severity: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(severityLabel, labelConstraints);

        severity.setEditable(true);
        editConstraints.gridy ++;
        mainPanel.add(severity, editConstraints);

        JLabel epicLabel = new JLabel("Epic: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(epicLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(epic, editConstraints);

        JLabel storyLabel = new JLabel("Story: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(storyLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(story, editConstraints);

        JLabel featureLabel = new JLabel("Feature: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(featureLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(feature, editConstraints);

        JLabel tagsLabel = new JLabel("Tags (comma-delimited): ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(tagsLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(tags, editConstraints);

        JLabel parametersLabel = new JLabel("Params (comma-delimited): ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(parametersLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(parameters, editConstraints);

        JLabel ownerLabel = new JLabel("Owner: ", JLabel.RIGHT);
        labelConstraints.gridy ++;
        mainPanel.add(ownerLabel, labelConstraints);

        editConstraints.gridy ++;
        mainPanel.add(owner, editConstraints);

        separatorConstraints.insets = new Insets(3, 0, 0, 0);
        separatorConstraints.gridy = 12;
        mainPanel.add(new JSeparator(), separatorConstraints);

        JLabel linksLabel = new JLabel("Links (format: name-comma-URL)", JLabel.CENTER);
        labelConstraints.anchor = GridBagConstraints.CENTER;
        labelConstraints.insets = new Insets(3, 0, 0, 0);
        labelConstraints.gridy += 2;
        labelConstraints.gridwidth = 9;
        mainPanel.add(linksLabel, labelConstraints);

        editConstraints.gridx = 0;
        editConstraints.gridy += 3;
        editConstraints.gridwidth = 9;
        JScrollPane scrollPane1 = new JScrollPane(links);
        scrollPane1.setPreferredSize(new Dimension(200, 77));
        mainPanel.add(scrollPane1, editConstraints);

        JLabel extraLabelsLabel = new JLabel("Extra labels (Allure Test Management System only)", JLabel.CENTER);
        labelConstraints.gridy += 2;
        mainPanel.add(extraLabelsLabel, labelConstraints);

        editConstraints.gridy += 2;
        JScrollPane scrollPane2 = new JScrollPane(extraLabels);
        scrollPane2.setPreferredSize(new Dimension(200, 172));
        mainPanel.add(scrollPane2, editConstraints);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);

        add(container, BorderLayout.CENTER);
    }

    private static Component addHelpLinkToPanel(Container panel, String helpPage) {
        JLabel linkLabel = new JLabel("<html><a href=''>How to use this plugin?</a></html>");
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(helpPage));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        panel.add(linkLabel);

        return panel;
    }
}