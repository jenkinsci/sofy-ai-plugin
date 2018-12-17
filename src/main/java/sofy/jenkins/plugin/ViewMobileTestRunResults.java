package sofy.jenkins.plugin;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class ViewMobileTestRunResults implements Action, RunAction2 {

    private AbstractBuild<?, ?> build;
    private CreateMobileTestRunResponse mobileTestRun;
    private final String apiToken;
    private transient Run run;

    public ViewMobileTestRunResults(final AbstractBuild<?, ?> build, CreateMobileTestRunResponse testRunResponse, String api_token) {
        this.build = build;
        this.mobileTestRun = testRunResponse;
        this.apiToken = api_token;
    }

    public CreateMobileTestRunResponse getMobileTestRun() {
        return this.mobileTestRun;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/sofy_plugin/img/sofy.png";
    }

    @Override
    public String getDisplayName() {
        return "Sofy.ai Mobile Test Run Results";
    }

    @Override
    public String getUrlName() {
        return "sofy_mobile_report";
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
        return this.apiToken;
    }
}
