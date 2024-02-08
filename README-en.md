[rus](./README.md)

# Allure Test Controller


## Overview
This Apache JMeter plugin represents a controller that contains one or more test cases, the results of which will be presented in a format compatible with Allure.


## Motivation
JMeter has a built-in flexible mechanism for generating test reports. However, its basic capabilities are not enough to work with the Allure Report framework and TMS, which use this tool for report visualization. This plugin is designed to solve this problem and provide the ability to customize test results to your needs in a user-friendly interface and save them in the correct format.


## Description of controller attributes and their application:
- **Results folder path**: Specifies the path to the folder with test results. The only field that must be filled in. You can use the "Browse..." button to specify the path. ATTENTION! Do not specify the final folder here, only the path to it. In the specified directory, a folder "allure-results" will be created, into which the test results will fall.
  
- **Overwrite folder**: If set, the folder specified by the path from "Results folder path" will be cleared before the controller is executed. It is recommended to use only for the first controller inside Thread Group if you use only one thread group inside the scenario. If you use several thread groups, then you should use the checkbox only before the start of testing. For example, inside SetUp Thread Group.
  
- **Stop test on error**: If set, it stops the thread if any of the samplers inside the controller is executed with an error.
  
- **Single step tests**: If set, it changes the logic of forming results: each sampler inside the controller will be considered a separate test with its name and description, which will be taken from the name and description of the sampler. The other fields will be common for all tests in the controller (severity, epic, etc.).
  
- **Without content**: If set, attached files with request and response details will no longer be formed for test steps in the report. ATTENTION! If the "Stop test on error" checkbox is set, attached files will still be formed for the step with an error so that you can see the cause of the error.
  
- **Without non-HTTP steps**: If set, all NON-HTTP samplers (for example, JSR223 Sampler or Debug Sampler) will stop falling into the test case steps. ATTENTION! If the "Stop test on error" checkbox is set, even if the first sampler with an error (on which the thread will stop) becomes a NON-HTTP sampler, a step will still be created for it to show the cause of the test stop in the report.
  
- **Debug mode**: If set, nothing will change in the report, but the .json file with test results will acquire a readable look. The function is needed only for manual checking of results.
  
- **Test name + Description**: The name of the test case and its description. If the "Single step tests" checkbox is set, these fields become unavailable for filling, because then each sampler inside the controller will have its name and description.
  
- **Severity**: Indicates the severity of the test case. By default in Allure, the options are: "trivial", "minor", "normal", "critical" and "blocker". Free input is available. If you leave the field empty, "normal" will be indicated.
  
- **Epic + Story + Feature**: Names of the corresponding entities. They serve to divide test cases in the report by functionality.
  
- **Tags (comma-delimited)**: Specifies tags for the test case.
- **Params (comma-delimited)**: Specifies the variables that need to be reflected in the test case. It is filled with variable names, without "${}". ATTENTION! If the "Single step tests" checkbox is set, the variables will take the values they have after the current sampler is executed. If the "Single step tests" checkbox is not set, the variables will take the values they have before the controller starts working.

- **Owner**: Specifies the creator or person responsible for this test case.
  
- **Links (format: name-comma-URL)**: Specifies links to the test case. Each link should start on a new line. If the value in the line does not match the format from the field name, the line will be skipped and will not appear in the report.
  
- **Extra labels (Allure TMS only)**: Specifies additional attributes of the test case. The filling format is the same as for "Links". ATTENTION! When generating a report using the Allure console utility, support for additional fields is not implemented. The values specified here must be supported by your Allure TMS to be visible in the report.

  
## Assertions
For each step, the plugin analyzes all nested assertions and outputs them in the report. In case at least one of the samplers in the test case ends with an error, the failure message from the first assertion that did not pass will be displayed in the report at the top of the test case card. If the first sampler with an error does not have any assertions that did not pass, the failure message will remain empty, and the cause of the error can be found in the files attached to the test case step with request/response details.


## Limitations
1. At the moment, the plugin does not support variables inside the test case step. Only case variables are available. Support for test case step parameters will appear in future versions.
2. If you use any loop controller inside Allure Test Controller (for example, While Controller or ForEach Controller), each iteration for samplers inside the loop will create new test case steps in the report. Exception: if you use [Retry Post-Processor](https://github.com/tilln/jmeter-retrier), the step will be created only for the last attempt to execute the sampler.
3. Try not to use Allure Test Controller inside the same controller. If the "Stop test on error" checkbox is set in the child controller and not in the parent, then in case of an error inside the child controller, the results of the parent will not appear in the report because the thread will be stopped before the results of the parent controller are written to the file.

