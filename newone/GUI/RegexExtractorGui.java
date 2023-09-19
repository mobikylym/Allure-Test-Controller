/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.extractor.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.processor.gui.AbstractPostProcessorGui;
import org.apache.jmeter.testelement.AbstractScopedTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledTextField;

/**
 * Regular Expression Extractor Post-Processor GUI
 */
@GUIMenuSortOrder(4)
@TestElementMetadata(labelResource = "regex_extractor_title")
public class RegexExtractorGui extends AbstractPostProcessorGui {
   
    @Override
    public void configure(TestElement el) {
        super.configure(el);
        if (el instanceof RegexExtractor){
            RegexExtractor re = (RegexExtractor) el;
            showScopeSettings(re, true);
            useHeaders.setSelected(re.useHeaders());
            useRequestHeaders.setSelected(re.useRequestHeaders());
            useBody.setSelected(re.useBody());
            useUnescapedBody.setSelected(re.useUnescapedBody());
            useBodyAsDocument.setSelected(re.useBodyAsDocument());
            useURL.setSelected(re.useUrl());
            useCode.setSelected(re.useCode());
            useMessage.setSelected(re.useMessage());
            regexField.setText(re.getRegex());
            templateField.setText(re.getTemplate());
            defaultField.setText(re.getDefaultValue());
            emptyDefaultValue.setSelected(re.isEmptyDefaultValue());
            matchNumberField.setText(re.getMatchNumberAsString());
            refNameField.setText(re.getRefName());
        }
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement extractor) {
        super.configureTestElement(extractor);
        if (extractor instanceof RegexExtractor) {
            RegexExtractor regex = (RegexExtractor) extractor;
            saveScopeSettings(regex);
            regex.setUseField(group.getSelection().getActionCommand());
            regex.setRefName(refNameField.getText());
            regex.setRegex(regexField.getText());
            regex.setTemplate(templateField.getText());
            regex.setDefaultValue(defaultField.getText());
            regex.setDefaultEmptyValue(emptyDefaultValue.isSelected());
            regex.setMatchNumber(matchNumberField.getText());
        }
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout());
        setBorder(makeBorder());

        Box box = Box.createVerticalBox();
        box.add(makeTitlePanel());
        box.add(createScopePanel(true));
        box.add(makeSourcePanel());
        add(box, BorderLayout.NORTH);
        add(makeParameterPanel(), BorderLayout.CENTER);
    }

    private static void addField(JPanel panel, JLabeledTextField field, GridBagConstraints gbc) {
        List<JComponent> item = field.getComponentList();
        panel.add(item.get(0), gbc.clone());
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        panel.add(item.get(1), gbc.clone());
    }

    // Next line
    private static void resetContraints(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill=GridBagConstraints.NONE;
    }

    private static void initConstraints(GridBagConstraints gbc) {
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
    }
}
