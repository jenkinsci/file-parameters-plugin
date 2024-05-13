/*
 * The MIT License
 *
 * Copyright 2024 CloudBees, Inc.
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

import hudson.ExtensionList;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.htmlunit.FormEncodingType;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.util.KeyDataPair;
import org.htmlunit.util.NameValuePair;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsSessionRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.TestExtension;

@Issue("JENKINS-73161")
public final class RestartTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public final JenkinsSessionRule rr = new JenkinsSessionRule();

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    /** @see AbstractFileParameterDefinitionTest#rest */
    @Test public void restBase64() throws Throwable {
        rr.then(r -> {
            r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
            r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("admin"));
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
            p.setDefinition(new CpsFlowDefinition("echo(/received $FILE_FILENAME: $FILE/)", true));
            WebRequest req = new WebRequest(new URL(r.getURL() + "job/p/buildWithParameters"), HttpMethod.POST);
            File f = tmp.newFile();
            FileUtils.write(f, "uploaded content here", "UTF-8");
            req.setEncodingType(FormEncodingType.MULTIPART);
            req.setRequestParameters(Collections.<NameValuePair>singletonList(new KeyDataPair("FILE", f, "myfile.txt", "text/plain", "UTF-8")));
            r.createWebClient().withBasicApiToken("admin").getPage(req);
        });
        rr.then(r -> {
            ExtensionList.lookupSingleton(Block.class).ready = true;
            WorkflowJob p = r.jenkins.getItemByFullName("p", WorkflowJob.class);
            r.waitUntilNoActivity();
            WorkflowRun b = p.getBuildByNumber(1);
            assertNotNull(b);
            r.assertLogContains("received myfile.txt: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
        });
    }
    @TestExtension("restBase64") public static final class Block extends QueueTaskDispatcher {
        boolean ready;
        @Override public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {
            return ready ? null : new CauseOfBlockage.BecauseNodeIsBusy(node);
        }
    }

}
