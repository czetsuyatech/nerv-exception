package com.czetsuyatech.nerv.exception.api;

public interface NervErrorCode {

  String code();

  String message();

  int status();

  default boolean retryable() {
    return false;
  }

  default String category() {
    return "GENERAL";
  }
}
