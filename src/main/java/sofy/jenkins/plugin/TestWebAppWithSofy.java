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
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TestWebAppWithSofy extends Recorder {

    private String apiToken;
    private String webUrl;
    private Integer numberOfNodes;
    private CreateWebTestRunResponse testRunResponse;

    @DataBoundConstructor
    public TestWebAppWithSofy(String apiToken, String webUrl, String numberOfNodes) {
        this.apiToken = apiToken;
        this.webUrl = webUrl;
        this.numberOfNodes = Integer.parseInt(numberOfNodes == null || numberOfNodes.isEmpty() ? "20" : numberOfNodes);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            listener.getLogger().println("Preparing to Stage a test run for your Website on Sofy.ai");
            String testRunInfo = stageWebTestRun(listener.getLogger())
                    .replace("WebTestRunID", "webTestRunID")
                    .replace("ApplicationID", "applicationID")
                    .replace("CreatedDatetime", "createdDateTime");
            if (!testRunInfo.isEmpty()) {
                this.testRunResponse = new ObjectMapper().readValue(testRunInfo, CreateWebTestRunResponse.class);
                listener.getLogger().println("Test Run scheduled!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.getLogger().println("Unable to stage test run. Following error occurred.");
        }
        build.addAction(new ViewWebTestRunResults(build, this.testRunResponse, this.apiToken));
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    private String stageWebTestRun(PrintStream logger) throws Exception {
        // http
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("https://api.sofy.ai/v1/websites/create");
        // set Subscription Key
        String subKey = this.apiToken;
        try {
            subKey = UUID.fromString(this.apiToken).toString();
        } catch (Exception e) {
            e.printStackTrace();
            logger.println("Invalid API Key, unable to Stage test run on Sofy.ai. Please refresh your API Key");
        }
        request.setHeader("SubscriptionKey", subKey);
        request.setHeader("Content-Type", "application/json");
        // check if URL is valid
        logger.println("Staging test run with the following URL: \"" + this.webUrl + "\"");
        // add request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("browser", "chrome");
        requestBody.put("NodesToTest", this.numberOfNodes);
        requestBody.put("WebTestRunType", new String[]{"all"});
        requestBody.put("url", this.webUrl);
        request.setEntity(new StringEntity(requestBody.toString()));
        // wait for response
        HttpResponse response = client.execute(request);
        return new BasicResponseHandler().handleResponse(response);
    }


    @Symbol("createWebTestRun")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Schedule Website Test Run on Sofy.ai";
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
