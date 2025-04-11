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
import hudson.model.Failure;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class FileParameterWrapperTest {

    @Test
    void base64(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        String pipeline = """
                node('remote') {
                  withFileParameter('FILE') {
                    echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)
                  }
                }""";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE' from ", b);
    }

    @Test
    void base64UndefinedFail(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        String pipeline = """
                pipeline {
                  agent any
                  parameters {
                    base64File 'FILE'
                  }
                  stages {
                    stage('Example') {
                      steps {
                        withFileParameter('FILE') {
                          echo('foo')
                        }
                      }
                    }
                  }
                }""";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        r.assertBuildStatus(Result.FAILURE, run);
        r.assertLogContains("No parameter named FILE", run);
    }

    @Test
    void base64WithAllowNoFile(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        String pipeline = """
                pipeline {
                  agent any
                  parameters {
                    base64File 'FILE'
                  }
                  stages {
                    stage('Example') {
                      steps {
                        withFileParameter(name:'FILE', allowNoFile: true) {
                          echo('foo')
                        }
                      }
                    }
                  }
                }""";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        r.assertBuildStatus(Result.SUCCESS, run);
        r.assertLogContains("foo", run);
        r.assertLogContains("Skip file parameter as there is no parameter with name: 'FILE'", run);
    }

    @Test
    void base64DeclarativeParameterCreated(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");

        String pipeline = """
                pipeline {
                  agent any
                  parameters {
                    base64File 'FILE'
                  }
                  stages {
                    stage('Example') {
                      steps {
                        withFileParameter('FILE') {
                          echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)
                        }
                      }
                    }
                  }
                }""";

        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        // definitely will fail but we just ensure parameter has been created
        r.assertBuildStatus(Result.FAILURE, run);
        ParametersDefinitionProperty pdp = p.getProperty(ParametersDefinitionProperty.class);
        assertNotNull(pdp, "parameters definition property is null");
        ParameterDefinition pd = pdp.getParameterDefinition( "FILE");
        assertNotNull(pd, "parameters definition is null");
        assertEquals(Base64FileParameterDefinition.class, pd.getClass(), "parameter not type Base64FileParameterDefinition");

        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(2);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Test
    void stashed(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");
        p.addProperty(new ParametersDefinitionProperty(new StashedFileParameterDefinition("FILE")));
        String pipeline = """
                node('remote') {
                  withFileParameter('FILE') {
                    echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}' from $FILE/)
                  }
                }""";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Test
    void stashedDeclarativeParameterCreated(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "myjob");

        String pipeline = """
                pipeline {
                    agent any
                    parameters {
                        stashedFile 'FILE'
                    }
                    stages {
                        stage('Example') {
                            steps {
                                withFileParameter('FILE') {
                                    echo(/loaded '${readFile(FILE).toUpperCase(Locale.ROOT)}'/)
                                }
                            }
                        }
                    }
                }""";

        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        r.waitForCompletion(run);
        // definitely will fail but we just ensure parameter has been created
        r.assertBuildStatus(Result.FAILURE, run);
        ParametersDefinitionProperty pdp = p.getProperty(ParametersDefinitionProperty.class);
        assertNotNull(pdp, "parameters definition property is null");
        ParameterDefinition pd = pdp.getParameterDefinition("FILE");
        assertNotNull(pd, "parameters definition is null");
        assertEquals(StashedFileParameterDefinition.class, pd.getClass(), "parameter not type Base64FileParameterDefinition");

        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                       invokeWithArgs("-f", "-p", "FILE=", "myjob"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(2);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE'", b);
    }

    @Issue("SECURITY-3123")
    @Test
    void stashMaliciousFilename(JenkinsRule r) throws Exception {
        String hack = "../../../../../../../../../../../../../../../../../../../../../tmp/file-parameters-plugin-SECURITY-3123";
        File result = new File("/tmp/file-parameters-plugin-SECURITY-3123");
        FileUtils.deleteQuietly(result);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        try {
            p.addProperty(new ParametersDefinitionProperty(new StashedFileParameterDefinition(hack)));
        } catch (Failure x) {
            return; // good
        }
        p.setDefinition(new CpsFlowDefinition("", true));
        assertThat(new CLICommandInvoker(r, "build").
                       withStdin(new ByteArrayInputStream("malicious content here".getBytes())).
                       invokeWithArgs("-f", "-p", hack + "=", "p"),
                   CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        assertFalse(result.isFile());
    }

    @Test
    void shortParameterName(JenkinsRule r) throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("F")));
        String pipeline = """
                node('remote') {
                  withFileParameter('F') {
                    echo(/loaded '${readFile(F).toUpperCase(Locale.ROOT)}' from $F/)
                  }
                }""";
        p.setDefinition(new CpsFlowDefinition(pipeline, true));
        assertThat(new CLICommandInvoker(r, "build").
                withStdin(new ByteArrayInputStream("uploaded content here".getBytes())).
                invokeWithArgs("-f", "-p", "F=", "p"),
                CLICommandInvoker.Matcher.succeeded());
        WorkflowRun b = p.getBuildByNumber(1);
        assertNotNull(b);
        r.assertLogContains("loaded 'UPLOADED CONTENT HERE' from ", b);
    }

}
