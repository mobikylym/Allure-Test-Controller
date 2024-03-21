package mobikylym.jmeter.control; 

import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.assertions.AssertionResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllureTestController extends GenericController {

    private static final long serialVersionUID = 2001L;

    public static final String ATC_PATH_TO_RESULTS = "AllureTestController.pathToResults";
    public static final String ATC_FOLDER_OVERWRITE = "AllureTestController.folderOverwrite";
    public static final String ATC_IS_CRITICAL = "AllureTestController.isCritical";
    public static final String ATC_IS_SINGLE_STEP = "AllureTestController.isSingleStep";
    public static final String ATC_WITHOUT_CONTENT = "AllureTestController.withoutContent";
    public static final String ATC_WITHOUT_NON_HTTP = "AllureTestController.withoutNonHTTP";
    public static final String ATC_IS_CONTAINER = "AllureTestController.isContainer";
    public static final String ATC_DEBUG_MODE = "AllureTestController.debugMode";
    public static final String ATC_TEST_NAME = "AllureTestController.testName";
    public static final String ATC_DESCRIPTION = "AllureTestController.description";
    public static final String ATC_SEVERITY = "AllureTestController.severity";
    public static final String ATC_EPIC = "AllureTestController.epic";
    public static final String ATC_STORY = "AllureTestController.story";
    public static final String ATC_FEATURE = "AllureTestController.feature";
    public static final String ATC_TAGS = "AllureTestController.tags";
    public static final String ATC_PARAMETERS = "AllureTestController.parameters";
    public static final String ATC_OWNER = "AllureTestController.owner";
    public static final String ATC_LINKS = "AllureTestController.links";
    public static final String ATC_ATTACH = "AllureTestController.attach";
    public static final String ATC_EXTRA_LABELS = "AllureTestController.extraLabels";
    public static final String ATC_ENVIRONMENT = "AllureTestController.environment";

    private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

    final String PASSED = "passed";
    final String FAILED = "failed";

    private String testFileId = UUID.randomUUID().toString();
    private Map<String, Object> testFile = new HashMap<>();
    private List<Map<String, Object>> steps = new ArrayList<>();
    private String historyId = "";
    private String testStatus = PASSED;
    private String testFailureMessage = "";
    private Map<Integer, Boolean> processedSamplers = new HashMap<>();

    private String containerId = UUID.randomUUID().toString();
    private Map<String, Object> containerFile = new HashMap<>();
    private List<String> children = new ArrayList<>();
    private List<Map<String, Object>> befores = new ArrayList<>();
    private List<Map<String, Object>> afters = new ArrayList<>();
    private String stepState = "before";

    /**
     * Creates an Allure Test Controller
     */
    public AllureTestController() {
        super();
    }

    /**
     * This method from class GenericController describes the actions performed between samplers  
     * within the controller. The first loop starts before the execution of all nested samplers,  
     * and the last one - after the execution of the last one.
     */
    @Override
    public Sampler next() {    
        String filePrefix = UUID.randomUUID().toString();
        JMeterContext ctx = JMeterContextService.getContext();
        Sampler sampler = ctx.getCurrentSampler();
        SampleResult result = ctx.getPreviousResult(); 

        if (isFirst()) {
            if (!pathCheck()) {
                ctx.getThread().stop();
                return null;
            }

            if (!isContainer()) {
                if (!getEnvironment().trim().isEmpty()) {
                    try {
                        writeToFile(getPathToResults(), "environment.properties", getEnvironment().trim(), false);
                    } catch (IOException ex) {
                        log.error("Failed to write environment file.", ex);
                    }
                }
    
                File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                " " + (isSingleStepTest() ? this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() : getPropertyAsString(ATC_TEST_NAME).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim())).trim());
                if (file.exists() && JMeterUtils.getPropDefault("allure.retry.fallen", "false").equals("true")) {
                    try {
                        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                        if (lines.get(0).equals("true")) {
                            return null;
                        }
                    } catch (IOException e) {
                        log.error("Error reading the file: " + e.getMessage());
                    }
                }
    
                if (!isSingleStepTest()) {
                    if (file.exists()) {
                        try {
                            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                            if (lines.size() >= 2) {
                                historyId = lines.get(1);
                            } else {
                                historyId = UUID.randomUUID().toString();
                            }
                        } catch (IOException e) {
                            log.error("Error reading the file: " + e.getMessage());
                        }
                    } else {
                        historyId = UUID.randomUUID().toString();
                    }
                    children.add(testFileId);
                    startFileMaking(getTestId(getPropertyAsString(ATC_TEST_NAME)), testFileId, historyId, System.currentTimeMillis(), getTestNameField(getPropertyAsString(ATC_TEST_NAME)), getDescriptionField(), ctx.getThread().getThreadName());
                }
            } else {
                try {
                    writeToFile(getLastTryFolder(), ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + " -container", "", false);
                } catch (IOException e) {
                    log.error("Error overwriting the container file: " + e.getMessage());
                }
            }
            
            containerFile.put("uuid", containerId);

            if (this.getSubControllers().size() > 0 && this.getSubControllers().get(0) instanceof GenericController && result != null) {
                processedSamplers.put(result.hashCode(), true);
            }
        }

        if (sampler != null && !isFirst()) {
            int samplerHash = result.hashCode();
            if (isSingleStepTest() && getStepState(result).matches("step")) {
                File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                " " + sampler.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim());
                if (file.exists()) {
                    try {
                        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                        if (lines.get(0).equals("true") && result.isSuccessful() && JMeterUtils.getPropDefault("allure.retry.fallen", "false").equals("true")) {
                            processedSamplers.put(samplerHash, true);
                            return super.next();
                        } else {
                            if (lines.size() >= 2) {
                                historyId = lines.get(1);
                            } else {
                                historyId = UUID.randomUUID().toString();
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error reading the file: " + e.getMessage());
                    }
                } else {
                    historyId = UUID.randomUUID().toString();
                } 
            }
            
            if (!processedSamplers.containsKey(samplerHash)) {
                if (sampler instanceof HTTPSamplerProxy || !isWithoutNonHTTP() || (!result.isSuccessful() && isCriticalTest())) {
                    String stepFailureMessage = (result.getFirstAssertionFailureMessage() == null) ? "" : result.getFirstAssertionFailureMessage();

                    if (getStepState(result).matches("step")) {
                        if (!isContainer()) {
                            if (!result.isSuccessful() && testStatus.equals(PASSED)) {
                                testStatus = FAILED;
                                testFailureMessage = "First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" +
                                (result.getFirstAssertionFailureMessage() == null ? "See the attachments." : ("Assertion failure message: " + stepFailureMessage));
                            }
        
                            if (!isSingleStepTest()) {
                                continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                            } else {
                                children.add(filePrefix);
    
                                startFileMaking(getTestId(result.getSampleLabel()), filePrefix, historyId, result.getStartTime(), getTestNameField(result.getSampleLabel()), sampler.getComment(), ctx.getThread().getThreadName());
                                continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                                stopFileMaking(filePrefix, result.getEndTime(), (result.isSuccessful()) ? PASSED : FAILED, (result.isSuccessful()) ? "" : 
                                ("First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" + (result.getFirstAssertionFailureMessage() == null ?
                                "See the attachments." : ("Assertion failure message: " + stepFailureMessage))));
                            }
        
                            if (isCriticalTest() && testStatus.equals(FAILED)) {
                                if (!isSingleStepTest()) {
                                    stopFileMaking(testFileId, System.currentTimeMillis(), testStatus, testFailureMessage);
                                } else {
                                    try {
                                        writeToFile(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                                        " " + this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim(), "false", false);
                                    } catch (IOException ex) {
                                        log.error("Failed to write last result file.", ex);
                                    }
                                }
                                log.error("Test was stopped on sampler labeled \"{}\" fail.", result.getSampleLabel());
                                completeContainer();
                                ctx.getThread().stop();
                                return null;
                            }
                        }
                    } else {
                        continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                    }
                }
                processedSamplers.put(samplerHash, true);
            }
        }
        return super.next();
    }

    /**
     * This method from class GenericController is used to perform additional actions 
     * at the moment when all samplers from the controller have already been executed.  
     */
    @Override
    protected Sampler nextIsNull() throws NextIsNullException {
        if (!isSingleStepTest()) {
            if (!isContainer()) {
                stopFileMaking(testFileId, System.currentTimeMillis(), testStatus, testFailureMessage);
            }
        } else {
            try {
                writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                " " + this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim(), testStatus.equals(PASSED) ? "true" : "false", false);
            } catch (IOException ex) {
                log.error("Failed to write last result file.", ex);
            }
        }
        completeContainer();
        return super.nextIsNull();
    }

    private void startFileMaking(String testId, String uuid, String history, Long startTime, String testName, String description, String threadName) {
        testFile = new HashMap<>();

        testFile.put("name", testName.trim());
        testFile.put("description", description.trim());
        testFile.put("start", startTime);
        testFile.put("uuid", uuid);
        testFile.put("historyId", history);
        testFile.put("fullName", threadName.trim() + "  " + testName.trim());
        ParametersConstructor(testFile, getParametersField());
        linkConstructor(testFile);
        labelsConstructor(testFile, testId);
    }

    private void continueFileMaking(String uuid, String failureMessage, Sampler sampler, SampleResult result) {
        if (isSingleStepTest()) {
            steps = new ArrayList<>();
        }

        String stepStatus = (result.isSuccessful()) ? PASSED : FAILED;
        Map<String, Object> currentStep = new HashMap<>();

        currentStep.put("name", getTestNameField(result.getSampleLabel()).trim());
        currentStep.put("status", stepStatus);
        currentStep.put("start", result.getStartTime());
        currentStep.put("stop", result.getEndTime()); 
        getStepParameters(currentStep, result);
        getAssertionResults(currentStep, result);
        getStepAttach(currentStep, sampler, result, uuid);

        if (!result.isSuccessful()) {
            Map<String, Object> statusDetails = new HashMap<>();
            statusDetails.put("message", failureMessage);
            currentStep.put("statusDetails", statusDetails);
        }

        if (stepState.equals("before")) {
            befores.add(currentStep);
        } else

        if (stepState.equals("step")) {
            steps.add(currentStep);
        } else

        if (stepState.equals("after")) {
            afters.add(currentStep);
        }
        
    }

    private void stopFileMaking(String uuid, Long stopTime, String status, String failureMessage) {

        testFile.put("steps", steps);
        testFile.put("stop", stopTime);
        testFile.put("status", status);

        if (status.equals(FAILED)) {
            Map<String, Object> statusDetails = new HashMap<>();
            statusDetails.put("message", failureMessage);
            testFile.put("statusDetails", statusDetails);
        }

        if (!getAttachField().matches("\\s*")) {
            List<Map<String, Object>> attachArray = new ArrayList<>();
            attachConstructor(attachArray, getAttachField());
            testFile.put("attachments", attachArray);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(testFile);

            if (isDebugMode()) {
                jsonString = formatJson(jsonString);
            }

            writeToFile(getPathToResults(), uuid + "-result.json", jsonString, false);
            writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + " " +
            ((isSingleStepTest()) ? JMeterContextService.getContext().getPreviousResult().getSampleLabel().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() : 
            getPropertyAsString(ATC_TEST_NAME).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim())).trim(), ((status.equals(PASSED)) ? "true" : "false") + "\n" + historyId, false);
            
            writeToFile(getLastTryFolder(), JMeterContextService.getContext().getThread().getThreadName()
            .replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + " -container", uuid + "\n", true);
        } catch (IOException ex) {
            log.error("Failed to write result file.", ex);
        }
    }

    private void completeContainer() {
        if (isContainer()) {
            File file = new File(getLastTryFolder(), JMeterContextService.getContext().getThread().getThreadName()
            .replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + " -container");
            try {
                List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (!line.matches("\\s*")) {
                        children.add(line);
                    }
                }
            } catch (IOException ex) {
                log.error("Error reading the container file: " + ex.getMessage());
            }
        }

        if ((!befores.isEmpty() || !afters.isEmpty()) && !children.isEmpty()) {
            
            containerFile.put("children", children);
            ObjectMapper mapper = new ObjectMapper();

            if (!isContainer()) {
                containerFile.put("befores", befores);
                containerFile.put("afters", afters);
    
                try {
                    String jsonString = mapper.writeValueAsString(containerFile);
    
                    if (isDebugMode()) {
                        jsonString = formatJson(jsonString);
                    }
    
                    writeToFile(getPathToResults(), containerId + "-container.json", jsonString, false);
                } catch (IOException ex) {
                    log.error("Failed to write container file.", ex);
                }
            } else {
                if (!befores.isEmpty()) {
                    containerId = "0000a" + containerId.substring(5);
                    containerFile.put("uuid", containerId);
                    containerFile.put("befores", befores);

                    try {
                        String jsonString = mapper.writeValueAsString(containerFile);
        
                        if (isDebugMode()) {
                            jsonString = formatJson(jsonString);
                        }
        
                        writeToFile(getPathToResults(), containerId + "-container.json", jsonString, false);
                    } catch (IOException ex) {
                        log.error("Failed to write container file.", ex);
                    }
                }

                if (!afters.isEmpty()) {
                    containerId = "zzzzz" + containerId.substring(5);
                    containerFile.put("uuid", containerId);
                    containerFile.remove("befores");
                    containerFile.put("afters", afters);

                    try {
                        String jsonString = mapper.writeValueAsString(containerFile);
        
                        if (isDebugMode()) {
                            jsonString = formatJson(jsonString);
                        }
        
                        writeToFile(getPathToResults(), containerId + "-container.json", jsonString, false);
                    } catch (IOException ex) {
                        log.error("Failed to write container file.", ex);
                    }
                }
            } 
        }
    }

    private void writeToFile(File folder, String filename, String data, boolean checkUpdate) throws IOException {
        File file = new File(folder, filename);
        if (checkUpdate) {
            long lastModified = file.lastModified();
            long currentTime = System.currentTimeMillis();
            boolean shouldAppend = file.exists() && (currentTime - lastModified) <= 60 * 30 * 1000;
            if (shouldAppend) {
                FileUtils.writeStringToFile(file, data, "UTF-8", true);
            } else {
                FileUtils.writeStringToFile(file, data, "UTF-8", false);
            }
        } else {
            FileUtils.writeStringToFile(file, data, "UTF-8", false);
        }
    }

    private String formatRequestData(SampleResult result) {
        return result.getRequestHeaders().toString().replaceAll("[aA][uU][tT][hH][oO][rR][iI][zZ][aA][tT][iI][oO][nN]:.*", "Authorization: XXX (Has been replaced for safety)")
            .replaceAll("[xX]-[aA][pP][iI]-[tT][oO][kK][eE][nN]:.*", "X-Api-Token: XXX (Has been replaced for safety)")
            .replaceAll("[xX]-[aA][pP][iI]-[kK][eE][yY]:.*", "X-Api-Key: XXX (Has been replaced for safety)") + "\n" + result.getSamplerData().toString();
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

    /**
     * The following two methods are needed for the correct display of content in JSON 
     * format. Without them, everything will be in one line.  
     */
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

    /**
     * This method is needed to obtain the results of all assertions from each sampler.  
     * The results are immediately converted into the necessary JSON format.  
     */
    private void getAssertionResults(Map<String, Object> resultsTo, SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        List<Map<String, Object>> results = new ArrayList<>();
    
        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName();

            if (name.startsWith("(F)")) {
                continue;
            }

            String status = (assertionResult.isFailure() || assertionResult.isError()) ? FAILED : PASSED;
            String message = (status.equals(PASSED)) ? "" : assertionResult.getFailureMessage();
    
            Map<String, Object> resultJson = new HashMap<>();
            resultJson.put("name", name.trim());
            resultJson.put("status", status);
    
            if (status.equals(FAILED)) {
                Map<String, Object> statusDetails = new HashMap<>();
                statusDetails.put("message", message.trim());
                resultJson.put("statusDetails", statusDetails);
            }
    
            results.add(resultJson);
        }
    
        if (!results.isEmpty()) {
            resultsTo.put("steps", results);
        }
    }

    private String getStepState(SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        
        if (!isContainer()) {
            if (stepState.equals("before")) {
                for (AssertionResult assertionResult : assertionResults) {
                    String name = assertionResult.getName();
                    if (name.startsWith("(F) before")) {
                        return stepState;
                    }
                }
            }
    
            if (stepState.equals("after")) {
                return stepState;
            } else {
                for (AssertionResult assertionResult : assertionResults) {
                    String name = assertionResult.getName();
                    if (name.startsWith("(F) after")) {
                        return stepState = "after";
                    }
                }
            }
        } else {
            if (stepState.equals("before")) {
                for (AssertionResult assertionResult : assertionResults) {
                    String name = assertionResult.getName();
                    if (name.startsWith("(F) BEFORE")) {
                        return stepState;
                    }
                }
            }
    
            if (stepState.equals("after")) {
                return stepState;
            } else {
                for (AssertionResult assertionResult : assertionResults) {
                    String name = assertionResult.getName();
                    if (name.startsWith("(F) AFTER")) {
                        return stepState = "after";
                    }
                }
            }
        }

        return stepState = "step";
    }

    /**
     * This method is needed to get the step parameters. The parameters are JMeter variables.
     * In this case, the values of the variables that they have after the step execution are taken.
     * Instructions for the correct output of step parameters are written in the repository.
     */
    private void getStepParameters(Map<String, Object> paramsTo, SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
    
        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName().replace("\"", "\\\"");
            if (name.startsWith("(F) parameters:")) {
                String params = name.replaceAll("\\(F\\) parameters:", "").trim();
                if (!params.matches("\\s*")) {
                    ParametersConstructor(paramsTo, params);
                }
            }
        }
    }

    private void getStepAttach(Map<String, Object> attachTo, Sampler sampler, SampleResult result, String uuid) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        List<Map<String, Object>> attachments = new ArrayList<>();
        StringBuilder allAttachments = new StringBuilder();

        if (!isWithoutContent() || (isCriticalTest() && !result.isSuccessful())) {
            try {
                if (sampler instanceof HTTPSamplerProxy) {
                    writeToFile(getPathToResults(), uuid + "-request-attachment", formatRequestData(result), false);
                    writeToFile(getPathToResults(), uuid + "-response-attachment", formatResponseData(result), false);
                } else {
                    writeToFile(getPathToResults(), uuid + "-request-attachment", result.getSamplerData().toString(), false);
                    writeToFile(getPathToResults(), uuid + "-response-attachment", result.getResponseDataAsString(), false);
                }
            } catch (IOException ex) {
                log.error("Failed to write request or response file.", ex);
            }

            Map<String, Object> requestAttachment = new HashMap<>();
            requestAttachment.put("name", "Request");
            requestAttachment.put("source", uuid + "-request-attachment");
            requestAttachment.put("type", "application/json");
            attachments.add(requestAttachment);

            Map<String, Object> responseAttachment = new HashMap<>();
            responseAttachment.put("name", "Response");
            responseAttachment.put("source", uuid + "-response-attachment");
            responseAttachment.put("type", "application/json");
            attachments.add(responseAttachment);
        }
    
        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName().replace("\"", "\\\"");
            if (name.startsWith("(F) attach:")) {
                String attach = name.replaceAll("\\(F\\) attach:", "").trim();
                allAttachments.append(attach.trim()).append("\n");
            }
        }

        if (allAttachments.length() > 0) {
            attachConstructor(attachments, allAttachments.toString());
        } 

        if (!attachments.isEmpty()) {
            attachTo.put("attachments", attachments);
        }
    }

    //
    // Path to results
    //
    public void setPathToResults(String pathToResults) {
        setProperty(ATC_PATH_TO_RESULTS, pathToResults);
    }

    public File getPathToResults() {
        if (getPropertyAsString(ATC_PATH_TO_RESULTS).matches("\\s*")) {
            return null;
        } else {
            File folder = new File(getPropertyAsString(ATC_PATH_TO_RESULTS), "allure-results");
            return folder;
        }
    }

    private boolean pathCheck() {
        File folder = getPathToResults();

        if (folder == null) {
            log.error("Choose directory.");
            return false;
        }

        if (!folder.getParentFile().exists()) {
            log.error("Directory path \"{}\" does not exist.", folder.getParentFile());
            return false;
        }

        if (!folder.exists()) {
            try {
                if (folder.mkdir()) {
                    log.info("Directory \"{}\" created.", folder);
                } else {
                    log.error("Failed to create directory \"{}\".", folder);
                    return false;
                }
            } catch (SecurityException ex) {
                log.error("Permission denied: Cannot create directory \"{}\"", folder, ex);
                return false;
            }
            return true;
        } else {
            if (isFolderOverwrite()) {
                try {
                    FileUtils.cleanDirectory(folder); 
                    log.info("Directory \"{}\" cleared.", folder);
                } catch (IOException ex) {
                    log.error("Failed to clear directory \"{}\".", folder, ex);
                    return false;
                }
            }
            return true;
        }
    }

    public File getLastTryFolder() {
        File folder = new File(getPropertyAsString(ATC_PATH_TO_RESULTS), "last-try-results");

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
    // Without content
    //
    public void setWithoutContent(boolean wc) {
        setProperty(new BooleanProperty(ATC_WITHOUT_CONTENT, wc));
    }
    
    public boolean isWithoutContent() {
        return getPropertyAsBoolean(ATC_WITHOUT_CONTENT, false);
    }

    //
    // Without non-HTTP steps
    //
    public void setWithoutNonHTTP(boolean wn) {
        setProperty(new BooleanProperty(ATC_WITHOUT_NON_HTTP, wn));
    }
    
    public boolean isWithoutNonHTTP() {
        return getPropertyAsBoolean(ATC_WITHOUT_NON_HTTP, false);
    }

    //
    // Is container
    //
    public void setIsContainer(boolean ic) {
        setProperty(new BooleanProperty(ATC_IS_CONTAINER, ic));
    }
    
    public boolean isContainer() {
        return getPropertyAsBoolean(ATC_IS_CONTAINER, false);
    }

    //
    // Debug mode
    //
    public void setDebugMode(boolean dm) {
        setProperty(new BooleanProperty(ATC_DEBUG_MODE, dm));
    }
    
    public boolean isDebugMode() {
        return getPropertyAsBoolean(ATC_DEBUG_MODE, false);
    }

    //
    // Test id
    //

    public String getTestId(String testNameString) {
        if (testNameString.matches("\\d+\\s*-.+")) {
            return testNameString.split("-")[0].trim();
        } else {
            return "";
        }
    }

    //
    // Test name
    //
    public void setTestNameField(String te) {
        setProperty(ATC_TEST_NAME, te);
    }

    public String getTestNameField(String testNameString) {
        return testNameString.replaceFirst("^\\d+\\s*-\\s*", "");
    }

    //
    // Description
    //
    public void setDescriptionField(String de) {
        setProperty(ATC_DESCRIPTION, de);
    }

    public String getDescriptionField() {
        return getPropertyAsString(ATC_DESCRIPTION);
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
            return getPropertyAsString(ATC_SEVERITY);
        }
    }

    //
    // Epic
    //
    public void setEpicField(String ep) {
        setProperty(ATC_EPIC, ep);
    }

    public String getEpicField() {
        return getPropertyAsString(ATC_EPIC);
    }

    //
    // Feature
    //
    public void setFeatureField(String fe) {
        setProperty(ATC_FEATURE, fe);
    }

    public String getFeatureField() {
        return getPropertyAsString(ATC_FEATURE);
    }

    //
    // Story
    //
    public void setStoryField(String st) {
        setProperty(ATC_STORY, st);
    }

    public String getStoryField() {
        return getPropertyAsString(ATC_STORY);
    }

    //
    // Tags
    //
    public void setTagsField(String ta) {
        setProperty(ATC_TAGS, ta);
    }

    public String getTagsField() {
        return getPropertyAsString(ATC_TAGS);
    }

    //
    // Parameters
    //
    public void setParametersField(String pa) {
        setProperty(ATC_PARAMETERS, pa);
    }

    public String getParametersField() {
        return getPropertyAsString(ATC_PARAMETERS);
    }

    private void ParametersConstructor(Map<String, Object> paramsTo, String params) {
        List<Map<String, Object>> paramsArray = new ArrayList<>();
    
        String[] values = params.split(",");
        
        Pattern pattern = Pattern.compile("\\s*");
        JMeterContext context = JMeterContextService.getContext();
        
        for (String value : values) {
            value = value.trim();
            if (!pattern.matcher(value).matches()) {
                String variableValue = context.getVariables().get(value);
                Map<String, Object> parameter = new HashMap<>();
                parameter.put("name", value);
                parameter.put("value", variableValue);
                paramsArray.add(parameter);
            }
        }
        
        if (!paramsArray.isEmpty()) {
            paramsTo.put("parameters", paramsArray);
        }
    }

    //
    // Owner
    //
    public void setOwnerField(String ow) {
        setProperty(ATC_OWNER, ow);
    }

    public String getOwnerField() {
        return getPropertyAsString(ATC_OWNER);
    }

    //
    // Links
    //
    public void setLinksField(String li) {
        setProperty(ATC_LINKS, li);
    }

    public String getLinksField() {
        return getPropertyAsString(ATC_LINKS);
    }

    private void linkConstructor(Map<String, Object> linksTo) {
        List<Map<String, Object>> linkArray = new ArrayList<>();
    
        String links = getLinksField();
        String[] lines = links.split("\n");
    
        Pattern pattern1 = Pattern.compile("[^,]+,[^,]+");
        Pattern pattern2 = Pattern.compile("[^,]+");
    
        for (String line : lines) {
            boolean isIssue = false;
            if (line.startsWith("(I)")) {
                line = line.substring(3);
                isIssue = true;
            }
    
            Matcher matcher1 = pattern1.matcher(line);
            Matcher matcher2 = pattern2.matcher(line);
            Map<String, Object> link = new HashMap<>();
            if (isIssue) {
                link.put("type", "issue");
            }
    
            if (matcher1.matches()) {
                String[] parts = line.split(",");
                link.put("name", parts[0].trim());
                link.put("url", parts[1].trim());
                linkArray.add(link);
            } else if (matcher2.matches() && line.trim().matches("^https?://.*")) {
                String value = line.trim();
                String name = value.substring(value.lastIndexOf("/") + 1).trim();
                link.put("name", name);
                link.put("url", value);
                linkArray.add(link);
            } else if (matcher2.matches() && JMeterUtils.getPropDefault("allure.tmsLink.prefix", null) != null) {
                String value = line.trim();
                String name = value.contains("/") ? value.substring(value.lastIndexOf("/") + 1).trim() : value;
                if (!isIssue) {
                    link.put("type", "tms");
                }
                link.put("name", name);
                link.put("url", JMeterUtils.getProperty("allure.tmsLink.prefix") + value);
                linkArray.add(link);
            }
        }
    
        if (!linkArray.isEmpty()) {
            linksTo.put("links", linkArray);
        }
    }

    //
    // Attachments
    //
    public void setAttachField(String at) {
        setProperty(ATC_ATTACH, at);
    }

    public String getAttachField() {
        return getPropertyAsString(ATC_ATTACH);
    }

    private void attachConstructor(List<Map<String, Object>> attachments, String attach) {
        String[] lines = attach.split("\n");
    
        Pattern pattern = Pattern.compile("[^,]+,[^,]+,[^,]+");
    
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String[] parts = line.split(",");
                Map<String, Object> attachment = new HashMap<>();
                attachment.put("name", parts[0].trim());
                attachment.put("source", parts[1].trim());
                attachment.put("type", parts[2].trim());
                attachments.add(attachment);
            }
        }
    }

    //
    // Extra Labels
    //
    public void setExtraLabelsField(String ex) {
        setProperty(ATC_EXTRA_LABELS, ex);
    }

    public String getExtraLabelsField() {
        return getPropertyAsString(ATC_EXTRA_LABELS);
    }

    private void labelsConstructor(Map<String, Object> labelsTo, String testId) {
        List<Map<String, Object>> labelArray = new ArrayList<>();

        addLabel(labelArray, "severity", getSeverity());
        if (!testId.matches("\\s*")) {
            addLabel(labelArray, "allure_id", testId);
        }
        addLabelIfNotEmpty(labelArray, "epic", getEpicField());
        addLabelIfNotEmpty(labelArray, "feature", getFeatureField());
        addLabelIfNotEmpty(labelArray, "story", getStoryField());
        addLabelIfNotEmpty(labelArray, "owner", getOwnerField());

        String tags = getTagsField();
        String[] values = tags.split(",");
        Pattern pattern1 = Pattern.compile("\\s*");

        for (String value : values) {
            if (!pattern1.matcher(value).matches()) {
                addLabel(labelArray, "tag", value);
            }
        }

        String extraLabels = getExtraLabelsField();
        String[] lines = extraLabels.split("\n");
        Pattern pattern2 = Pattern.compile("[^,]+,[^,]+");

        for (String line : lines) {
            Matcher matcher = pattern2.matcher(line);
            if (matcher.matches()) {
                String[] parts = line.split(",");
                addLabel(labelArray, parts[0], parts[1]);
            }
        }
    
        labelsTo.put("labels", labelArray);
    }

    private void addLabel(List<Map<String, Object>> labelArray, String name, String value) {
        Map<String, Object> label = new HashMap<>();
        label.put("name", name.trim());
        label.put("value", value.trim());
        labelArray.add(label);
    }
    
    private void addLabelIfNotEmpty(List<Map<String, Object>> labelArray, String name, String value) {
        if (!value.matches("\\s*")) {
            addLabel(labelArray, name, value);
        }
    }

    //
    // Environment
    //
    public void setEnvironment(String en) {
        setProperty(ATC_ENVIRONMENT, en);
    }

    public String getEnvironment() {
        return getPropertyAsString(ATC_ENVIRONMENT);
    }
}