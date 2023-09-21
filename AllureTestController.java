package org.apache.jmeter.control; 

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.jmeter.samplers.SampleEvent; 
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.schema.PropertiesAccessor;
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
public class AllureTestController extends GenericController implements SampleListener, Controller, Serializable {
    /**
     * Used to identify Transaction Controller Parent Sampler
     */
    static final String NUMBER_OF_SAMPLES_IN_TRANSACTION_PREFIX = "Number of samples in transaction : ";

    private static final String TRUE = Boolean.toString(true); // i.e. "true"

    private static final Logger log = LoggerFactory.getLogger(AllureTestController.class);

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
        lnf = new ListenerNotifier();
    }

    @Override
    public PropertiesAccessor<? extends AllureTestController> getProps() {
        return new PropertiesAccessor<>(this, getSchema());
    }

    @Override
    protected Object readResolve(){
        super.readResolve();
        lnf = new ListenerNotifier();
        return this;
    }

    //
    // Path to results
    //
    public void setPathToResults(String res) {
        set(getSchema().getPathToRes(), res);
    }

    public String getPathToResults() {
        return get(getSchema().getPathToRes());
    }

    @Override   // Проверка валидности пути к папке + создание папки, если это возможно
    public void testStarted() {
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
        set(getSchema().getFolderOverwrite(), ov);
    }
    
    public boolean isFolderOverwrite() {
        return get(getSchema().getFolderOverwrite());
    }

    //
    // Stop test on error
    //
    public void setIsCritical(boolean ic) {
        set(getSchema().getIsCritical(), ic);
    }
    
    public boolean isCriticalTest() {
        return get(getSchema().getIsCritical());
    }

    //
    // Single step tests
    //
    public void setIsSingleStep(boolean ss) {
        set(getSchema().getIsSingleStep(), ss);
    }
    
    public boolean isSingleStepTest() {
        return get(getSchema().getIsSingleStep());
    }

















    /**
     * @see org.apache.jmeter.control.Controller#next()
     */
    @Override
    public Sampler next(){
        return nextWithoutTransactionSampler();
    }

///////////////// Transaction Controller - parent ////////////////

    @Override // ------------------------------------------------------------------------------ Надобность под вопросом
    protected Sampler nextIsAController(Controller controller) throws NextIsNullException {
        if (!isGenerateParentSample()) {
            return super.nextIsAController(controller);
        }
        Sampler returnValue;
        Sampler sampler = controller.next();
        if (sampler == null) {
            currentReturnedNull(controller);
            // We need to call the super.next, instead of this.next, which is done in GenericController,
            // because if we call this.next(), it will return the TransactionSampler, and we do not want that.
            // We need to get the next real sampler or controller
            returnValue = super.next();
        } else {
            returnValue = sampler;
        }
        return returnValue;
    }

