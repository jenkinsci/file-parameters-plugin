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
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import java.io.ByteArrayInputStream;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.model.Result;
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
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("node('remote') {withFileParameter('FILE') {echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)}}", true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE' from ", b);
    }

    @Test public void base64UndefinedFail() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        String pipeline = "pipeline {\n" +
            "  agent any\n" +
            "  parameters {\n" +
            "    base64File 'FILE'\n" +
            "  }\n" +
            "  stages {\n" +
            "    stage('Example') {\n" +
            "      steps {\n" +
            "        withFileParameter('FILE') {\n" +
            "          echo('foo') \n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        r.assertBuildStatus(Result.FAILURE, run);
        r.assertLogContains("No parameter named FILE", run);
    }

    @Test public void base64WithAllowNoFile() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        String pipeline = "pipeline {\n" +
            "  agent any\n" +
            "  parameters {\n" +
            "    base64File 'FILE'\n" +
            "  }\n" +
            "  stages {\n" +
            "    stage('Example') {\n" +
            "      steps {\n" +
            "        withFileParameter(name:'FILE', allowNoFile: true) {\n" +
            "          echo('foo') \n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        r.assertBuildStatus(Result.SUCCESS, run);
        r.assertLogContains("foo", run);
        r.assertLogContains("Skip file parameter as there is no parameter with name: 'FILE'", run);
    }

    @Test public void base64DeclarativeParameterCreated() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");

        String pipeline = "pipeline {\n" +
            "  agent any\n" +
            "  parameters {\n" +
            "    base64File 'FILE'\n" +
            "  }\n" +
            "  stages {\n" +
            "    stage('Example') {\n" +
            "      steps {\n" +
            "        withFileParameter('FILE') {\n" +
            "          echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/) \n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        // definitely will fail but we just ensure parameter has been created
        r.assertBuildStatus(Result.FAILURE, run);
        ParametersDefinitionProperty pdp = p.getProperty(ParametersDefinitionProperty.class);
        assertNotNull("parameters definition property is null", pdp);
        ParameterDefinition pd = pdp.getParameterDefinition( "FILE");
        assertNotNull("parameters definition is null", pd);
        assertEquals("parameter not type Base64FileParameterDefinition", Base64FileParameterDefinition.class, pd.getClass());

        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(2);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Test public void stashed() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new StashedFileParameterDefinition("FILE")));
        p.setDefinition(new CpsFlowDefinition("node('remote') {withFileParameter('FILE') {echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)}}", true));
        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Test public void stashedDeclarativeParameterCreated() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");

        String pipeline = "pipeline {\n" +
            "    agent any\n" +
            "    parameters {\n" +
            "        stashedFile 'FILE'\n" +
            "    }\n" +
            "    stages {\n" +
            "        stage('Example') {\n" +
            "            steps {\n" +
            "                withFileParameter('FILE') {\n" +
            "                    echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}'/)\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        // definitely will fail but we just ensure parameter has been created
        r.assertBuildStatus(Result.FAILURE, run);
        ParametersDefinitionProperty pdp = p.getProperty(ParametersDefinitionProperty.class);
        assertNotNull("parameters definition property is null", pdp);
        ParameterDefinition pd = pdp.getParameterDefinition("FILE");
        assertNotNull("parameters definition is null", pd);
        assertEquals("parameter not type Base64FileParameterDefinition", StashedFileParameterDefinition.class, pd.getClass());

        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(2);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Test public void shortParameterName() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("F")));
        p.setDefinition(new CpsFlowDefinition("node('remote') {withFileParameter('F') {echo(/loaded '${readFile(F).toUpperCase(Locale.ROOT)}' from $F/)}}", true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "F=", "p"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE' from ", b);
    }

}
