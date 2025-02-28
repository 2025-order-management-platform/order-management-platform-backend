package com.nineteen.omp.payment.exception;

import com.nineteen.omp.global.exception.CustomException;

public class PaymentException extends CustomException {

  public PaymentException(PaymentExceptionCode paymentExceptionCode) {
    super(paymentExceptionCode);
  }

  public static class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException() {
      super(PaymentExceptionCode.NOT_FOUND_PAYMENT);
    }
  }

  public static class PaymentNotUserException extends PaymentException {

    public PaymentNotUserException() {
      super(PaymentExceptionCode.NOT_USER_PAYMENT);
    }
  }

  public static class PaymentNotValidCancelRequestException extends PaymentException {

    public PaymentNotValidCancelRequestException() {
      super(PaymentExceptionCode.NOT_VALID_CANCEL_REQUEST);
    }
  }

  public static class PaymentNotOwnerException extends PaymentException {

    public PaymentNotOwnerException() {
      super(PaymentExceptionCode.NOT_OWNER_PAYMENT);
    }
  }

}
