package mobikylym.jmeter.control; 

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.control.TransactionSampler;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.Controller;
//import org.apache.jmeter.testelement.schema.PropertiesAccessor;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jmeter.threads.SamplePackage;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.Header;




public class AllureTestController extends GenericController {

    private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

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

    private Map<Integer, Boolean> processedSamplers = new HashMap<>();

    
    /**
     * Creates an Allure Test Controller
     */
    public AllureTestController() {
        super();
    }

    @Override
    public Sampler next() {
        if (isFirst()) {
            pathCheck();
        }    
        String filePrefix = UUID.randomUUID().toString();
        JMeterContext ctx = JMeterContextService.getContext();
        Sampler sampler = ctx.getCurrentSampler();
        SampleResult result = ctx.getPreviousResult();
        if (sampler != null && !isFirst()) {
            int samplerHash = result.hashCode();
            if (!processedSamplers.containsKey(samplerHash)) {
                try {
                    if (sampler instanceof HTTPSamplerProxy) {
                        writeToFile(getPathToResults(), filePrefix + "--request.txt", formatRequestData(result));
                        writeToFile(getPathToResults(), filePrefix + "--response.txt", formatResponseData(result));
                    } else {
                        writeToFile(getPathToResults(), filePrefix + "--request.txt", result.getSamplerData().toString());
                        writeToFile(getPathToResults(), filePrefix + "--response.txt", result.getResponseDataAsString());
                    }
                    processedSamplers.put(samplerHash, true);
                } catch (IOException ex) {
                    log.error("Failed to write request or response file.", ex);
                }
            }
        }
        return super.next();
    }

    private void writeToFile(File folder, String filename, String data) throws IOException {
        File file = new File(folder, filename);
        FileUtils.writeStringToFile(file, data, "UTF-8");
    }

    private String formatRequestData(SampleResult result) {
        return result.getRequestHeaders().toString().replaceAll("[aA]uthorization:.*", "Authorization: XXX (Has been replaced for safety)")
            .replaceAll("[xX]-[aA]pi-[tT]oken:.*", "X-Api-Token: XXX (Has been replaced for safety)") + "\n" + result.getSamplerData().toString();
    }

    private String formatResponseData(SampleResult result) {
        String contentType = result.getContentType();
        String responseData = result.getResponseHeaders().toString() + "\nRESPONSE DATA:\n";
        if (contentType.contains("json")) {
            responseData += formatJson(result.getResponseDataAsString());
        } else {
            responseData += result.getResponseDataAsString();
        }
        return responseData;
    }


    private String formatJson(String json) {
        int level = 0;
        boolean inQuotes = false;
        boolean isEscaped = false;
        StringBuilder prettyJson = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\"' && !isEscaped) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{' || c == '[') {
                    prettyJson.append(c);
                    prettyJson.append('\n');
                    prettyJson.append(repeat("  ", ++level));
                } else if (c == '}' || c == ']') {
                    prettyJson.append('\n');
                    prettyJson.append(repeat("  ", --level));
                    prettyJson.append(c);
                } else if (c == ',') {
                    prettyJson.append(c);
                    prettyJson.append('\n');
                    prettyJson.append(repeat("  ", level));
                } else {
                    prettyJson.append(c);
                }
            } else {
                prettyJson.append(c);
            }
            isEscaped = c == '\\' && !isEscaped;
        }
        return prettyJson.toString();
    }
    
    private static String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    

    //
    // Path to results
    //
    public void setPathToResults(String pathToResults) {
        setProperty(ATC_PATH_TO_RESULTS, pathToResults);
    }

    public File getPathToResults() {
        File folder = new File(getPropertyAsString(ATC_PATH_TO_RESULTS), "allure-results");
        return folder;
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
        if(sev.matches("\\s*")) {
            setProperty(ATC_SEVERITY, "normal");
        } else {
            setProperty(ATC_SEVERITY, sev);
        }
    }

    public String getSeverity() {
        if(getPropertyAsString(ATC_SEVERITY).matches("\\s*")) {
            return "normal";
        } else {
            return getPropertyAsString(ATC_SEVERITY, "normal");
        }
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

    //
    // Folder path check and overwright if needed
    //
    private void pathCheck() {
        File folder = getPathToResults();

        if (!folder.getParentFile().exists()) {
            log.error("Directory \"{}\" does not exist.", folder.getParentFile());
            JMeterContextService.getContext().getEngine().stopTest();
            return;
        }

        if (!folder.exists()) {
            try {
                if (folder.mkdir()) {
                    log.info("Directory \"{}\" created.", folder);
                } else {
                    log.error("Failed to create directory \"{}\".", folder);
                    JMeterContextService.getContext().getEngine().stopTest();
                    return;
                }
            } catch (SecurityException ex) {
                log.error("Permission denied: Cannot create directory \"{}\"", folder, ex);
                JMeterContextService.getContext().getEngine().stopTest();
                return;
            }
            return;
        } else {
            if (isFolderOverwrite()) {
                try {
                    FileUtils.cleanDirectory(folder); 
                    log.info("Directory \"{}\" cleared.", folder);
                } catch (IOException ex) {
                    log.error("Failed to clear directory \"{}\".", folder, ex);
                    return;
                }
            }
            return;
        }
    }




}
