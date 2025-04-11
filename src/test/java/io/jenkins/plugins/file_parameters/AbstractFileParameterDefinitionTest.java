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

import hudson.cli.CLICommandInvoker;
import hudson.model.ParametersDefinitionProperty;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.htmlunit.FormEncodingType;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlFileInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.http.HttpStatus;
import org.htmlunit.util.KeyDataPair;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class AbstractFileParameterDefinitionTest {

    @TempDir
    private File tmp;

    @Test
    void gui(JenkinsRule r) throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("admin"));
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received $FILE_FILENAME: $FILE/)", true));
        File f = Files.createFile(new File(tmp, "myfile.txt").toPath()).toFile();
        FileUtils.write(f, "uploaded content here", StandardCharsets.UTF_8);
        JenkinsRule.WebClient wc = r.createWebClient().login("admin");
        wc.setThrowExceptionOnFailingStatusCode(false);
        wc.setRedirectEnabled(true);
        HtmlPage parametersPage = wc.goTo("job/myjob/build?delay=0sec");
        assertThat(parametersPage.getWebResponse().getStatusCode(), is(HttpStatus.METHOD_NOT_ALLOWED_405));
        HtmlForm form = parametersPage.getFormByName("parameters");
        HtmlFileInput file = form.getInputByName("file");
        file.setValue(f.getAbsolutePath());
        assertThat(r.submit(form).getWebResponse().getStatusCode(), is(HttpStatus.OK_200)); // 303 myjob/build → myjob/
        r.waitUntilNoActivity();
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received myfile.txt: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
    }

    // adapted from BuildCommandTest.fileParameter
    @Test
    void cli(JenkinsRule r) throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received $env.FILE_FILENAME: $FILE/)", true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received null: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
    }

    @Test
    void rest(JenkinsRule r) throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("admin"));
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received $FILE_FILENAME: $FILE/)", true));
        // Like: curl -u $auth -F FILE=@/tmp/f $jenkins/job/myjob/buildWithParameters
        WebRequest req = new WebRequest(new URL(r.getURL() + "job/myjob/buildWithParameters"), HttpMethod.POST);
        File f = File.createTempFile("junit", null, tmp);
        FileUtils.write(f, "uploaded content here", StandardCharsets.UTF_8);
        req.setEncodingType(FormEncodingType.MULTIPART);
        req.setRequestParameters(Collections.singletonList(new KeyDataPair("FILE", f, "myfile.txt", "text/plain", StandardCharsets.UTF_8)));
        r.createWebClient().withBasicApiToken("admin").getPage(req);
        r.waitUntilNoActivity();
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received myfile.txt: dXBsb2FkZWQgY29udGVudCBoZXJl", b);
    }

    @Issue("https://github.com/jenkinsci/file-parameters-plugin/issues/26")
    @Test
    void restMissingValue(JenkinsRule r) throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("admin"));
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("echo(/received $env.FILE_FILENAME: $env.FILE/)", true));
        // Like: curl -u $auth $jenkins/job/myjob/buildWithParameters
        WebRequest req = new WebRequest(new URL(r.getURL() + "job/myjob/buildWithParameters"), HttpMethod.POST);
        r.createWebClient().withBasicApiToken("admin").getPage(req);
        r.waitUntilNoActivity();
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("received null: null", b);
    }

    @Test
    void buildStep(JenkinsRule r) throws Exception {
        WorkflowJob us = r.createProject(WorkflowJob.class, "us");
        us.setDefinition(new CpsFlowDefinition("build job: 'ds', parameters: [base64File(name: 'FILE', base64: Base64.encoder.encodeToString('a message'.bytes))]", true));
        WorkflowJob ds = r.createProject(WorkflowJob.class, "ds");
        ds.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        ds.setDefinition(new CpsFlowDefinition("echo(/got ${new String(Base64.decoder.decode(FILE))}/)", true));
        r.buildAndAssertSuccess(us);
        WorkflowRun b = ds.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("got a message", b);
    }

}
