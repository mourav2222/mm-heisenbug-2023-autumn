package guru.qa.rococo.api;

import guru.qa.rococo.config.Config;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;

public abstract class RestService {

  protected static final Config CFG = Config.getInstance();

  protected final OkHttpClient httpClient;
  protected final Retrofit retrofit;

  public RestService(String baseUrl, boolean followRedirect, Interceptor... interceptors) {
    this(baseUrl, followRedirect, null, interceptors);
  }

  public RestService(String baseUrl, boolean followRedirect, CookieStore cookieStore, Interceptor... interceptors) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .followRedirects(followRedirect);

    if (interceptors != null) {
      for (Interceptor interceptor : interceptors) {
        builder.addNetworkInterceptor(interceptor);
      }
    }

    if (cookieStore != null) {
      builder.cookieJar(new JavaNetCookieJar(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)));
    } else {
      builder.cookieJar(new JavaNetCookieJar(new CookieManager(ThreadLocalCookieStore.INSTANCE, CookiePolicy.ACCEPT_ALL)));
    }
    builder.addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC));

    this.httpClient = builder.build();
    this.retrofit = new Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(JacksonConverterFactory.create())
        .build();
  }
}
