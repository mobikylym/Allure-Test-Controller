[rus](./README.md)

# Allure Test Controller

## Contents 
- [Overview](#overview)
- [Motivation](#motivation)
- [Description of controller attributes](#description-of-controller-attributes-and-their-application)
- [Assertions](#assertions)
- [Limitations](#limitations)
- [Selective retest mechanism](#selective-retest-mechanism)
- [How the selective retest mechanism affects the scenario and Allure report](#how-the-selective-retest-mechanism-affects-the-scenario-and-allure-report)

## Overview
This Apache JMeter plugin represents a controller that contains one or more test cases, the results of which will be presented in a format compatible with Allure.  
![Allure Test Controller](./img/ATC_preview.jpg)  
[back](#contents)


## Motivation
JMeter has a built-in flexible mechanism for generating test reports. However, its basic capabilities are not enough to work with the Allure Report framework and TMS, which use this tool for report visualization. This plugin is designed to solve this problem and provide the ability to customize test results to your needs in a user-friendly interface and save them in the correct format.  
[back](#contents)


## Description of controller attributes and their application:
- **Results folder path**: Specifies the path to the folder with test results. The only field that must be filled in. You can use the "Browse..." button to specify the path. ATTENTION! Do not specify the final folder here, only the path to it. In the specified directory, a folder "allure-results" will be created, into which the test results will fall.
  
- **Overwrite folder**: If set, the folder specified by the path from "Results folder path" will be cleared before the controller is executed. It is recommended to use only for the first controller inside Thread Group if you use only one thread group inside the scenario. If you use several thread groups, then you should use the checkbox only before the start of testing. For example, inside SetUp Thread Group.
  
- **Stop test on error**: If set, it stops the thread if any of the samplers inside the controller is executed with an error.
  
- **Single step tests**: If set, it changes the logic of forming results: each sampler inside the controller will be considered a separate test with its name and description, which will be taken from the name and description of the sampler. The other fields will be common for all tests in the controller (severity, epic, etc.).
  
- **Without content**: If set, attached files with request and response details will no longer be formed for test steps in the report. ATTENTION! If the "Stop test on error" checkbox is set, attached files will still be formed for the step with an error so that you can see the cause of the error.
  
- **Without non-HTTP steps**: If set, all NON-HTTP samplers (for example, JSR223 Sampler or Debug Sampler) will stop falling into the test case steps. ATTENTION! If the "Stop test on error" checkbox is set, even if the first sampler with an error (on which the thread will stop) becomes a NON-HTTP sampler, a step will still be created for it to show the cause of the test stop in the report.
  
- **Debug mode**: If set, nothing will change in the report, but the .json file with test results will acquire a readable look. The function is needed only for manual checking of results.
  
- **Test name + Description**: The name of the test case and its description. If the "Single step tests" checkbox is set, these fields become unavailable for filling, because then each sampler inside the controller will have its name and description. ATTENTION! The “Test name” field can also contain the id of the related case when using Allure TMS if necessary. To indicate the connection, you need to specify the numeric id of the case from TMS before the test name, then a dash, and then the case name (for example, “1425 - Authorization with a personal account”). This functionality also works when using the “Single step tests” checkbox.
  
- **Severity**: Indicates the severity of the test case. By default in Allure, the options are: "trivial", "minor", "normal", "critical" and "blocker". Free input is available. If you leave the field empty, "normal" will be indicated.
  
- **Epic + Feature + Story**: Names of the corresponding entities. They serve to divide test cases in the report by functionality.
  
- **Tags (comma-delimited)**: Specifies tags for the test case.
  
- **Params (comma-delimited)**: Specifies the variables that need to be reflected in the test case. It is filled with variable names, without "${}". ATTENTION! If the "Single step tests" checkbox is set, the variables will take the values they have after the current sampler is executed. If the "Single step tests" checkbox is not set, the variables will take the values they have before the controller starts working.

- **Step params**: This field is not present in the controller UI because each individual step can have its own unique parameters. How to correctly add parameters to a specific step? Add a child element of type “Response Assertion” to the sampler you need. Leave all fields in it empty, except for “Name”. This field should start with "parameters: " (necessarily in lowercase), and after the colon, list the names of all variables, the values of which you want to display for this step in the report, without “${}”. ATTENTION! All parameters must be written in the same Response Assertion. If several elements in the same step start with "parameters: ", only the parameters from the element that is higher in the tree of elements will get into the report, the rest will be ignored. It is important to remember that the test parameters are determined by the value of the variables immediately before entering the controller, and the step parameters - immediately after the completion of this step. That is, if any extractor is used in your step that creates a new variable, you can immediately display it in the parameters of this step.

- **Owner**: Specifies the creator or person responsible for this test case.
  
- **Links**: Indicating links to the test case. Each link should start on a new line and follow the format, otherwise it will be ignored. There are 2 ways to specify a link:
    1. Specify in the format “Name -> comma -> URL”. Suitable for all cases;
    2. Specify in the format “URL”. In this case, the part of the link after the last slash becomes the name of the link. Suitable only for cases when the endpoint should be the name. For example, when specifying in the line “https://example.com/tms/TMS-112”, the link name will automatically become “TMS-112”.

- **TMS Links**: This functionality has been added exclusively for convenience when using an external TMS in addition to Allure. To add this link, you need to specify the JMeter property “allure.tmsLink.prefix” in the .properties file or define it in the scenario using a sampler that supports code input (for example, BeanShell Sampler). After that, in the “Links” field, you can simply specify the name of the entity so that the plugin automatically adds a link to it from the property as a prefix. For example, with “allure.tmsLink.prefix” = “https://example.com/tms/” and specifying in the line “TMS-112”, a link with the name “TMS-112” and URL = “https://example.com/tms/TMS-112” will be added to the test case. ATTENTION! For more flexible settings, you can specify not the full path in the property, but only its immutable part, and write the rest in the line. For example, with “allure.tmsLink.prefix” = “https://example.com/” and specifying in the line “some/path/to/use/TMS-112”, a link with the name “TMS-112” and URL = “https://example.com/some/path/to/use/TMS-112” will be added to the test case.

- **Extra labels (Allure TMS only)**: Indicating additional attributes of the test case in the format “Key -> comma -> value”. Each label should start on a new line and follow the format, otherwise it will be ignored. ATTENTION! When generating a report using the Allure console utility, support for additional fields is not implemented. The values specified here must be supported and configured in your Allure TMS to be visible in the report.

[back](#contents)


## Assertions
For each step, the plugin analyzes all nested assertions and outputs them in the report. In case at least one of the samplers in the test case ends with an error, the failure message from the first assertion that did not pass will be displayed in the report at the top of the test case card. If the first sampler with an error does not have any assertions that did not pass, the failure message will remain empty, and the cause of the error can be found in the files attached to the test case step with request/response details.  
[back](#contents)


## Limitations
1. For security purposes, when writing files with step data, the plugin automatically substitutes the “Authorization”, “X-Api-Token”, and “X-Api-Key” headers in the request with the value “XXX (Has been replaced for safety)”. This substitution is not configurable and always happens.
2. If you use any loop controller inside Allure Test Controller (for example, While Controller or ForEach Controller), each iteration for samplers inside the loop will create new test case steps in the report. Exception: if you use [Retry Post-Processor](https://github.com/tilln/jmeter-retrier), the step will be created only for the last attempt to execute the sampler.
3. Try not to use Allure Test Controller inside the same controller. If the "Stop test on error" checkbox is set in the child controller and not in the parent, then in case of an error inside the child controller, the results of the parent will not appear in the report because the thread will be stopped before the results of the parent controller are written to the file.

[back](#contents)


# Selective retest mechanism

In addition to saving the report for Allure, the plugin also saves the results of each individual test in the “last-try-results” folder, which is automatically created in the same location as the “allure-results” folder. You can configure the scenario in such a way that it skips all tests that were successful last time when launched. 
The configuration is done through the JMeter property “allure.retry.fallen”. If “allure.retry.fallen” = “true”, the selective launch mechanism will work and only tests that ended with errors last time will be run again. 
This property can be set in several ways:
- **Write in the .properties file**: It is recommended to do this only if you always want to enable this option (“true”) by default.
  
- **Specify in the scenario using a sampler that supports code input**: For instance, setting the property in BeanShell Sampler would look like this:
```Java
import org.apache.jmeter.util.JMeterUtils;
JMeterUtils.setProperty("allure.retry.fallen", "true");
```

- **Specify when launching through the terminal using the -J parameter**:
```
jmeter -Jallure.retry.fallen=true -n -t [your_jmx_file]
```  
[back](#contents)


## How the selective retest mechanism affects the scenario and Allure report:
1. If “Single step tests” is not set, then if at least one step of the test ended with an error last time, the entire test will be performed next time and will be included in the report. If the test was successful last time, then this time the samplers in it will not be executed, and this test will not be included in the report.
2. When “Single step tests” is set: if at least one of the samplers inside the controller ended with an error last time, all samplers inside it will be executed in the scenario. The report will only include tests that ended with an error last time, and tests that ended with an error this time. For example, inside the controller there are 5 samplers. If only the second one ended with an error last time, and after the current launch the fourth and fifth gave an error, then 3 tests will be included in the report: 2, 4 and 5. If all samplers inside the controller were successful last time - execution will be skipped and these tests will not be included in the report. ATTENTION! For recording the results of the previous launch of a one-step test, a unique combination of “Thread Name” + “Sampler Name” is used. Do not use the same sampler names inside the controller if “Single step tests” is set, otherwise the results of some tests will erase the results of others.

[back](#contents)