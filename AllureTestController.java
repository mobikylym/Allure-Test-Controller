package mobikylym.jmeter.control; 

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.control.TransactionSampler;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.control.TransactionController;
//import org.apache.jmeter.testelement.schema.PropertiesAccessor;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jmeter.threads.SamplePackage;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction Controller to measure transaction times
 *
 * There are two different modes for the controller:
 * - generate additional total sample after nested samples (as in JMeter 2.2)
 * - generate parent sampler containing the nested samples
 *
 */
public class AllureTestController extends TransactionController {

    //private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

    public static final String ATC_PATH_TO_RESULTS = "AllureTestController.pathToResults";
    public static final String ATC_FOLDER_OVERWRITE = "AllureTestController.folderOverwrite";
    public static final String ATC_IS_CRITICAL = "AllureTestController.isCritical";
    public static final String ATC_IS_SINGLE_STEP = "AllureTestController.isSingleStep";
    public static final String ATC_TEST_NAME = "AllureTestController.testName";
    public static final String ATC_DESCRIPTION = "AllureTestController.description";
    public static final String ATC_SEVERITY = "AllureTestController.severity";
    public static final String ATC_EPIC = "AllureTestController.epic";
    public static final String ATC_STORY = "AllureTestController.story";
    public static final String ATC_FEATURE = "AllureTestController.feature";
    public static final String ATC_TAGS = "AllureTestController.tags";
    public static final String ATC_PARAMETERS = "AllureTestController.parameters";
    public static final String ATC_CONTENT_TYPE = "AllureTestController.contentType";
    public static final String ATC_OWNER = "AllureTestController.owner";
    public static final String ATC_LINKS = "AllureTestController.links";
    public static final String ATC_ISSUES = "AllureTestController.issues";
    public static final String ATC_EXTRA_LABELS = "AllureTestController.extraLabels";

    /**
     * Creates a Allure Test Controller
     */
    public AllureTestController() {
        super();
    }

    @Override
    public Sampler next() {
        return super.next();
    }

    //
    // Path to results
    //
    public void setPathToResults(String pathToResults) {
        setProperty(ATC_PATH_TO_RESULTS, pathToResults);
    }

    public String getPathToResults() {
        return getPropertyAsString(ATC_PATH_TO_RESULTS);
    }

    //
    // Overwrite folder
    //
    public void setFolderOverwrite(boolean ov) {
        setProperty(new BooleanProperty(ATC_FOLDER_OVERWRITE, ov));
    }
    
    public boolean isFolderOverwrite() {
        return getPropertyAsBoolean(ATC_FOLDER_OVERWRITE, false);
    }

    //
    // Stop test on error
    //
    public void setIsCritical(boolean ic) {
        setProperty(new BooleanProperty(ATC_IS_CRITICAL, ic));
    }
    
    public boolean isCriticalTest() {
        return getPropertyAsBoolean(ATC_IS_CRITICAL, false);
    }

    //
    // Single step tests
    //
    public void setIsSingleStep(boolean ss) {
        setProperty(new BooleanProperty(ATC_IS_SINGLE_STEP, ss));
    }
    
    public boolean isSingleStepTest() {
        return getPropertyAsBoolean(ATC_IS_SINGLE_STEP, false);
    }

    //
    // Test
    //
    public void setTestNameField(String te) {
        if(!isSingleStepTest()) {
            setProperty(ATC_TEST_NAME, te);
        } else {
            setProperty(ATC_TEST_NAME, "");
        }
    }

    public String getTestNameField() {
        return getPropertyAsString(ATC_TEST_NAME, "");
    }

    //
    // Description
    //
    public void setDescriptionField(String de) {
        if(!isSingleStepTest()) {
            setProperty(ATC_DESCRIPTION, de);
        } else {
            setProperty(ATC_DESCRIPTION, "");
        }
    }

    public String getDescriptionField() {
        return getPropertyAsString(ATC_DESCRIPTION, "");
    }

    //
    // Severity
    //
    public void setSeverity(String sev) {
        if(sev.toLowerCase().equals("blocker") || sev.toLowerCase().equals("critical") || sev.toLowerCase().equals("normal") || sev.toLowerCase().equals("minor") || sev.toLowerCase().equals("trivial")) {
            setProperty(ATC_SEVERITY, sev.toLowerCase());
        } else {
            setProperty(ATC_SEVERITY, "normal");
        }
    }

    public String getSeverity() {
        return getPropertyAsString(ATC_SEVERITY, "normal");
    }

    //
    // Epic
    //
    public void setEpicField(String ep) {
        setProperty(ATC_EPIC, ep);
    }

    public String getEpicField() {
        return getPropertyAsString(ATC_EPIC, "");
    }

    //
    // Story
    //
    public void setStoryField(String st) {
        setProperty(ATC_STORY, st);
    }

    public String getStoryField() {
        return getPropertyAsString(ATC_STORY, "");
    }

    //
    // Feature
    //
    public void setFeatureField(String fe) {
        setProperty(ATC_FEATURE, fe);
    }

    public String getFeatureField() {
        return getPropertyAsString(ATC_FEATURE, "");
    }

    //
    // Tags
    //
    public void setTagsField(String ta) {
        setProperty(ATC_TAGS, ta);
    }

    public String getTagsField() {
        return getPropertyAsString(ATC_TAGS, "");
    }

    //
    // Parameters
    //
    public void setParametersField(String pa) {
        setProperty(ATC_PARAMETERS, pa);
    }

    public String getParametersField() {
        return getPropertyAsString(ATC_PARAMETERS, "");
    }

    //
    // Content type
    //
    public void setContentTypeField(String co) {
        setProperty(ATC_CONTENT_TYPE, co);
    }

    public String getContentTypeField() {
        return getPropertyAsString(ATC_CONTENT_TYPE, "");
    }

    //
    // Owner
    //
    public void setOwnerField(String ow) {
        setProperty(ATC_OWNER, ow);
    }

    public String getOwnerField() {
        return getPropertyAsString(ATC_OWNER, "");
    }

    //
    // Links
    //
    public void setLinksField(String li) {
        setProperty(ATC_LINKS, li);
    }

    public String getLinksField() {
        return getPropertyAsString(ATC_LINKS, "");
    }

    //
    // Issues
    //
    public void setIssuesField(String is) {
        setProperty(ATC_ISSUES, is);
    }

    public String getIssuesField() {
        return getPropertyAsString(ATC_ISSUES, "");
    }

    //
    // Extra Labels
    //
    public void setExtraLabelsField(String ex) {
        setProperty(ATC_EXTRA_LABELS, ex);
    }

    public String getExtraLabelsField() {
        return getPropertyAsString(ATC_EXTRA_LABELS, "");
    }

}
