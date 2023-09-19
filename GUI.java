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

import org.apache.jmeter.control.AllureTestController;
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
public class AllureTestControllerGui extends AbstractControllerGui implements ClipboardOwner {

    private JRadioButton blocker; //Severity
    private JRadioButton critical; //Severity
    private JRadioButton normal; //Severity
    private JRadioButton minor; //Severity
    private JRadioButton trivial; //Severity
    private ButtonGroup group;
    private JTextField pathToResults; //Path to results
    private JCheckBox folderOverwrite; //Overwrite folder
    private JCheckBox isSingleStep; //Single step tests?
    private JCheckBox isCritical; //Critical?
    private JTextField testName; //Test
    private JTextField description; //Description
    private JTextField epic; //Epic
    private JTextField story; //Story
    private JTextField feature; //Feature
    private JTextField tags; //Tags
    private JTextField parameters; //Parameters
    private JTextField contentType; //Content type
    private JTextField owner; //Owner
    private final JLabel linksLabel; //Links
    private transient JTable linksTable;
    protected transient ObjectTableModel linksTableModel;
    private final JLabel issuesLabel; //Issues
    private transient JTable issuesTable;
    protected transient ObjectTableModel issuesTableModel;
    private final JLabel extraOptionsLabel; //Extra options
    private transient JTable extraOptionsTable;
    protected transient ObjectTableModel extraOptionsTableModel;
    private JComponent MainPanel;
    private JButton add;
    private JButton delete;

    /**
     * Added background support for reporting tool
     */
    private final Color background;

    /**
     * Boolean indicating whether this component is a standalone component or it
     * is intended to be used as a subpanel for another component.
     */
    private final boolean standalone;

    private JButton up;
    private JButton down;
    private JButton showDetail;

    /** Enable Up and Down buttons */
    private final boolean enableUpDown;

    /** Disable buttons :Detail, Add, Add from Clipboard, Delete, Up and Down*/
    private final boolean disableButtons;

    private final Function<String[], ? extends Argument> argCreator;

    private static final String ADD = "add"; // $NON-NLS-1$
    private static final String ADD_FROM_CLIPBOARD = "addFromClipboard"; // $NON-NLS-1$
    private static final String DELETE = "delete"; // $NON-NLS-1$
    private static final String UP = "up"; // $NON-NLS-1$
    private static final String DOWN = "down"; // $NON-NLS-1$
    private static final String DETAIL = "detail"; // $NON-NLS-1$

    /** When pasting from the clipboard, split lines on linebreak */
    private static final String CLIPBOARD_LINE_DELIMITERS = "\n"; //$NON-NLS-1$

    /** When pasting from the clipboard, split parameters on tab */
    private static final String CLIPBOARD_ARG_DELIMITERS = "\t"; //$NON-NLS-1$

    public static final String COLUMN_RESOURCE_NAMES_0 = "name"; // $NON-NLS-1$
    public static final String COLUMN_RESOURCE_NAMES_1 = "value"; // $NON-NLS-1$
    public static final String COLUMN_RESOURCE_NAMES_2 = "description"; // $NON-NLS-1$


    /**
     * Create a new AllureTestControllerGui instance.
     */
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
        GuiUtils.stopTableEditing(linksTable);
        linksTableModel.clearData();
        GuiUtils.stopTableEditing(issuesTable);
        issuesTableModel.clearData();
        GuiUtils.stopTableEditing(extraOptionsTable);
        extraOptionsTableModel.clearData();
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
        blocker.setActionCommand(RegexExtractor.USE_BLOCKER);
        critical.setActionCommand(RegexExtractor.USE_CRITICAL);
        normal.setActionCommand(RegexExtractor.USE_NORMAL);
        minor.setActionCommand(RegexExtractor.USE_MINOR);
        trivial.setActionCommand(RegexExtractor.USE_TRIVIAL);

        return panel;
    }

    private JPanel makePathToResultsPanel() {
        JPanel pathPanel = new JPanel(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.FIRST_LINE_END;

        GridBagConstraints editConstraints = new GridBagConstraints();
        editConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        editConstraints.weightx = 1.0;
        editConstraints.fill = GridBagConstraints.HORIZONTAL;

        addToPanel(pathPanel, labelConstraints, 0, 1, new JLabel("Path to results: ", JLabel.RIGHT));
        addToPanel(pathPanel, editConstraints, 1, 1, pathToResults = new JTextField(20));
        JButton browseButton = new JButton("Browse...");
        addToPanel(pathPanel, labelConstraints, 2, 1, browseButton);
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
        isSingleStep = new JCheckBox("Single step tests");
        isCritical = new JCheckBox("Stop test on error");
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
        panel.add(isSingleStep);
        panel.add(isCritical);
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
                // Checkbox has been selected
                testName.setEnabled(false);
                testName.setText("");
                description.setEnabled(false);
                description.setText("");
            } else {
                // Checkbox has been deselected
                testName.setEnabled(true);
                description.setEnabled(true);
            }
        });

        return panel;
    }

    @Override //-----------------------------------------------------Доделать
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
            isSingleStep.setSelected(atc.isSingleStepTest());
            isCritical.setSelected(atc.isCriticalTest());
            testName.setText(atc.getTestNameField());
            description.setText(atc.getDescriptionField());
            epic.setText(atc.getEpicField());
            story.setText(atc.getStoryField());
            feature.setText(atc.getFeatureField());
            tags.setText(atc.getTagsField());
            parameters.setText(atc.getParametersField());
            contentType.setText(atc.getContentTypeField());
            owner.setText(atc.getOwnerField());
            // Дописать про таблицы
        }
    }

    @Override //-----------------------------------------------------Доделать
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);
        if (te instanceof AllureTestController) {
            AllureTestController atc = (AllureTestController) te;
            atc.setSeverityGroup(group.getSelection().getActionCommand()); //Создать панель (group)
            atc.setPathToResults(pathToResults.getText());
            atc.setFolderOverwrite(folderOverwrite.isSelected());
            atc.setIsSingleStep(isSingleStep.isSelected());
            atc.setIsCritical(isCritical.isSelected());
            atc.setTestNameField(testName.getText());
            atc.setDescriptionField(description.getText());
            atc.setEpicField(epic.getText());
            atc.setStoryField(story.getText());
            atc.setFeatureField(feature.getText());
            atc.setTagsField(tags.getText());
            atc.setParametersField(parameters.getText());
            atc.setContentTypeField(contentType.getText());
            atc.setOwnerField(owner.getText());
            // Дописать про таблицы
        }
    }

    

//-----------------------------------------------------Доделать
    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        Box box = Box.createVerticalBox();
        box.add(makeTitlePanel(), BorderLayout.NORTH);
        box.add(makeSeverityPanel(), BorderLayout.NORTH);
        box.add(makePathToResultsPanel(), BorderLayout.NORTH); 
        box.add(makeMainParameterPanel(), BorderLayout.CENTER);
        box.add(); // Здесь будет панель с таблицами либо по панели на таблицу (изучить этот вопрос)
    }
}
