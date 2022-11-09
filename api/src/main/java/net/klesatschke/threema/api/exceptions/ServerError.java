package net.klesatschke.threema.api.exceptions;

import java.net.http.HttpResponse;

public class ServerError extends ThreemaError {

  private static final long serialVersionUID = 1306889474607193102L;

  public ServerError(HttpResponse<?> response) {
    super(response);
  }

  public ServerError(HttpResponse<?> response, String string) {
    super(response, string);
  }
}
