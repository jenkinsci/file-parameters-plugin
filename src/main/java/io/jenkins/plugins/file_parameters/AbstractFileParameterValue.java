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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Failure;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * Implement either {@link #open} and/or {@link #createTempFile}.
 */
public abstract class AbstractFileParameterValue extends ParameterValue {

    private @CheckForNull String filename;

    protected AbstractFileParameterValue(String name) {
        super(name);
    }

    public final String getFilename() {
        return filename;
    }

    final void setFilename(String filename) {
        try {
            Jenkins.checkGoodName(filename);
            this.filename = filename;
        } catch (Failure x) {
            // Ignore and leave the filename undefined.
            // FileItem.getName Javadoc claims Opera might pass a full path.
            // This is a best effort anyway (scripts should be written to tolerate an undefined name).
        }
    }

    protected InputStream open(@CheckForNull Run<?,?> build) throws IOException, InterruptedException {
        assert Util.isOverridden(AbstractFileParameterValue.class, getClass(), "createTempFile", Run.class, FilePath.class, EnvVars.class, Launcher.class, TaskListener.class);
        if (build == null) {
            throw new IOException("Cannot operate outside of a build context");
        }
        FilePath tempDir = new FilePath(Util.createTempDir());
        FilePath f = createTempFile(build, tempDir, new EnvVars(EnvVars.masterEnvVars), new Launcher.LocalLauncher(TaskListener.NULL), TaskListener.NULL);
        return new FilterInputStream(f.read()) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    tempDir.deleteRecursive();
                } catch (InterruptedException x) {
                    throw new IOException(x);
                }
            }
        };
    }

    protected FilePath createTempFile(@NonNull Run<?,?> build, @NonNull FilePath tempDir, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException, InterruptedException {
        assert Util.isOverridden(AbstractFileParameterValue.class, getClass(), "open", Run.class);
        FilePath f = tempDir.createTempFile(StringUtils.rightPad(name, 3, 'x'), null);
        try (InputStream is = open(build)) {
            f.copyFrom(is);
        }
        return f;
    }

    public void doDownload(@AncestorInPath Run<?,?> build, StaplerResponse2 rsp) throws Exception {
        rsp.setContentType("application/octet-stream");
        try (InputStream is = open(build); OutputStream os = rsp.getOutputStream()) {
            IOUtils.copy(is, os);
        }
    }

    // TODO equals/hashCode
    
    @Override public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        super.buildEnvironment(build, env);
        String fname = getFilename();
        if (fname != null) {
            env.put(name + "_FILENAME", fname);
        }
    }

}
