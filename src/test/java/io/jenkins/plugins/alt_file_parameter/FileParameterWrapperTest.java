/*
 * The MIT License
 *
 * Copyright 2020 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.alt_file_parameter;

import hudson.cli.CLICommandInvoker;
import hudson.model.ParametersDefinitionProperty;
import java.io.ByteArrayInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class FileParameterWrapperTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void base64() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE", null)));
        p.setDefinition(new CpsFlowDefinition("node('remote') {withFileParameter('FILE') {echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)}}", true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE' from ", b);
    }

}
