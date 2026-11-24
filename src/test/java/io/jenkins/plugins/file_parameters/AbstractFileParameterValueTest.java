/*
 * The MIT License
 *
 * Copyright 2026 CloudBees, Inc.
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

import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.RootAction;
import hudson.model.Run;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.htmlunit.WebResponse;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies where the download link of a parameter value leads to.
 */
@WithJenkins
class AbstractFileParameterValueTest {

    private JenkinsRule r;
    private FreeStyleBuild build;
    private Base64FileParameterValue value;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        FreeStyleProject p = r.createFreeStyleProject("myjob");
        p.addProperty(new ParametersDefinitionProperty(new Base64FileParameterDefinition("FILE")));
        value = new Base64FileParameterValue("FILE");
        value.setBase64(Base64.getEncoder().encodeToString("uploaded content here".getBytes(StandardCharsets.UTF_8)));
        build = r.assertBuildStatusSuccess(p.scheduleBuild2(0, new ParametersAction(value)));
    }

    @Test
    void parametersPageLinksToTheBuild() throws Exception {
        try (JenkinsRule.WebClient wc = r.createWebClient()) {
            HtmlPage page = wc.getPage(build, "parameters");
            DomElement link = page.querySelector("a[href$='parameter/FILE/download']");
            assertNotNull(link, "no download link");
            assertThat(link.getAttribute("href"), is(r.contextPath + "/job/myjob/1/parameters/parameter/FILE/download"));
        }
    }

    /**
     * The parameters of a build are also shown on other pages than the Parameters page, for example by a dialog that
     * Jenkins renders on demand. The page then gives {@code value.jelly} the build, and the link has to work from there.
     */
    @Test
    void downloadLinkWorksFromAnotherPage() throws Exception {
        ExtensionList.lookupSingleton(ValuePage.class).show(build, value);
        try (JenkinsRule.WebClient wc = r.createWebClient().withThrowExceptionOnFailingStatusCode(false)) {
            HtmlPage page = wc.goTo(ValuePage.URL_NAME);
            DomElement link = page.querySelector("a[href$='parameter/FILE/download']");
            assertNotNull(link, "no download link");
            WebResponse response = wc.getPage(new URL(page.getUrl(), link.getAttribute("href"))).getWebResponse();
            assertThat(response.getStatusCode(), is(200));
            assertThat(response.getContentAsString(), is("uploaded content here"));
        }
    }

    /**
     * A page that shows a parameter value the way the parameters of a build are shown, but is not below the build.
     */
    @TestExtension("downloadLinkWorksFromAnotherPage")
    public static final class ValuePage implements RootAction {

        private static final String URL_NAME = "file-parameter-value";

        private Run<?, ?> run;
        private AbstractFileParameterValue value;

        void show(Run<?, ?> run, AbstractFileParameterValue value) {
            this.run = run;
            this.value = value;
        }

        public Run<?, ?> getRun() {
            return run;
        }

        public AbstractFileParameterValue getValue() {
            return value;
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return URL_NAME;
        }
    }
}
