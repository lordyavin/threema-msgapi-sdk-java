package net.klesatschke.threema.api.exceptions;

import java.net.http.HttpResponse;

public class ClientError extends ThreemaError {
  private static final long serialVersionUID = 5586830942850922017L;

  public ClientError(HttpResponse<?> response, String string) {
    super(response, string);
  }
}
