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

import hudson.Extension;
import hudson.model.ParameterDefinition;
import java.io.IOException;
import java.io.InputStream;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public final class Base64FileParameterDefinition extends AbstractFileParameterDefinition {

    @DataBoundConstructor public Base64FileParameterDefinition(String name) {
        super(name);
    }

    @Override protected Class<? extends AbstractFileParameterValue> valueType() {
        return Base64FileParameterValue.class;
    }

    @Override protected AbstractFileParameterValue createValue(String name, InputStream src) throws IOException {
        return new Base64FileParameterValue(name, src);
    }

    // TODO equals/hashCode

    @Symbol("base64File")
    @Extension public static final class DescriptorImpl extends ParameterDefinition.ParameterDescriptor {
        
        @Override public String getDisplayName() {
            return "Base64 File Parameter";
        }
    }

}
