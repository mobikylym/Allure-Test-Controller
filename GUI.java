package org.apache.jmeter.control.gui;

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
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.jmeter.control.AllureTestController;
import org.apache.jmeter.gui.util.CheckBoxPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.apache.jorphan.gui.ObjectTableModel;


/**
 * A Allure test controller component.
 */
public class AllureTestControllerGui extends AbstractControllerGui implements ClipboardOwner {

    private JTextField pathToResults; //Path to results
    private JCheckBox folderOverwrite; //Overwrite folder
    private JCheckBox isSingleStep; //Single step tests?
    private JCheckBox isCritical; //Critical?
    private JRadioButton blocker; //Severity
    private JRadioButton critical; //Severity
    private JRadioButton normal; //Severity
    private JRadioButton minor; //Severity
    private JRadioButton trivial; //Severity
    private ButtonGroup group;
    private JTextField testName; //Test
    private JTextField description; //Description
    private JTextField epic; //Epic
    private JTextField story; //Story
    private JTextField feature; //Feature
    private JTextField tags; //Tags
    private final JLabel linksLabel;
    private transient JTable linksTable;
    protected transient ObjectTableModel linksTableModel;
    private final JLabel issuesLabel;
    private transient JTable issuesTable;
    protected transient ObjectTableModel issuesTableModel;
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

    private JTextField parameters; //Parameters
    private JTextField contentType; //Content type
    private JTextField owner; //Owner
    private final JLabel extraOptionsLabel;
    private transient JTable extraOptionsTable;
    protected transient ObjectTableModel extraOptionsTableModel;



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
        lc.setIncludeTimers(false); // change default for new test elements
        configureTestElement(lc);
        return lc;
    }

    @Override
    public void clearGui() {
        super.clearGui();
        initFields();
    }

    private JPanel makeSeverityPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Choose severity:")); //$NON-NLS-1$

        blocker = new JRadioButton("Blocker"); //$NON-NLS-1$
        critical = new JRadioButton("Critical"); //$NON-NLS-1$
        normal = new JRadioButton("Normal"); //$NON-NLS-1$
        minor = new JRadioButton("Minor"); //$NON-NLS-1$
        trivial = new JRadioButton("Trivial"); //$NON-NLS-1$

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

    @Override //-----------------------------------------------------Доделать
    public void configure(TestElement el) {
        super.configure(el);
        generateParentSample.setSelected(((AllureTestController) el).isGenerateParentSample());
        includeTimers.setSelected(((AllureTestController) el).isIncludeTimers());
    }

    @Override //-----------------------------------------------------Доделать
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);
        if (te instanceof AllureTestController) {
            AllureTestController atc = (AllureTestController) te;
            saveScopeSettings(atc);
            atc.setPathToResults(pathToResults.getText());
            atc.setFolderOverwrite(folderOverwrite.isSelected());
            atc.setIsSingleStep(isSingleStep.isSelected());
            atc.setIsCritical(isCritical.isSelected());
            atc.setUseField(group.getSelection().getActionCommand()); //Создать панель (group)
            atc.setFileHeader(header.getText());
            atc.setFileFooter(footer.getText());
        }
    }

    

    /**
     * Initialize the GUI components and layout for this component.
     */
    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel());
        generateParentSample = new JCheckBox(JMeterUtils.getResString("transaction_controller_parent")); // $NON-NLS-1$
        add(CheckBoxPanel.wrap(generateParentSample));
        includeTimers = new JCheckBox(JMeterUtils.getResString("transaction_controller_include_timers"), true); // $NON-NLS-1$
        add(CheckBoxPanel.wrap(includeTimers));
    }
}
