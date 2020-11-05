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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.kohsuke.stapler.DataBoundConstructor;

// TODO variant to archive instead

public final class StashedFileParameterValue extends AbstractFileParameterValue {

    private transient File tmp;
    
    @DataBoundConstructor public StashedFileParameterValue(String name, FileItem file) throws IOException {
        this(name, file.getInputStream());
        file.delete();
    }

    StashedFileParameterValue(String name, InputStream src) throws IOException {
        super(name);
        tmp = new File(Util.createTempDir(), name);
        tmp.deleteOnExit();
        FileUtils.copyInputStreamToFile(src, tmp);
    }

    @Override public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        if (tmp != null) {
            TaskListener listener = TaskListener.NULL; // TODO no option to print to build log
            try {
                StashManager.stash(build, name, new FilePath(tmp.getParentFile()), new Launcher.LocalLauncher(listener), env, listener, tmp.getName(), null, false, false);
            } catch (IOException | InterruptedException x) {
                throw new RuntimeException(x);
            }
            tmp.delete();
            tmp = null;
        }
    }

    @Override protected InputStream open() throws IOException {
        throw new IOException(); // TODO StashManager.unstash to a temp dir
    }

}
