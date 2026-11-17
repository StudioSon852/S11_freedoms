package vn.shoegroup.sd11.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class CheckoutForm {
    @NotBlank(message = "Vui lòng nhập người nhận") private String customerName;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 30, message = "Số điện thoại không hợp lệ")
    private String customerPhone;
    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String shippingAddress;
    private String customerNote;
    private String couponCode;
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán") private String paymentMethod = "BANK_TRANSFER";
    public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;}
    public String getCustomerPhone(){return customerPhone;} public void setCustomerPhone(String v){customerPhone=v;}
    public String getShippingAddress(){return shippingAddress;} public void setShippingAddress(String v){shippingAddress=v;}
    public String getCustomerNote(){return customerNote;} public void setCustomerNote(String v){customerNote=v;}
    public String getCouponCode(){return couponCode;} public void setCouponCode(String v){couponCode=v;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
}
