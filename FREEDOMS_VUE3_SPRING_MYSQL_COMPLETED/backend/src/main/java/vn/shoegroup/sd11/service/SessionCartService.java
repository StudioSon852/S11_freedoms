package vn.shoegroup.sd11.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.dto.CartViewLine;
import vn.shoegroup.sd11.entity.ProductVariant;
import vn.shoegroup.sd11.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SessionCartService {
    public static final String CUSTOMER_CART = "FREEDOMS_CUSTOMER_CART";
    public static final String POS_CART = "FREEDOMS_POS_CART";
    private final ProductVariantRepository variants;
    public SessionCartService(ProductVariantRepository variants) { this.variants = variants; }

    @SuppressWarnings("unchecked")
    public Map<Long, Integer> getMap(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value instanceof Map<?, ?> map) return (Map<Long, Integer>) map;
        Map<Long, Integer> created = new LinkedHashMap<>(); session.setAttribute(key, created); return created;
    }
    public void add(HttpSession session, String key, Long variantId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        ProductVariant v = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!Boolean.TRUE.equals(v.getActive())) throw new IllegalArgumentException("Sản phẩm đã ngừng kinh doanh.");
        Map<Long,Integer> map = getMap(session,key);
        int newQty = map.getOrDefault(variantId, 0) + quantity;
        if (newQty > v.getStockQuantity()) throw new IllegalArgumentException("Số lượng vượt quá tồn kho hiện tại.");
        map.put(variantId,newQty);
    }
    public List<CartViewLine> view(HttpSession session, String key) {
        List<CartViewLine> lines = new ArrayList<>();
        for (Map.Entry<Long,Integer> entry : getMap(session,key).entrySet()) {
            variants.findById(entry.getKey()).ifPresent(v -> lines.add(new CartViewLine(v, entry.getValue(), v.getUnitPrice().multiply(BigDecimal.valueOf(entry.getValue())))));
        }
        return lines;
    }
    public List<CartEntry> entries(HttpSession session, String key) {
        List<CartEntry> lines = new ArrayList<>();
        getMap(session,key).forEach((id,qty) -> lines.add(new CartEntry(id,qty)));
        return lines;
    }
    public void update(HttpSession session, String key, Long variantId, int quantity) {
        Map<Long,Integer> map = getMap(session,key);
        if (quantity <= 0) { map.remove(variantId); return; }
        ProductVariant v = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (quantity > v.getStockQuantity()) throw new IllegalArgumentException("Số lượng vượt quá tồn kho hiện tại.");
        map.put(variantId,quantity);
    }
    public void remove(HttpSession session,String key,Long variantId){getMap(session,key).remove(variantId);}
    public void clear(HttpSession session,String key){session.removeAttribute(key);}
}
