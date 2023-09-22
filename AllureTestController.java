package kg.apc.jmeter.control; 

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.samplers.SampleEvent; 
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.testelement.schema.PropertiesAccessor;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jmeter.threads.SamplePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ------------------------------------Здесь добавить описание класса
 */
public class AllureTestController extends TransactionController implements Serializable {
    
    public static final String PATH_TO_RESULTS = "AllureTestController.pathToResults";
    public static final String FOLDER_OVERWRITE = "AllureTestController.folderOverwrite";
    public static final String IS_CRITICAL = "AllureTestController.isCritical";
    public static final String IS_SINGLE_STEP = "AllureTestController.isSingleStep";
    public static final String TEST_NAME = "AllureTestController.testName";
    public static final String DESCRIPTION = "AllureTestController.description";
    public static final String SEVERITY = "AllureTestController.severity";
    public static final String EPIC = "AllureTestController.epic";
    public static final String STORY = "AllureTestController.story";
    public static final String FEATURE = "AllureTestController.feature";
    public static final String TAGS = "AllureTestController.tags";
    public static final String PARAMETERS = "AllureTestController.parameters";
    public static final String CONTENT_TYPE = "AllureTestController.contentType";
    public static final String OWNER = "AllureTestController.owner";
    // Подумать, как добавить сюда таблицы

    private static final String TRUE = Boolean.toString(true); // i.e. "true"

    private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

    private List<AllureTestController> subControllersAndSamplers = new ArrayList<>();

    private transient ListenerNotifier lnf;
    private transient SampleResult res;

    /**
     * Only used in NON parent Mode
     */
    private transient int calls;

    /**
     * Only used in NON parent Mode
     */
    private transient int noFailingSamples;

    /**
     * Cumulated pause time to exclude timer and post/pre processor times
     * Only used in NON parent Mode
     */
    private transient long pauseTime;

    /**
     * Previous end time
     * Only used in NON parent Mode
     */
    private transient long prevEndTime;

    /**
     * Creates a Allure Test Controller
     */
    public AllureTestController() {
        super();
    }
/* 
    @Override
    public PropertiesAccessor<? extends AllureTestController> getProps() {
        return new PropertiesAccessor<>(this, getSchema());
    }*/

    @Override
    protected Object readResolve(){
        super.readResolve();
        lnf = new ListenerNotifier();
        return this;
    }

    //
    // Path to results
    //
    public void setPathToResults(String pathToResults) {
        setProperty(PATH_TO_RESULTS, pathToResults);
    }

    public String getPathToResults() {
        return getPropertyAsString(PATH_TO_RESULTS);
    }

    private List<AllureTestController> getSameSubControllers() {
        return subControllersAndSamplers;
    }

    protected void checkSameSubControllers() {
        for (AllureTestController te : subControllersAndSamplers) {
            if(te instanceof AllureTestController) {
                log.error("AllureTestController can not be in same Controller.");
                    return;
            }
        }
    }

    // Проверка валидности пути к папке + создание папки, если это возможно
    private void testStarted() {
        checkSameSubControllers();

        String pathToResults = getPathToResults();
        File folder = new File(pathToResults);
        if (!folder.getParentFile().exists()) {
            log.error("Folder path {} does not exist.", folder.getParent());
            return;
        }
        if (!folder.exists()) {
            try {
                if (folder.mkdir()) {
                    log.info("Directory {} created.", pathToResults);
                } else {
                    log.error("Failed to create directory {}.", pathToResults);
                    return;
                }
                } catch (SecurityException ex) {
                    log.error("Permission denied: Cannot create directory {}", pathToResults, ex);
                    return;
                }
        } else {
            if (!folder.isDirectory()) {
                log.error("{} is not directory.", pathToResults);
                return;
            } else {
                if (isFolderOverwrite()) { // Потом нужно будет перенести очистку папки на момент начала выполнения контроллера
                    try {
                        Files.walk(Paths.get(pathToResults))
                            .map(Path::toFile)
                            .forEach(File::delete);
                            log.info("Directory {} cleared.", pathToResults);
                    } catch (IOException ex) {
                    log.warn("Failed to clear directory {}.", pathToResults, ex);
                    return;
                    }
                }
            }
        } 
    }

    //
    // Overwrite folder
    //
    public void setFolderOverwrite(boolean ov) {
        setProperty(new BooleanProperty(FOLDER_OVERWRITE, ov));
    }
    
    public boolean isFolderOverwrite() {
        return getPropertyAsBoolean(FOLDER_OVERWRITE, false);
    }

    //
    // Stop test on error
    //
    public void setIsCritical(boolean ic) {
        setProperty(new BooleanProperty(IS_CRITICAL, ic));
    }
    
    public boolean isCriticalTest() {
        return getPropertyAsBoolean(IS_CRITICAL, false);
    }

    //
    // Single step tests
    //
    public void setIsSingleStep(boolean ss) {
        setProperty(new BooleanProperty(IS_SINGLE_STEP, ss));
    }
    
    public boolean isSingleStepTest() {
        return getPropertyAsBoolean(IS_SINGLE_STEP, false);
    }

    //
    // Test
    //
    public void setTestNameField(String te) {
        if(!isSingleStepTest()) {
            setProperty(TEST_NAME, te);
        } else {
            setProperty(TEST_NAME, "");
        }
    }

    public String getTestNameField() {
        return getPropertyAsString(TEST_NAME, "");
    }

    //
    // Description
    //
    public void setDescriptionField(String de) {
        if(!isSingleStepTest()) {
            setProperty(DESCRIPTION, de);
        } else {
            setProperty(DESCRIPTION, "");
        }
    }

    public String getDescriptionField() {
        return getPropertyAsString(DESCRIPTION, "");
    }

    //
    // Severity
    //
    public void setSeverity(String sev) {
        if(sev.toLowerCase().equals("blocker") || sev.toLowerCase().equals("critical") || sev.toLowerCase().equals("normal") || sev.toLowerCase().equals("minor") || sev.toLowerCase().equals("trivial")) {
            setProperty(SEVERITY, sev.toLowerCase());
        } else {
            setProperty(SEVERITY, "normal");
        }
    }

    public String getSeverity() {
        return getPropertyAsString(SEVERITY, "normal");
    }

    //
    // Epic
    //
    public void setEpicField(String ep) {
        setProperty(EPIC, ep);
    }

    public String getEpicField() {
        return getPropertyAsString(EPIC, "");
    }

    //
    // Story
    //
    public void setStoryField(String st) {
        setProperty(STORY, st);
    }

    public String getStoryField() {
        return getPropertyAsString(STORY, "");
    }

    //
    // Feature
    //
    public void setFeatureField(String fe) {
        setProperty(FEATURE, fe);
    }

    public String getFeatureField() {
        return getPropertyAsString(FEATURE, "");
    }

    //
    // Tags
    //
    public void setTagsField(String ta) {
        setProperty(TAGS, ta);
    }

    public String getTagsField() {
        return getPropertyAsString(TAGS, "");
    }

    //
    // Parameters
    //
    public void setParametersField(String pa) {
        setProperty(PARAMETERS, pa);
    }

    public String getParametersField() {
        return getPropertyAsString(PARAMETERS, "");
    }

    //
    // Content type
    //
    public void setContentTypeField(String co) {
        setProperty(CONTENT_TYPE, co);
    }

    public String getContentTypeField() {
        return getPropertyAsString(CONTENT_TYPE, "");
    }

    //
    // Owner
    //
    public void setOwnerField(String ow) {
        setProperty(OWNER, ow);
    }

    public String getOwnerField() {
        return getPropertyAsString(OWNER, "");
    }
}