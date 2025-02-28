package com.nineteen.omp.payment.service;

import com.nineteen.omp.coupon.domain.UserCoupon;
import com.nineteen.omp.order.domain.Order;
import com.nineteen.omp.payment.domain.Payment;
import com.nineteen.omp.payment.domain.PaymentStatus;
import com.nineteen.omp.payment.exception.PaymentException.PaymentNotFoundException;
import com.nineteen.omp.payment.exception.PaymentException.PaymentNotOwnerException;
import com.nineteen.omp.payment.exception.PaymentException.PaymentNotUserException;
import com.nineteen.omp.payment.exception.PaymentException.PaymentNotValidCancelRequestException;
import com.nineteen.omp.payment.repository.PaymentRepository;
import com.nineteen.omp.payment.service.dto.CreatePaymentRequestCommand;
import com.nineteen.omp.payment.service.dto.ExecutePaymentRequestCommand;
import com.nineteen.omp.payment.service.dto.ExecutePaymentResponseCommand;
import com.nineteen.omp.payment.service.dto.GetPaymentListResponseCommand;
import com.nineteen.omp.payment.service.dto.GetPaymentResponseCommand;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;

  @Override
  @Transactional
  public void createPayment(CreatePaymentRequestCommand createPaymentRequestCommand) {

    int totalAmount =
        calculateTotalAmount(createPaymentRequestCommand.order(),
            createPaymentRequestCommand.userCoupon());

    Payment newPayment = Payment.builder()
        .order(createPaymentRequestCommand.order())
        .userCoupon(createPaymentRequestCommand.userCoupon())
        .totalAmount(totalAmount)
        .pgProvider(createPaymentRequestCommand.pgProvider())
        .status(PaymentStatus.PENDING)
        .method(createPaymentRequestCommand.paymentMethod())
        .build();

    ExecutePaymentResponseCommand responseCommand = executePaymentGateway(newPayment);

    newPayment.success(responseCommand.pgTid());

    paymentRepository.save(newPayment);
  }

  private ExecutePaymentResponseCommand executePaymentGateway(Payment newPayment) {
    PaymentGatewayService paymentGatewayService = new DummyPaymentGatewayService();
    ExecutePaymentRequestCommand executePaymentRequestCommand =
        new ExecutePaymentRequestCommand(newPayment);
    return paymentGatewayService.executePayment(executePaymentRequestCommand);
  }

  @Override
  @Transactional
  public void cancelPayment(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    payment.cancelForce();
  }

  @Override
  public GetPaymentResponseCommand getPaymentById(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    return new GetPaymentResponseCommand(payment);
  }

  private int calculateTotalAmount(Order order, UserCoupon userCoupon) {
    int totalAmount = order.getTotalPrice();

    if (userCoupon != null) {
      totalAmount = userCoupon.useCoupon(totalAmount);
    }
    return totalAmount;
  }

  @Override
  public GetPaymentListResponseCommand getUsersPaymentList(Long userId, Pageable pageable) {
    Page<Payment> payments = paymentRepository.findByOrder_User_Id(userId, pageable);
    return convertGetPaymentListResponseCommand(payments);
  }

  @Override
  public GetPaymentListResponseCommand getStoresPaymentList(UUID storeId, Pageable pageable) {
    Page<Payment> payments = paymentRepository.findByOrder_Store_Id(storeId, pageable);
    return convertGetPaymentListResponseCommand(payments);
  }

  @Override
  public void isOwnersPayment(Long ownerId, UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    if (!payment.getOrder().getStore().getUser().getId().equals(ownerId)) {
      throw new PaymentNotOwnerException();
    }
  }

  @Override
  @Transactional
  public void cancelPaymentRequest(Long userId, UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    if (!payment.getOrder().getUser().getId().equals(userId)) {
      throw new PaymentNotUserException();
    }
    payment.cancelRequest();
  }

  @Override
  @Transactional
  public void cancelPaymentRequestDenied(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    if (!payment.isCancelRequest()) {
      throw new PaymentNotValidCancelRequestException();
    }
    payment.cancelRequestDenied();
  }

  @Override
  public void isUsersPayment(Long userId, UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(PaymentNotFoundException::new);
    if (!payment.getOrder().getUser().getId().equals(userId)) {
      throw new PaymentNotUserException();
    }
  }

  @Override
  public GetPaymentListResponseCommand searchPaymentListByUserNickname(
      String nickname,
      Pageable pageable
  ) {
    Page<Payment> payments = paymentRepository.findByOrder_User_Nickname(nickname, pageable);
    return convertGetPaymentListResponseCommand(payments);
  }

  @Override
  public GetPaymentListResponseCommand searchPaymentListByStoreName(
      String storeName,
      Pageable pageable
  ) {
    Page<Payment> payments = paymentRepository.findByOrder_Store_Name(storeName, pageable);
    return convertGetPaymentListResponseCommand(payments);
  }

  private static GetPaymentListResponseCommand convertGetPaymentListResponseCommand(
      Page<Payment> payments
  ) {
    List<GetPaymentResponseCommand> contents = payments.stream()
        .map(GetPaymentResponseCommand::new)
        .toList();
    PageImpl<GetPaymentResponseCommand> responseCommands =
        new PageImpl<>(contents, payments.getPageable(), payments.getTotalElements());
    return new GetPaymentListResponseCommand(responseCommands);
  }

}
