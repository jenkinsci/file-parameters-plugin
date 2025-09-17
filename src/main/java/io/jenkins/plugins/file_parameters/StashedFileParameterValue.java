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

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.kohsuke.stapler.DataBoundConstructor;

public final class StashedFileParameterValue extends AbstractFileParameterValue {

    private static final Logger LOGGER = Logger.getLogger(StashedFileParameterValue.class.getName());

    private static final long serialVersionUID = 1L;

    private final String tmpFile;

    @DataBoundConstructor public StashedFileParameterValue(String name, FileItem file) throws IOException {
        this(name, file.getInputStream());
        setFilename(file.getName());
        file.delete();
    }

    StashedFileParameterValue(String name, InputStream src) throws IOException {
        super(name);
        File tmp = new File(Util.createTempDir(), name);
        FileUtils.copyInputStreamToFile(src, tmp);
        tmpFile = tmp.getAbsolutePath();
    }

    @Override public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        super.buildEnvironment(build, env);
        File tmp = tmpFile != null ? new File(tmpFile) : null;
        if (tmp != null && tmp.isFile()) {
            try {
                FlowExecutionOwner feo = build instanceof FlowExecutionOwner.Executable ? ((FlowExecutionOwner.Executable) build).asFlowExecutionOwner() : null;
                TaskListener listener = feo != null ? feo.getListener() : TaskListener.NULL;
                StashManager.stash(build, name, new FilePath(tmp.getParentFile()),
                                    new Launcher.LocalLauncher(listener), env, listener, tmp.getName(), null, false,
                                    false );
            } catch (IOException | InterruptedException x) {
                throw new RuntimeException( x );
            }
            try {
                FileUtils.deleteDirectory(tmp.getParentFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override protected FilePath createTempFile(Run<?, ?> build, FilePath tempDir, EnvVars env, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        StashManager.unstash(build, name, tempDir, launcher, env, listener);
        return tempDir.child(name);
    }

    @Extension
    public static class CancelledQueueListener extends QueueListener {

        @Override
        public void onLeft(Queue.LeftItem li) {
            if (li.isCancelled()) {
                List<ParametersAction> actions = li.getActions(ParametersAction.class);
                actions.forEach(a -> {
                    a.getAllParameters().stream()
                            .filter(p -> p instanceof StashedFileParameterValue)
                            .map(p -> (StashedFileParameterValue) p)
                            .forEach(p -> {
                                if (p.tmpFile != null) {
                                    File tmp = new File(p.tmpFile);
                                    try {
                                        FileUtils.deleteDirectory(tmp.getParentFile());
                                    } catch (IOException | IllegalArgumentException e) {
                                        LOGGER.log(Level.WARNING, "Unable to delete temporary file {0} for parameter {1} of task {2}",
                                                new Object[]{tmp.getAbsolutePath(), p.getName(), li.task.getName()});
                                    }
                                }
                            });
                });
            }
        }
    }
}
