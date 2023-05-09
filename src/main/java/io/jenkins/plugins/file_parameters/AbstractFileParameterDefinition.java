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

import hudson.cli.CLICommand;
import hudson.model.Failure;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

abstract class AbstractFileParameterDefinition extends ParameterDefinition {

    protected AbstractFileParameterDefinition(String name) {
        super(name);
        Jenkins.checkGoodName(name);
    }

    protected Object readResolve() {
        Jenkins.checkGoodName(getName());
        return this;
    }

    protected abstract Class<? extends AbstractFileParameterValue> valueType();

    protected abstract AbstractFileParameterValue createValue(String name, InputStream src) throws IOException;

    @Override public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        AbstractFileParameterValue p = req.bindJSON(valueType(), jo);
        p.setDescription(getDescription());
        return p;
    }

    @Override public ParameterValue createValue(StaplerRequest req) {
        try {
            FileItem src;
            try {
                src = req.getFileItem(getName());
            } catch (ServletException x) {
                if (x.getCause() instanceof FileUploadBase.InvalidContentTypeException) {
                    src = null;
                } else {
                    throw x;
                }
            }
            if (src == null) {
                return null;
            }
            AbstractFileParameterValue p;
            try (InputStream in = src.getInputStream()) {
                p = createValue(getName(), in);
            }
            src.delete();
            p.setDescription(getDescription());
            p.setFilename(src.getName());
            return p;
        } catch (ServletException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
        AbstractFileParameterValue p;
        if (value.isEmpty()) {
            p = createValue(getName(), command.stdin);
        } else {
            byte[] data = Base64.getDecoder().decode(value);
            p = createValue(getName(), new ByteArrayInputStream(data));
        }
        p.setDescription(getDescription());
        return p;
    }

    protected static abstract class AbstractFileParameterDefinitionDescriptor extends ParameterDescriptor {

        public FormValidation doCheckName(@QueryParameter String name) {
            try {
                Jenkins.checkGoodName(name);
                return FormValidation.ok();
            } catch (Failure x) {
                return FormValidation.error(x.getMessage());
            }
        }

    }

}
