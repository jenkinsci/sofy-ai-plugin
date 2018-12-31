package sofy.jenkins.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class TestMobileAppWithSofy extends Recorder {

    private String apiToken;
    private String apkPath;
    private CreateMobileTestRunResponse testRunResponse;

    @DataBoundConstructor
    public TestMobileAppWithSofy(String apiToken, String apkPath) {
        this.apiToken = apiToken;
        this.apkPath = apkPath;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            listener.getLogger().println("Preparing to Stage a test run for your Application on Sofy.ai");
            String apkLocation = build.getWorkspace() + "/" + this.apkPath;
            String testRunInfo = stageMobileTestRun(listener.getLogger(), apkLocation);
            if (testRunInfo != null && !testRunInfo.isEmpty()) {
                this.testRunResponse = new ObjectMapper().readValue(testRunInfo.replaceAll("[\\[\\]]", ""), CreateMobileTestRunResponse.class);
                listener.getLogger().println("Test Run scheduled!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.getLogger().println("Unable to stage test run. An error occurred.");
        }
        build.addAction(new ViewMobileTestRunResults(build, this.testRunResponse, this.apiToken));
        return true;
    }

    private String stageMobileTestRun(PrintStream logger, String apkLocation) throws Exception {
        // http
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost httppost = new HttpPost("https://api.sofy.ai/v1/applications/create?deviceid=68");
        // set Subscription Key
        String subKey = this.apiToken;
        try {
            subKey = UUID.fromString(this.apiToken).toString();
        } catch (Exception e) {
            e.printStackTrace();
            logger.println("Invalid API Key, unable to Stage test run on Sofy.ai. Please refresh your API Key");
        }
        httppost.setHeader("SubscriptionKey", subKey);
        // Upload APK
        File apkHandle = null;
        try {
            apkHandle = new File(new File(apkLocation).getAbsolutePath());
            // path is wrong
            if (!apkHandle.exists()) {
                logger.println("Invalid APK location provided. Provided path does not exist: \"" + apkHandle.getAbsolutePath() + "\"");
                logger.println("Unable to stage test run on Sofy.ai");
                return "";
            }

            if (apkHandle.isDirectory()) {
                apkHandle = Arrays.stream(apkHandle.listFiles() != null ? apkHandle.listFiles() : new File[]{})
                        .filter(file -> file.getName().toLowerCase().endsWith(".apk"))
                        .findFirst()
                        .orElse(null);
            }

            if (apkHandle == null || !apkHandle.getName().toLowerCase().endsWith(".apk")) {
                logger.println("Invalid APK location provided. No '.apk' file found in provided directory: \"" + (apkHandle == null ? "" : apkHandle.getAbsolutePath()) + "\"");
                logger.println("Unable to stage test run on Sofy.ai");
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.println("Invalid APK location provided. Unable to stage test run on Sofy.ai");
        }

        if (apkHandle != null) {
            logger.println("Staging test run with the following APK: \"" + apkHandle.getAbsolutePath() + "\"");
            FileBody fileBodyApk = new FileBody(apkHandle, ContentType.DEFAULT_BINARY);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("applicationFile", fileBodyApk);
            HttpEntity entity = builder.build();
            httppost.setEntity(entity);
            // wait for response
            HttpResponse response = client.execute(httppost);
            return new BasicResponseHandler().handleResponse(response);
        }

        return "";
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Symbol("createMobileTestRun")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Schedule Mobile App Test Run on Sofy.ai";
        }


        public FormValidation doCheckAuthTokenValidity(@QueryParameter("apiToken") final String apiToken) {

            try {
                if (checkApiTokenExists(apiToken.trim())) {
                    return FormValidation.ok("Your API Key is valid");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Invalid API Key Entered");
            }

            return FormValidation.error("Invalid API Key");
        }


        private boolean checkApiTokenExists(String apiToken) throws Exception {

            UUID uuid = UUID.fromString(apiToken);

            StringBuilder result = new StringBuilder();
            URL url = new URL("https://api.sofy.ai/api/Plugin/validateAPIKey?api_key=" + uuid.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return result.toString().contains("1");
        }

    }


}
