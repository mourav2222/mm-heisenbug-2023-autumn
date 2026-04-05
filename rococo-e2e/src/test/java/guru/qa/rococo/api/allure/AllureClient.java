package guru.qa.rococo.api.allure;

import guru.qa.rococo.api.RestService;
import guru.qa.rococo.model.allure.AllureProject;
import guru.qa.rococo.model.allure.AllureResults;
import guru.qa.rococo.model.allure.LoginRequest;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.Optional;

public class AllureClient extends RestService {

  private final AllureApi allureDockerApi;

  public AllureClient() {
    super(
        Optional.ofNullable(
            System.getenv("ALLURE_DOCKER_API")
        ).orElse("http://127.0.0.1:5050/"),
        false,
        AllureClientCookieStore.INSTANCE,
        new CsrfTokenInterceptor());
    this.allureDockerApi = retrofit.create(AllureApi.class);
  }

  public void login(String username, String password) throws IOException {
    int code = allureDockerApi.login(new LoginRequest(username, password)).execute().code();
    Assertions.assertEquals(200, code);
  }

  public void clean(String projectId) throws IOException {
    allureDockerApi.cleanResults(projectId).execute();
  }

  public void generateReport(String projectId) throws IOException {
    allureDockerApi.generateReport(
        projectId,
        System.getenv("HEAD_COMMIT_MESSAGE"),
        System.getenv("BUILD_URL"),
        System.getenv("EXECUTION_TYPE")
    ).execute();
  }

  public void sendResultsToAllure(String projectId, AllureResults allureResults) throws IOException {
    int code = allureDockerApi.uploadResults(
        projectId,
        allureResults
    ).execute().code();
    Assertions.assertEquals(200, code);
  }

  public void createProjectIfNotExist(String projectId) throws IOException {
    int code = allureDockerApi.project(
        projectId
    ).execute().code();
    if (code == 404) {
      code = allureDockerApi.createProject(new AllureProject(projectId)).execute().code();
      Assertions.assertEquals(201, code);
    } else {
      Assertions.assertEquals(200, code);
    }
  }

  private static class CsrfTokenInterceptor implements Interceptor {

    CsrfTokenInterceptor() {
    }

    @Override
    public @NonNull Response intercept(Interceptor.Chain chain) throws IOException {
      Request original = chain.request();
      String csrfToken = getCsrfToken();

      if (csrfToken != null && !csrfToken.isEmpty()) {
        Request.Builder builder = original.newBuilder()
            .header("X-CSRF-TOKEN", csrfToken);
        Request request = builder.build();
        return chain.proceed(request);
      }

      return chain.proceed(original);
    }

    private String getCsrfToken() {
      try {
        return AllureClientCookieStore.INSTANCE.cookieValue("csrf_access_token");
      } catch (Exception e) {
        // Silently ignore exceptions when extracting CSRF token
        return null;
      }
    }
  }
}