package net.klesatschke.threema.api.exceptions;

import java.io.IOException;
import java.net.http.HttpResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ThreemaError extends IOException {
  private static final long serialVersionUID = -5089520812601719876L;
  @Getter private final HttpResponse<?> response;

  public ThreemaError(HttpResponse<?> response, String string) {
    super(string);
    this.response = response;
  }
}
