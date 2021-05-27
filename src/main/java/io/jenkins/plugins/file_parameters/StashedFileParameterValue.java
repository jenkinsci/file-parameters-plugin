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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.kohsuke.stapler.DataBoundConstructor;

public final class StashedFileParameterValue extends AbstractFileParameterValue {

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Doesn't make sense to persist it")
    private transient File tmp;
    
    @DataBoundConstructor public StashedFileParameterValue(String name, FileItem file) throws IOException {
        this(name, file.getInputStream());
        filename = file.getName();
        file.delete();
    }

    StashedFileParameterValue(String name, InputStream src) throws IOException {
        super(name);
        tmp = new File(Util.createTempDir(), name);
        tmp.deleteOnExit();
        FileUtils.copyInputStreamToFile(src, tmp);
    }

    @Override public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        super.buildEnvironment(build, env);
        if (tmp != null) {
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
                Files.deleteIfExists(tmp.toPath());
                tmp = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override protected FilePath createTempFile(Run<?, ?> build, FilePath tempDir, EnvVars env, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        StashManager.unstash(build, name, tempDir, launcher, env, listener);
        return tempDir.child(name);
    }

}
