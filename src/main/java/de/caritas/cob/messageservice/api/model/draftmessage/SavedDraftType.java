package de.caritas.cob.messageservice.api.model.draftmessage;

import org.springframework.http.HttpStatus;

public enum SavedDraftType {

  NEW_MESSAGE(HttpStatus.CREATED),
  OVERWRITTEN_MESSAGE(HttpStatus.OK);

  private final HttpStatus httpStatus;

  SavedDraftType(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {
    return this.httpStatus;
  }

}
