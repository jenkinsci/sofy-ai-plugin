package sofy.jenkins.plugin;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class ViewWebTestRunResults implements Action, RunAction2 {

    private AbstractBuild<?, ?> build;
    private transient Run run;
    private CreateWebTestRunResponse webTestRun;
    private final String apiToken;

    public ViewWebTestRunResults(AbstractBuild build, CreateWebTestRunResponse testRunResponse, String api_token) {
        this.build = build;
        this.webTestRun = testRunResponse;
        this.apiToken = api_token;
    }

    public CreateWebTestRunResponse getWebTestRun() {
        return this.webTestRun;
    }


    @Override
    public String getIconFileName() {
        return "/plugin/sofy_plugin/img/sofy.png";
    }

    @Override
    public String getDisplayName() {
        return "Sofy.ai Website Test Run Results";
    }

    @Override
    public String getUrlName() {
        return "sofy_web_report";
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }


    public Run getRun() {
        return this.run;
    }

    public String getApiToken() {
        return apiToken;
    }
}
