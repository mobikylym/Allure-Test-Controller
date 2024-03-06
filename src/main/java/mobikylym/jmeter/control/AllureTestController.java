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
    public static final String ATC_EXTRA_LABELS = "AllureTestController.extraLabels";

    private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

    private String testFileId = UUID.randomUUID().toString();
    private String testFile = "";
    private String testStatus = "passed";
    private String testFailureMessage = "";
    private Map<Integer, Boolean> processedSamplers = new HashMap<>();

    final String PASSED = "passed";
    final String FAILED = "failed";

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

            File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
            " " + (isSingleStepTest() ? this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() : getPropertyAsString(ATC_TEST_NAME).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim())).trim());
            if (file.exists() && JMeterUtils.getPropDefault("allure.retry.fallen", "false").equals("true")) {
                try {
                    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    if (content.equals("true")) {
                        return null;
                    }
                } catch (IOException e) {
                    log.error("Ошибка при чтении файла: " + e.getMessage());
                }
            }

            if (!isSingleStepTest()) {
                startFileMaking(getTestId(getPropertyAsString(ATC_TEST_NAME)), testFileId, String.valueOf(System.currentTimeMillis()), getTestNameField(getPropertyAsString(ATC_TEST_NAME)), getDescriptionField(), ctx.getThread().getThreadName().replace("\"", "\\\""));
            }

            if (this.getSubControllers().size() > 0 && this.getSubControllers().get(0) instanceof GenericController && result != null) {
                processedSamplers.put(result.hashCode(), true);
            }
        }

        if (sampler != null && !isFirst()) {
            int samplerHash = result.hashCode();
            if (isSingleStepTest()) {
                File file = new File(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                " " + sampler.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim());
                if (file.exists() && JMeterUtils.getPropDefault("allure.retry.fallen", "false").equals("true")) {
                    try {
                        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        if (content.equals("true") && result.isSuccessful()) {
                            processedSamplers.put(samplerHash, true);
                            return super.next();
                        }
                    } catch (IOException e) {
                        log.error("Ошибка при чтении файла: " + e.getMessage());
                    }
                }
            }
            
            if (!processedSamplers.containsKey(samplerHash)) {
                if (sampler instanceof HTTPSamplerProxy || !isWithoutNonHTTP() || (!result.isSuccessful() && isCriticalTest())) {
                    String stepFailureMessage = (result.getFirstAssertionFailureMessage() == null) ? "" : result.getFirstAssertionFailureMessage().replace("\"", "\\\"");

                    if (!result.isSuccessful() && testStatus.equals(PASSED)){
                        testStatus = FAILED;
                        testFailureMessage = "Error on step \\\"" + result.getSampleLabel().replace("\"", "\\\"") + 
                        "\\\".\\nAssertion failure message: " + stepFailureMessage;
                    }

                    if (!isSingleStepTest()) {
                        continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                    } else {
                        startFileMaking(getTestId(result.getSampleLabel()), filePrefix, String.valueOf(result.getStartTime()), getTestNameField(result.getSampleLabel()), sampler.getComment().replace("\"", "\\\""), ctx.getThread().getThreadName().replace("\"", "\\\""));
                        continueFileMaking(filePrefix, stepFailureMessage, sampler, result);
                        stopFileMaking(filePrefix, String.valueOf(result.getEndTime()), (result.isSuccessful()) ? PASSED : FAILED, (result.isSuccessful()) ? "" : 
                        "Error on step \\\"" + result.getSampleLabel().replace("\"", "\\\"") + "\\\".\\nAssertion failure message: " + stepFailureMessage);
                    }

                    if (isCriticalTest() && testStatus.equals(FAILED)){
                        if (!isSingleStepTest()) {
                            stopFileMaking(testFileId, String.valueOf(System.currentTimeMillis()), testStatus, testFailureMessage);
                        } else {
                            try {
                                writeToFile(getLastTryFolder(), (ctx.getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                                " " + this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim(), "false");
                            } catch (IOException ex) {
                                log.error("Failed to write last result file.", ex);
                            }
                        }
                        log.error("Test was stopped on sampler labeled \"{}\" fail.", result.getSampleLabel());
                        ctx.getThread().stop();
                        return null;
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
            stopFileMaking(testFileId, String.valueOf(System.currentTimeMillis()), testStatus, testFailureMessage);
        } else {
            try {
                writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + 
                " " + this.getName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim()).trim(), testStatus.equals(PASSED) ? "true" : "false");
            } catch (IOException ex) {
                log.error("Failed to write last result file.", ex);
            }
        }
        return super.nextIsNull();
    }

    private void startFileMaking(String testId, String uuid, String startTime, String testName, String description, String threadName) {
        testFile = "{\"name\":\"" + testName + 
        "\",\"description\":\"" + description + 
        "\",\"stage\":\"finished\",\"start\":" + startTime +
        ",\"uuid\":\"" + uuid + 
        "\",\"historyId\":\"" + uuid +
        "\",\"fullName\":\"" + threadName + "  " + testName +
        "\",\"parameters\":[" + ParametersConstructor(getParametersField()) +
        "],\"links\":[" + linkConstructor() +
        "],\"labels\":[" + testId + getEpicField() + getStoryField() + getFeatureField() +
        getSeverity() + getOwnerField() + tagsConstructor() + extraLabelsConstructor() +
        "{\"name\":\"host\",\"value\":\"" + threadName +
        "\"}],\"steps\":[";
    }

    private void continueFileMaking(String uuid, String failureMessage, Sampler sampler, SampleResult result) {
        String stepStatus = (result.isSuccessful()) ? "passed" : "failed";
        testFile += "{\"name\":\"" + getTestNameField(result.getSampleLabel()) +
        "\",\"status\":\"" + stepStatus +
        "\",\"stage\":\"finished\",\"parameters\":[" + getStepParameters(result) +
        "],\"steps\":[" + getAssertionResults(result) +
        "],\"statusDetails\":{\"message\":\"" + failureMessage +
        "\"},\"start\":" + result.getStartTime() +
        ",\"stop\":" + result.getEndTime() +
        ",";

        if (!isWithoutContent() || (isCriticalTest() && !result.isSuccessful())) {
            try {
                if (sampler instanceof HTTPSamplerProxy) {
                    writeToFile(getPathToResults(), uuid + "-request-attachment", formatRequestData(result));
                    writeToFile(getPathToResults(), uuid + "-response-attachment", formatResponseData(result));
                } else {
                    writeToFile(getPathToResults(), uuid + "-request-attachment", result.getSamplerData().toString());
                    writeToFile(getPathToResults(), uuid + "-response-attachment", result.getResponseDataAsString());
                }
            } catch (IOException ex) {
                log.error("Failed to write request or response file.", ex);
            }

            testFile += "\"attachments\":[{\"name\":\"Request\",\"source\":\"" + uuid +
            "-request-attachment\",\"type\":\"application/json\"},{\"name\":\"Response\",\"source\":\"" + uuid +
            "-response-attachment\",\"type\":\"application/json\"}]},";
        } else {
            testFile += "\"attachments\":[]},";
        }
    }

    private void stopFileMaking(String uuid, String stopTime, String status, String failureMessage) {
        if (testFile.endsWith(",")) {
            testFile = testFile.replaceFirst(".$", "");
        }
        testFile += "],\"stop\":" + stopTime +
        ",\"status\":\"" + status +
        "\",\"statusDetails\":{\"message\":\"" + failureMessage +
        "\"}}";
        if (isDebugMode()) {
            testFile = formatJson(testFile);
        }

        try {
            writeToFile(getPathToResults(), uuid + "-result.json", testFile);
            writeToFile(getLastTryFolder(), (JMeterContextService.getContext().getThread().getThreadName().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() + " " +
            ((isSingleStepTest()) ? JMeterContextService.getContext().getPreviousResult().getSampleLabel().replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim() : getPropertyAsString(ATC_TEST_NAME).replaceAll("[\\*\\?\\\\\\/\\<\\>\\:\\|\"]", "").trim())).trim(), (status.equals(PASSED)) ? "true" : "false");
        } catch (IOException ex) {
            log.error("Failed to write result file.", ex);
        }
    }

    private void writeToFile(File folder, String filename, String data) throws IOException {
        File file = new File(folder, filename);
        FileUtils.writeStringToFile(file, data, "UTF-8");
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
    private String getAssertionResults(SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        List<String> results = new ArrayList<>();

        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName().replace("\"", "\\\"");
            if (name.startsWith("parameters:")) {
                continue;
            }
            String status = (assertionResult.isFailure() || assertionResult.isError()) ? "failed" : "passed";
            String message = (status.equals("passed")) ? "" : assertionResult.getFailureMessage().replace("\"", "\\\"");

            String resultString = String.format("{\"name\":\"%s\",\"status\":\"%s\",\"stage\":\"finished\",\"statusDetails\":{\"message\":\"%s\"}}", name, status, message);
            results.add(resultString);
        }

        return String.join(",", results);
    }

    /**
     * This method is needed to get the step parameters. The parameters are JMeter variables.
     * In this case, the values of the variables that they have after the step execution are taken.
     * Instructions for the correct output of step parameters are written in the repository.
     */
    private String getStepParameters(SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
    
        for (AssertionResult assertionResult : assertionResults) {
            String name = assertionResult.getName().replace("\"", "\\\"");
            if (name.startsWith("parameters:")) {
                String params = name.trim().replaceAll("parameters:", "");
                if (!params.matches("\\s*")) {
                    return ParametersConstructor(params);
                }
            }
        }
    
        return "";
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
        if (testNameString.matches("\\d+\\s*-.+")){
            return "{\"name\":\"allure_id\",\"value\":\"" + testNameString.split("-")[0].trim() + "\"},";
        } else {
            return "";
        }
    }

    //
    // Test name
    //
    public void setTestNameField(String te) {
        if(!isSingleStepTest()) {
            setProperty(ATC_TEST_NAME, te);
        } else {
            setProperty(ATC_TEST_NAME, "");
        }
    }

    public String getTestNameField(String testNameString) {
        return testNameString.replaceFirst("^\\d+\\s*-\\s*", "").replace("\"", "\\\"");
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
        return getPropertyAsString(ATC_DESCRIPTION, "").replace("\"", "\\\"");
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
            return "{\"name\":\"severity\",\"value\":\"normal\"},";
        } else {
            return "{\"name\":\"severity\",\"value\":\"" + getPropertyAsString(ATC_SEVERITY).replace("\"", "\\\"") + "\"},";
        }
    }

    //
    // Epic
    //
    public void setEpicField(String ep) {
        setProperty(ATC_EPIC, ep);
    }

    public String getEpicField() {
        if (getPropertyAsString(ATC_EPIC).matches("\\s*")) {
            return "";
        } else {
            return "{\"name\":\"epic\",\"value\":\"" + getPropertyAsString(ATC_EPIC).replace("\"", "\\\"") + "\"},";
        }
    }

    //
    // Story
    //
    public void setStoryField(String st) {
        setProperty(ATC_STORY, st);
    }

    public String getStoryField() {
        if (getPropertyAsString(ATC_STORY).matches("\\s*")) {
            return "";
        } else {
            return "{\"name\":\"story\",\"value\":\"" + getPropertyAsString(ATC_STORY).replace("\"", "\\\"") + "\"},";
        }
    }

    //
    // Feature
    //
    public void setFeatureField(String fe) {
        setProperty(ATC_FEATURE, fe);
    }

    public String getFeatureField() {
        if (getPropertyAsString(ATC_FEATURE).matches("\\s*")) {
            return "";
        } else {
            return "{\"name\":\"feature\",\"value\":\"" + getPropertyAsString(ATC_FEATURE).replace("\"", "\\\"") + "\"},";
        }
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

    private String tagsConstructor() {
        String tags = getTagsField();
        String[] values = tags.split(",");
        StringBuilder result = new StringBuilder();

        Pattern pattern = Pattern.compile("\\s*");

        for (String value : values) {
            if (!pattern.matcher(value).matches()) {
                result.append("{ \"name\":\"tag\",\"value\":\"").append(value.trim().replace("\"", "\\\"")).append("\"},");
            }
        }

        return result.toString();
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

    private String ParametersConstructor(String params) {
        String[] values = params.split(",");
        StringBuilder result = new StringBuilder();
    
        Pattern pattern = Pattern.compile("\\s*");
        JMeterContext context = JMeterContextService.getContext();
    
        for (String value : values) {
            value = value.trim();
            if (!pattern.matcher(value).matches()) {
                String variableValue = context.getVariables().get(value);
                if (variableValue != null) {
                    variableValue = variableValue.replaceAll("\\\\*\"", "\\\\\"");
                } else {
                    variableValue = "null";
                }
                result.append("{ \"name\":\"").append(value).append("\",\"value\":\"").append(variableValue).append("\"},");
            }
        }
    
        if (result.length() > 0) {
            result.setLength(result.length() - 1); // comma delete
        }
        return result.toString();
    }

    //
    // Owner
    //
    public void setOwnerField(String ow) {
        setProperty(ATC_OWNER, ow);
    }

    public String getOwnerField() {
        if (getPropertyAsString(ATC_OWNER).matches("\\s*")) {
            return "";
        } else {
            return "{\"name\":\"owner\",\"value\":\"" + getPropertyAsString(ATC_OWNER).replace("\"", "\\\"") + "\"},";
        }
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

    private String linkConstructor() {
        String links = getLinksField();
        String[] lines = links.split("\n");
        StringBuilder result = new StringBuilder();
    
        Pattern pattern1 = Pattern.compile("[^,]+,[^,]+");
        Pattern pattern2 = Pattern.compile("[^,]+");
    
        for (String line : lines) {
            Matcher matcher1 = pattern1.matcher(line);
            Matcher matcher2 = pattern2.matcher(line);
            if (matcher1.matches()) {
                String[] parts = line.split(",");
                result.append("{ \"name\":\"").append(parts[0].trim().replace("\"", "\\\"")).append("\",\"url\":\"").append(parts[1].trim().replace("\"", "\\\"")).append("\"},");
            } else if (matcher2.matches() && line.trim().matches("^https?://.*")) {
                String value = line.trim().replace("\"", "\\\"");
                String name = value.substring(value.lastIndexOf("/") + 1);
                result.append("{ \"name\":\"").append(name).append("\",\"url\":\"").append(value).append("\"},");
            } else if (matcher2.matches() && JMeterUtils.getPropDefault("allure.tmsLink.prefix", null) != null) {
                String value = line.trim().replace("\"", "\\\"");
                String name = value.contains("/") ? value.substring(value.lastIndexOf("/") + 1) : value;
                result.append("{ \"type\":\"tms\",\"name\":\"").append(name).append("\",\"url\":\"").append(JMeterUtils.getProperty("allure.tmsLink.prefix") + value).append("\"},");
            }
        }
    
        if (result.length() > 0) {
            result.setLength(result.length() - 1); // comma delete
        }
        return result.toString();
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

    private String extraLabelsConstructor() {
        String extraLabels = getExtraLabelsField();
        String[] lines = extraLabels.split("\n");
        StringBuilder result = new StringBuilder();

        Pattern pattern = Pattern.compile("[^,]+,[^,]+");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String[] parts = line.split(",");
                result.append("{ \"name\":\"").append(parts[0].trim().replace("\"", "\\\"")).append("\",\"value\":\"").append(parts[1].trim().replace("\"", "\\\"")).append("\"},");
            }
        }
        return result.toString();
    }
}