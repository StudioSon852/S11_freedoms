package vn.shoegroup.sd11.shipping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ShippingService {
    private final RestClient restClient = RestClient.create();
    @Value("${app.shipping.ghn.base-url:https://online-gateway.ghn.vn/shiip/public-api}") private String ghnBaseUrl;
    @Value("${app.shipping.ghn.token:}") private String ghnToken;
    @Value("${app.shipping.ghn.shop-id:}") private String ghnShopId;
    @Value("${app.shipping.ghn.service-type-id:2}") private Integer serviceTypeId;
    @Value("${app.shipping.free-threshold:1000000}") private BigDecimal freeThreshold;
    @Value("${app.shipping.fallback.hanoi:20000}") private BigDecimal hanoiFee;
    @Value("${app.shipping.fallback.danang:32000}") private BigDecimal danangFee;
    @Value("${app.shipping.fallback.hcm:38000}") private BigDecimal hcmFee;
    @Value("${app.shipping.fallback.other:30000}") private BigDecimal otherFee;

    public Quote calculate(String channel, String province, Integer toDistrictId, String toWardCode,
                           int weightGram, BigDecimal merchandiseAfterDiscount) {
        BigDecimal merchandise = merchandiseAfterDiscount == null ? BigDecimal.ZERO : merchandiseAfterDiscount.max(BigDecimal.ZERO);
        if ("POS".equalsIgnoreCase(channel)) return new Quote(BigDecimal.ZERO, "STORE_PICKUP", "Bán hàng tại quầy/nhận tại cửa hàng không tính phí giao hàng.");
        if (merchandise.compareTo(freeThreshold) >= 0) return new Quote(BigDecimal.ZERO, "FREE_THRESHOLD", "Miễn phí vận chuyển vì tiền hàng sau giảm đạt 1.000.000đ.");
        if (hasGhnCredentials() && toDistrictId != null && toDistrictId > 0 && toWardCode != null && !toWardCode.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                int selectedServiceType = weightGram >= 20000 ? 5 : serviceTypeId;
                Map<String,Object> response = restClient.post().uri(ghnBaseUrl + "/v2/shipping-order/fee")
                        .header("Token", ghnToken).header("ShopId", ghnShopId).header("Content-Type", "application/json")
                        .body(Map.of("service_type_id", selectedServiceType, "to_district_id", toDistrictId,
                                "to_ward_code", toWardCode, "weight", Math.max(weightGram, 500),
                                "length", 30, "width", 20, "height", 10, "insurance_value", 0))
                        .retrieve().body(Map.class);
                if (response != null && Integer.valueOf(200).equals(asInt(response.get("code")))) {
                    Object dataObj = response.get("data");
                    if (dataObj instanceof Map<?,?> data && data.get("total") != null) {
                        BigDecimal total = new BigDecimal(data.get("total").toString());
                        return new Quote(total, "GHN", "Phí vận chuyển được báo trực tiếp từ GHN.");
                    }
                }
            } catch (RestClientException | NumberFormatException ignored) {
                // GHN unavailable: continue with a transparent region fallback rather than blocking checkout.
            }
        }
        String key = province == null ? "" : province.toLowerCase().replace("đ", "d");
        BigDecimal fee = key.contains("ha noi") || key.contains("hà nội") ? hanoiFee
                : key.contains("da nang") || key.contains("đà nẵng") || key.contains("dà nẵng") ? danangFee
                : key.contains("ho chi minh") || key.contains("hồ chí minh") || key.contains("tp.hcm") || key.contains("tp hcm") ? hcmFee
                : otherFee;
        return new Quote(fee.setScale(0, RoundingMode.HALF_UP), "REGION_FALLBACK",
                "GHN chưa có cấu hình hoặc chưa nhận đủ mã quận/phường; áp dụng phí khu vực dự phòng.");
    }

    private boolean hasGhnCredentials() { return ghnToken != null && !ghnToken.isBlank() && ghnShopId != null && !ghnShopId.isBlank(); }
    private Integer asInt(Object value) { try { return value == null ? null : Integer.valueOf(value.toString()); } catch (Exception ex) { return null; } }
    public record Quote(BigDecimal fee, String source, String message) {}
}
