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
    final String BROKEN = "broken";
    final String UNKNOWN = "unknown";

    private String testFileId = UUID.randomUUID().toString();
    private Map<String, Object> testFile = new HashMap<>();
    private List<Map<String, Object>> steps = new ArrayList<>();
    private String historyId = "";
    private String testStatus = PASSED;
    private String stepStatus = PASSED;
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

            if (!getEnvironment().trim().isEmpty()) {
                try {
                    writeToFile(getPathToResults(), "environment.properties", getEnvironment().trim(), false);
                } catch (IOException ex) {
                    log.error("Failed to write environment file.", ex);
                }
            }

            File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().trim() + 
            " " + this.getName().trim()).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim());
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

            if (!isContainer()) {
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
                    startFileMaking(getTestId(this.getName()), testFileId, historyId, System.currentTimeMillis(), getTestNameField(this.getName()), this.getComment(), ctx.getThread().getThreadName());
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
                File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().trim() + 
                " " + sampler.getName().trim()).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim());
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
                    getStatuses(result, stepFailureMessage);

                    if (getStepState(result).matches("step")) {
                        if (!isContainer()) {
                            if (!isSingleStepTest()) {
                                continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                            } else {
                                children.add(filePrefix);
    
                                startFileMaking(getTestId(result.getSampleLabel()), filePrefix, historyId, result.getStartTime(), getTestNameField(result.getSampleLabel()), sampler.getComment(), ctx.getThread().getThreadName());
                                continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                                stopFileMaking(filePrefix, result.getEndTime(), stepStatus, (result.isSuccessful()) ? "" : 
                                ("First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" + (result.getFirstAssertionFailureMessage() == null ?
                                "See the attachments." : ("Assertion failure message: " + stepFailureMessage))));
                            }
        
                            if (isCriticalTest() && !testStatus.equals(PASSED)) {
                                if (!isSingleStepTest()) {
                                    stopFileMaking(testFileId, System.currentTimeMillis(), testStatus, testFailureMessage);
                                } else {
                                    try {
                                        writeToFile(getLastTryFolder(), (ctx.getThread().getThreadName().trim() + 
                                        " " + this.getName().trim()).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim(), "false", false);
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
                writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().trim() + 
                " " + this.getName().trim()).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim(), testStatus.equals(PASSED) ? "true" : "false", false);
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

        if (!status.equals(PASSED)) {
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
            writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().trim() + " " +
            (isSingleStepTest() ? JMeterContextService.getContext().getPreviousResult().getSampleLabel().trim() : 
            this.getName().trim())).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim(), ((status.equals(PASSED)) ? "true" : "false") + "\n" + historyId, false);
            
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

            try {
                writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().trim() + " " +
                this.getName().trim()).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim(), ((testStatus.equals(PASSED)) ? "true" : "false"), false);
            } catch (IOException ex) {
                log.error("Failed to write result file.", ex);
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
            .replaceAll("[xX]-[aA][pP][iI]-[kK][eE][yY]:.*", "X-Api-Key: XXX (Has been replaced for safety)")
            .replaceAll("[xX]-[jJ][wW][tT]-[aA][sS]{2}[eE][rR][tT][iI][oO][nN]:.*", "X-Jwt-Assertion: XXX (Has been replaced for safety)") + "\n" + result.getSamplerData().toString();
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

    private void getStatuses(SampleResult result, String stepFailureMessage) {
        if (!result.isSuccessful()){
            AssertionResult[] assertionResults = result.getAssertionResults();
            for (AssertionResult assertionResult : assertionResults) {
                String name = assertionResult.getName();
    
                if (name.startsWith("(F)") || (!assertionResult.isFailure() && !assertionResult.isError())) {
                    continue;
                }

                if (name.matches(".*\\[broken\\]\\s*$")) {
                    if (testStatus.equals(PASSED) && getStepState(result).matches("step")) {
                        testStatus = BROKEN;
                        testFailureMessage = "First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" +
                        (result.getFirstAssertionFailureMessage() == null ? "See the attachments." : ("Assertion failure message: " + stepFailureMessage));
                    }
                    stepStatus = BROKEN;
                    return;
                } else 
                if (name.matches(".*\\[unknown\\]\\s*$")) {
                    if (testStatus.equals(PASSED) && getStepState(result).matches("step")) {
                        testStatus = UNKNOWN;
                        testFailureMessage = "First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" +
                        (result.getFirstAssertionFailureMessage() == null ? "See the attachments." : ("Assertion failure message: " + stepFailureMessage));
                    }
                    stepStatus = UNKNOWN;
                    return;
                } 
            } 
            if (testStatus.equals(PASSED) && getStepState(result).matches("step")) {
                testStatus = FAILED;
                testFailureMessage = "First error on step \"" + getTestNameField(result.getSampleLabel()) + "\".\n" +
                (result.getFirstAssertionFailureMessage() == null ? "See the attachments." : ("Assertion failure message: " + stepFailureMessage));
            }
            stepStatus = FAILED;
            return;
        } else {
            stepStatus = PASSED;
        }
    }

    private String getStepState(SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        String beforeString = isContainer() ? "(F) BEFORE" : "(F) before";
        String afterString = isContainer() ? "(F) AFTER" : "(F) after";
        
        if (stepState.equals("before")) {
            for (AssertionResult assertionResult : assertionResults) {
                String name = assertionResult.getName();
                if (name.startsWith(beforeString)) {
                    return stepState;
                }
            }
        }

        if (stepState.equals("after")) {
            return stepState;
        } else {
            for (AssertionResult assertionResult : assertionResults) {
                String name = assertionResult.getName();
                if (name.startsWith(afterString)) {
                    return stepState = "after";
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
            String requestContentType = "text/plain";
            String responseContentType = "text/plain";
            String requestData = sampler instanceof HTTPSamplerProxy ? formatRequestData(result) : result.getSamplerData().toString();
            String responseData = sampler instanceof HTTPSamplerProxy ? formatResponseData(result) : result.getResponseDataAsString();
    
            try {
                writeToFile(getPathToResults(), uuid + "-request-attachment", requestData, false);
                writeToFile(getPathToResults(), uuid + "-response-attachment", responseData, false);
            } catch (IOException ex) {
                log.error("Failed to write request or response file.", ex);
            }

            if (sampler instanceof HTTPSamplerProxy) {
                requestContentType = getResponseContentType(result.getRequestHeaders().toString().toLowerCase());
                responseContentType = result.getMediaType().contains("json") ? "application/json" : result.getMediaType().contains("xml") ? "application/xml" : "text/plain";
            }
    
            attachments.add(createAttachment("Request", uuid + "-request-attachment", requestContentType));
            attachments.add(createAttachment("Response", uuid + "-response-attachment", responseContentType));
        }
    
        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName().replace("\"", "\\\"");
            if (name.startsWith("(F) attach:")) {
                allAttachments.append(name.replaceAll("\\(F\\) attach:", "").trim()).append("\n");
            }
        }
    
        if (allAttachments.length() > 0) {
            attachConstructor(attachments, allAttachments.toString());
        }
    
        if (!attachments.isEmpty()) {
            attachTo.put("attachments", attachments);
        }
    }

    private String getResponseContentType(String headers) {
        Pattern pattern = Pattern.compile("content-type:([^\\n]+)");
        Matcher matcher = pattern.matcher(headers);

        if (matcher.find()) {
            String contentType = matcher.group(1);
            if (contentType.contains("json")) {
                return "application/json";
            } else if (contentType.contains("xml")) {
                return "application/xml";
            }
        }
        return "text/plain";
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
        File folder = new File(getPropertyAsString(ATC_PATH_TO_RESULTS), ".last-try-results");

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
        Pattern pattern = Pattern.compile("^\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(testNameString.trim());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    //
    // Test name
    //
    public String getTestNameField(String testNameString) {
        return testNameString.trim().replaceFirst("^\\[(.*?)\\]\\s*", "");
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
                attachments.add(createAttachment(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            }
        }
    }
    
    private Map<String, Object> createAttachment(String name, String source, String type) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", name);
        attachment.put("source", source);
        attachment.put("type", type);
        return attachment;
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
        addLabelIfNotEmpty(labelArray, "allure_id", testId);
        addLabelIfNotEmpty(labelArray, "epic", getEpicField());
        addLabelIfNotEmpty(labelArray, "feature", getFeatureField());
        addLabelIfNotEmpty(labelArray, "story", getStoryField());
        addLabelIfNotEmpty(labelArray, "owner", getOwnerField());

        String[] tags = getTagsField().split(",");
        Pattern pattern1 = Pattern.compile("\\s*");

        for (String value : tags) {
            if (!pattern1.matcher(value).matches()) {
                addLabel(labelArray, "tag", value);
            }
        }

        String[] extraLabels = getExtraLabelsField().split("\n");
        Pattern pattern2 = Pattern.compile("[^,]+,[^,]+");

        for (String line : extraLabels) {
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