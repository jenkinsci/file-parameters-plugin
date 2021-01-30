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

package io.jenkins.plugins.file_parameters;

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.KeyDataPair;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import hudson.cli.CLICommandInvoker;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.User;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class AbstractFileParameterDefinitionTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule r = new JenkinsRule();

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    // TODO test of GUI upload

    // adapted from BuildCommandTest.fileParameter
    @Test public void cli() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received: $FILE/)", true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
    }

    @Test public void rest() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("admin"));
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received: $FILE/)", true));
        // Like: curl -u $auth -F FILE=@/tmp/f $jenkins/job/myjob/buildWithParameters
        WebRequest req = new WebRequest(new URL(r.getURL() + "job/myjob/buildWithParameters"), HttpMethod.POST);
        File f = tmp.newFile();
        FileUtils.write(f, "uploaded content here", "UTF-8");
        req.setEncodingType(FormEncodingType.MULTIPART);
        req.setRequestParameters(Collections.<NameValuePair>singletonList(new KeyDataPair("FILE", f, "FILE", "text/plain", "UTF-8")));
        User.getById("admin", true); // TODO workaround for https://github.com/jenkinsci/jenkins-test-harness/pull/273
        r.createWebClient().withBasicApiToken("admin").getPage(req);
        r.waitUntilNoActivity();
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
    }

}
