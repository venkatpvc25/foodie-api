package com.pvc.foodie.feature.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.config.RazorpayProperties;
import com.pvc.foodie.feature.order.dto.RazorpayCheckoutResponse;
import com.pvc.foodie.feature.order.dto.RazorpayTransferPlan;
import com.pvc.foodie.feature.order.entity.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Transfer;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayGatewayService {

    private final RazorpayProperties properties;
    private final RazorpaySplitPaymentService razorpaySplitPaymentService;

    public RazorpayCheckoutResponse createOrder(Order order) {
        ensureConfigured();
        try {
            RazorpayClient client = client();
            JSONObject request = new JSONObject();
            Long amount = toCurrencySubunits(order.getTotal());
            RazorpayTransferPlan transferPlan = razorpaySplitPaymentService.planTransfers(order);
            request.put("amount", amount);
            request.put("currency", properties.currency());
            request.put("receipt", order.getOrderNumber());
            request.put("transfers", transfers(order, transferPlan));

            JSONObject notes = new JSONObject();
            notes.put("app_order_id", order.getId().toString());
            notes.put("order_number", order.getOrderNumber());
            request.put("notes", notes);

            com.razorpay.Order razorpayOrder = client.orders.create(request);
            String razorpayOrderId = razorpayOrder.get("id");
            order.setRazorpayOrderId(razorpayOrderId);
            order.setRestaurantPayoutAmount(razorpaySplitPaymentService.toMajorUnits(transferPlan.restaurantAmount()));
            order.setPlatformCommissionAmount(razorpaySplitPaymentService.toMajorUnits(transferPlan.platformCommissionAmount()));
            applyTransferIds(order, razorpayOrder);

            log.info("Razorpay order created with split: orderId={}, orderNumber={}, razorpayOrderId={}, amount={}, restaurantAmount={}, platformCommissionAmount={}, currency={}",
                    order.getId(), order.getOrderNumber(), razorpayOrderId, amount, transferPlan.restaurantAmount(),
                    transferPlan.platformCommissionAmount(), properties.currency());
            return checkoutResponse(order);
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed: orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber(), ex);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unable to create Razorpay order");
        }
    }

    public RazorpayCheckoutResponse checkoutResponse(Order order) {
        if (!hasRazorpayOrder(order)) {
            return null;
        }
        return new RazorpayCheckoutResponse(
                properties.keyId(),
                order.getRazorpayOrderId(),
                toCurrencySubunits(order.getTotal()),
                properties.currency(),
                order.getOrderNumber());
    }

    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        ensureConfigured();
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, properties.keySecret());
        } catch (RazorpayException ex) {
            log.warn("Razorpay signature verification failed: razorpayOrderId={}, razorpayPaymentId={}",
                    razorpayOrderId, razorpayPaymentId, ex);
            return false;
        }
    }

    public String transferDeliveryCharge(Order order, String deliveryPartnerAccountId) {
        ensureConfigured();
        try {
            RazorpayClient client = client();
            JSONObject request = new JSONObject();
            JSONArray transfers = new JSONArray();
            transfers.put(transfer(
                    deliveryPartnerAccountId,
                    toCurrencySubunits(order.getDeliveryCharge()),
                    "delivery_partner",
                    order));
            request.put("transfers", transfers);

            List<Transfer> createdTransfers = client.payments.transfer(order.getRazorpayPaymentId(), request);
            requireDeliveryTransferCreated(createdTransfers);

            String transferId = createdTransfers.get(0).get("id");
            log.info("Razorpay delivery transfer created: orderId={}, paymentId={}, transferId={}, deliveryPartnerAccountIdLength={}, amount={}",
                    order.getId(), order.getRazorpayPaymentId(), transferId, deliveryPartnerAccountId.length(),
                    order.getDeliveryCharge());
            return transferId;
        } catch (RazorpayException ex) {
            log.error("Razorpay delivery transfer failed: orderId={}, paymentId={}",
                    order.getId(), order.getRazorpayPaymentId(), ex);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unable to transfer delivery charge");
        }
    }

    private RazorpayClient client() throws RazorpayException {
        return new RazorpayClient(properties.keyId(), properties.keySecret());
    }

    private JSONArray transfers(Order order, RazorpayTransferPlan transferPlan) {
        JSONArray transfers = new JSONArray();
        transfers.put(transfer(
                transferPlan.restaurantAccountId(),
                transferPlan.restaurantAmount(),
                "restaurant",
                order));
        if (hasAdminTransfer(transferPlan)) {
            transfers.put(transfer(
                    transferPlan.adminAccountId(),
                    transferPlan.adminTransferAmount(),
                    "admin",
                    order));
        }
        return transfers;
    }

    private JSONObject transfer(String accountId, Long amount, String recipientType, Order order) {
        JSONObject transfer = new JSONObject();
        transfer.put("account", accountId);
        transfer.put("amount", amount);
        transfer.put("currency", properties.currency());
        transfer.put("on_hold", false);

        JSONObject notes = new JSONObject();
        notes.put("app_order_id", order.getId().toString());
        notes.put("order_number", order.getOrderNumber());
        notes.put("recipient_type", recipientType);
        transfer.put("notes", notes);
        return transfer;
    }

    private void applyTransferIds(Order order, com.razorpay.Order razorpayOrder) {
        Object transfersObject = razorpayOrder.get("transfers");
        if (!hasTransferArray(transfersObject)) {
            return;
        }
        JSONArray transfers = (JSONArray) transfersObject;
        for (int i = 0; i < transfers.length(); i++) {
            JSONObject transfer = transfers.getJSONObject(i);
            String transferId = transfer.optString("id", null);
            String recipient = transfer.optString("recipient", null);
            if (!hasTransferRecipient(transferId, recipient)) {
                continue;
            }
            if (isRestaurantRecipient(order, recipient)) {
                order.setRestaurantRazorpayTransferId(transferId);
            } else {
                order.setAdminRazorpayTransferId(transferId);
            }
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay payments are not configured");
        }
    }

    private void requireDeliveryTransferCreated(List<Transfer> createdTransfers) {
        if (createdTransfers.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay delivery transfer was not created");
        }
    }

    private boolean hasRazorpayOrder(Order order) {
        return order.getRazorpayOrderId() != null;
    }

    private boolean hasAdminTransfer(RazorpayTransferPlan transferPlan) {
        return transferPlan.adminTransferAmount() > 0;
    }

    private boolean hasTransferArray(Object transfersObject) {
        return transfersObject instanceof JSONArray;
    }

    private boolean hasTransferRecipient(String transferId, String recipient) {
        return transferId != null && recipient != null;
    }

    private boolean isRestaurantRecipient(Order order, String recipient) {
        return recipient.equals(order.getRestaurant().getRazorpayLinkedAccountId());
    }

    private boolean isConfigured() {
        return properties.enabled() && !isBlank(properties.keyId()) && !isBlank(properties.keySecret());
    }

    private Long toCurrencySubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