////////////////////// Transaction Controller - additional sample //////////////////////////////

    private Sampler nextWithoutTransactionSampler() {
        if (isFirst()) // must be the start of the subtree
        {
            calls = 0;
            noFailingSamples = 0;
            res = new SampleResult();
            res.setSampleLabel(getName());
            // Assume success
            res.setSuccessful(true);
            res.sampleStart();
            prevEndTime = res.getStartTime();//???
            pauseTime = 0;
        }
        boolean isLast = current==super.subControllersAndSamplers.size();
        Sampler returnValue = super.next();
        if (returnValue == null && isLast) // Must be the end of the controller
        {
            if (res != null) {
                // See BUG 55816
                if (!isIncludeTimers()) {
                    long processingTimeOfLastChild = res.currentTimeInMillis() - prevEndTime;
                    pauseTime += processingTimeOfLastChild;
                }
                res.setIdleTime(pauseTime+res.getIdleTime());
                res.sampleEnd();
                res.setResponseMessage(
                        TransactionController.NUMBER_OF_SAMPLES_IN_TRANSACTION_PREFIX
                                + calls + ", number of failing samples : "
                                + noFailingSamples);
                if(res.isSuccessful()) {
                    res.setResponseCodeOK();
                }
                notifyListeners();
            }
        }
        else {
            // We have sampled one of our children
            calls++;
        }

        return returnValue;
    }

    /**
     * @param res {@link SampleResult}
     * @return true if res is the ParentSampler transactions
     */
    public static boolean isFromTransactionController(SampleResult res) {
        return res.getResponseMessage() != null &&
                res.getResponseMessage().startsWith(
                        TransactionController.NUMBER_OF_SAMPLES_IN_TRANSACTION_PREFIX);
    }

    /**
     * @see org.apache.jmeter.control.GenericController#triggerEndOfLoop()
     */
    @Override
    public void triggerEndOfLoop() {
        if(!isGenerateParentSample()) {
            if (res != null) {
                res.setIdleTime(pauseTime + res.getIdleTime());
                res.sampleEnd();
                res.setSuccessful(TRUE.equals(JMeterContextService.getContext().getVariables().get(JMeterThread.LAST_SAMPLE_OK)));
                res.setResponseMessage(
                        TransactionController.NUMBER_OF_SAMPLES_IN_TRANSACTION_PREFIX
                                + calls + ", number of failing samples : "
                                + noFailingSamples);
                notifyListeners();
            }
        } else {
            Sampler subSampler = transactionSampler.getSubSampler();
            // See Bug 56811
            // triggerEndOfLoop is called when error occurs to end Main Loop
            // in this case normal workflow doesn't happen, so we need
            // to notify the children of TransactionController and
            // update them with SubSamplerResult
            if(subSampler instanceof TransactionSampler) {
                TransactionSampler tc = (TransactionSampler) subSampler;
                transactionSampler.addSubSamplerResult(tc.getTransactionResult());
            }
            transactionSampler.setTransactionDone();
            // This transaction is done
            transactionSampler = null;
        }
        super.triggerEndOfLoop();
    }

    /**
     * Create additional SampleEvent in NON Parent Mode
     */
    protected void notifyListeners() {
        // TODO could these be done earlier (or just once?)
        JMeterContext threadContext = getThreadContext();
        JMeterVariables threadVars = threadContext.getVariables();
        SamplePackage pack = (SamplePackage) threadVars.getObject(JMeterThread.PACKAGE_OBJECT);
        if (pack == null) {
            // If child of TransactionController is a ThroughputController and TPC does
            // not sample its children, then we will have this
            // TODO Should this be at warn level ?
            log.warn("Could not fetch SamplePackage");
        } else {
            SampleEvent event = new SampleEvent(res, threadContext.getThreadGroup().getName(),threadVars, true);
            // We must set res to null now, before sending the event for the transaction,
            // so that we can ignore that event in our sampleOccurred method
            res = null;
            lnf.notifyListeners(event, pack.getSampleListeners());
        }
    }

    @Override
    public void sampleOccurred(SampleEvent se) {
        if (!isGenerateParentSample()) {
            // Check if we are still sampling our children
            if(res != null && !se.isTransactionSampleEvent()) {
                SampleResult sampleResult = se.getResult();
                res.setThreadName(sampleResult.getThreadName());
                res.setBytes(res.getBytesAsLong() + sampleResult.getBytesAsLong());
                res.setSentBytes(res.getSentBytes() + sampleResult.getSentBytes());
                if (!isIncludeTimers()) {// Accumulate waiting time for later
                    pauseTime += sampleResult.getEndTime() - sampleResult.getTime() - prevEndTime;
                    prevEndTime = sampleResult.getEndTime();
                }
                if(!sampleResult.isSuccessful()) {
                    res.setSuccessful(false);
                    noFailingSamples++;
                }
                res.setAllThreads(sampleResult.getAllThreads());
                res.setGroupThreads(sampleResult.getGroupThreads());
                res.setLatency(res.getLatency() + sampleResult.getLatency());
                res.setConnectTime(res.getConnectTime() + sampleResult.getConnectTime());
            }
        }
    }

    @Override
    public void sampleStarted(SampleEvent e) {
    }

    @Override
    public void sampleStopped(SampleEvent e) {
    }

    /**
     * Whether to include timers and pre/post processor time in overall sample.
     * @param includeTimers Flag whether timers and pre/post processor should be included in overall sample
     */
    public void setIncludeTimers(boolean includeTimers) {
        set(getSchema().getIncludeTimers(), includeTimers);
    }

    /**
     * Whether to include timer and pre/post processor time in overall sample.
     *
     * @return boolean (defaults to true for backwards compatibility)
     */
    public boolean isIncludeTimers() {
        return get(getSchema().getIncludeTimers());
    }
}
