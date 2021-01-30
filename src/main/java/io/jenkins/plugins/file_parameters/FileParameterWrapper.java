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

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.io.InputStream;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Saves a file parameter to a temporary local file.
 */
public final class FileParameterWrapper extends SimpleBuildWrapper {

    public final String name;

    private boolean allowNoFile;

    @DataBoundConstructor public FileParameterWrapper(String name) {
        this.name = name;
    }

    public boolean isAllowNoFile() {
        return allowNoFile;
    }

    @DataBoundSetter
    public void setAllowNoFile(boolean allowNoFile) {
        this.allowNoFile = allowNoFile;
    }

    @Override public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        ParametersAction pa = build.getAction(ParametersAction.class);
        if (pa == null) {
            throw new AbortException("No parameters");
        }
        ParameterValue pv = pa.getParameter(name);
        if (pv == null && !allowNoFile) {
            throw new AbortException("No parameter named " + name);
        }
        if (!(pv instanceof AbstractFileParameterValue) && !allowNoFile) {
            throw new AbortException("Unsupported parameter type");
        }
        if (pv == null && allowNoFile) {
            listener.getLogger().println("Skip file parameter as there is no parameter with name: '" + name + "'");
            return;
        }
        FilePath tempDir = WorkspaceList.tempDir(workspace);
        if (tempDir == null) {
            throw new AbortException("Missing workspace or could not make temp dir");
        }
        tempDir.mkdirs();
        FilePath f = ((AbstractFileParameterValue) pv).createTempFile(build, tempDir, initialEnvironment, launcher, listener);
        context.env(name, f.getRemote());
        context.setDisposer(new Delete(f.getRemote()));
    }

    private static class Delete extends Disposer {

        private static final long serialVersionUID = 1;
        private final String file;

        Delete(String file) {
            this.file = file;
        }

        @Override public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            workspace.child(file).delete();
        }

    }

    @Symbol("withFileParameter")
    @Extension public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override public String getDisplayName() {
            return "Bind file parameter";
        }

        @Override public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

    }

}
