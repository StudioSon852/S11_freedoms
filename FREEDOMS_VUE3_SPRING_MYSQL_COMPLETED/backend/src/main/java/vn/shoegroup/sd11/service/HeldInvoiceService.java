package vn.shoegroup.sd11.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class HeldInvoiceService {
    private final HeldInvoiceRepository invoices;
    private final HeldInvoiceItemRepository items;
    private final ProductVariantRepository variants;
    private final UserRepository users;
    private final OrderService orderService;

    public HeldInvoiceService(HeldInvoiceRepository invoices, HeldInvoiceItemRepository items,
            ProductVariantRepository variants, UserRepository users, OrderService orderService) {
        this.invoices=invoices; this.items=items; this.variants=variants; this.users=users; this.orderService=orderService;
    }


    @Transactional
    public HeldInvoice createDraft(AppUser staff, String customerName, String phone) {
        AppUser lockedStaff = users.findByIdForUpdate(staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên."));
        if (invoices.countByCreatedBy_IdAndStatus(lockedStaff.getId(), "OPEN") >= 10)
            throw new IllegalArgumentException("Mỗi nhân viên chỉ được mở tối đa 10 quầy khách cùng lúc.");
        AppUser customer = phone == null || phone.isBlank() ? null : users.findFirstByPhone(phone.trim())
                .filter(u -> "CUSTOMER".equals(u.getRole().getRoleCode())).orElse(null);
        HeldInvoice invoice = new HeldInvoice();
        invoice.setHeldCode("POS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        invoice.setCreatedBy(lockedStaff); invoice.setCustomer(customer);
        invoice.setCustomerName(blankTo(customerName, customer != null ? customer.getFullName() : "Khách lẻ"));
        invoice.setCustomerPhone(blankTo(phone, customer != null ? customer.getPhone() : ""));
        invoice.setStatus("OPEN"); invoice.setCreatedAt(LocalDateTime.now()); invoice.setUpdatedAt(LocalDateTime.now());
        return invoices.save(invoice);
    }

    @Transactional
    public HeldInvoice replaceDraft(Long id, AppUser actor, String customerName, String phone, String note,
                                    String couponCode, List<CartEntry> cart) {
        HeldInvoice invoice = invoices.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quầy khách."));
        ensureAccessible(invoice, actor);
        if (!"OPEN".equals(invoice.getStatus())) throw new IllegalArgumentException("Quầy khách đã đóng.");
        AppUser customer = phone == null || phone.isBlank() ? null : users.findFirstByPhone(phone.trim())
                .filter(u -> "CUSTOMER".equals(u.getRole().getRoleCode())).orElse(null);
        invoice.setCustomer(customer);
        invoice.setCustomerName(blankTo(customerName, customer != null ? customer.getFullName() : "Khách lẻ"));
        invoice.setCustomerPhone(blankTo(phone, customer != null ? customer.getPhone() : ""));
        invoice.setCustomerNote(note); invoice.setCouponCode(couponCode == null || couponCode.isBlank() ? null : couponCode.trim());
        items.deleteByHeldInvoice_Id(id);
        if (cart != null) for (CartEntry entry : cart) {
            if (entry.variantId() == null || entry.quantity() == null || entry.quantity() < 1 || entry.quantity() > 1000)
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            ProductVariant variant = variants.findById(entry.variantId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm."));
            if (!Boolean.TRUE.equals(variant.getActive()) || !Boolean.TRUE.equals(variant.getProduct().getActive()))
                throw new IllegalArgumentException("Sản phẩm đã ngừng kinh doanh: " + variant.getSku());
            HeldInvoiceItem item = new HeldInvoiceItem(); item.setHeldInvoice(invoice); item.setProductVariant(variant);
            item.setProductNameSnapshot(variant.getProduct().getProductName());
            item.setVariantLabelSnapshot(OrderService.variantLabel(variant)); item.setSkuSnapshot(variant.getSku());
            item.setUnitPrice(variant.getUnitPrice()); item.setQuantity(entry.quantity()); items.save(item);
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        // Items are snapshots only. No stock is reserved or deducted here.
        return invoices.save(invoice);
    }

    @Transactional
    public HeldInvoice create(AppUser staff, String customerName, String phone, String note, String couponCode, List<CartEntry> cart) {
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("POS chưa có sản phẩm để lưu hóa đơn chờ.");
        AppUser lockedStaff = users.findByIdForUpdate(staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên."));
        if (invoices.countByCreatedBy_IdAndStatus(lockedStaff.getId(),"OPEN") >= 10)
            throw new IllegalArgumentException("Mỗi nhân viên chỉ được giữ tối đa 10 hóa đơn đang mở.");
        AppUser customer = phone == null || phone.isBlank() ? null : users.findFirstByPhone(phone.trim()).filter(u -> "CUSTOMER".equals(u.getRole().getRoleCode())).orElse(null);
        HeldInvoice invoice = new HeldInvoice();
        invoice.setHeldCode("HC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + UUID.randomUUID().toString().substring(0,4).toUpperCase());
        invoice.setCreatedBy(lockedStaff); invoice.setCustomer(customer);
        invoice.setCustomerName(blankTo(customerName, customer != null ? customer.getFullName() : "Khách lẻ"));
        invoice.setCustomerPhone(blankTo(phone, customer != null ? customer.getPhone() : "")); invoice.setCustomerNote(note);
        invoice.setCouponCode(couponCode); invoice.setStatus("OPEN"); invoice.setCreatedAt(LocalDateTime.now()); invoice.setUpdatedAt(LocalDateTime.now());
        invoices.save(invoice);
        for (CartEntry entry : cart) {
            ProductVariant variant = variants.findById(entry.variantId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm."));
            if (entry.quantity() == null || entry.quantity() < 1) throw new IllegalArgumentException("Số lượng không hợp lệ.");
            HeldInvoiceItem item = new HeldInvoiceItem(); item.setHeldInvoice(invoice); item.setProductVariant(variant);
            item.setProductNameSnapshot(variant.getProduct().getProductName()); item.setVariantLabelSnapshot(OrderService.variantLabel(variant));
            item.setSkuSnapshot(variant.getSku()); item.setUnitPrice(variant.getUnitPrice()); item.setQuantity(entry.quantity()); items.save(item);
        }
        // Saving a held invoice does not reserve stock or create an order/payment.
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<HeldInvoice> listFor(AppUser actor) {
        String role = actor.getRole().getRoleCode();
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) return invoices.findByStatusOrderByCreatedAtDesc("OPEN");
        return invoices.findByCreatedBy_IdAndStatusOrderByCreatedAtDesc(actor.getId(),"OPEN");
    }

    @Transactional
    public CustomerOrder convert(Long id, AppUser actor, String paymentMethod) {
        HeldInvoice invoice = invoices.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn chờ."));
        ensureAccessible(invoice, actor);
        if (!"OPEN".equals(invoice.getStatus())) throw new IllegalArgumentException("Hóa đơn chờ này đã được xử lý.");
        List<HeldInvoiceItem> savedItems = items.findByHeldInvoice_IdOrderByIdAsc(id);
        if (savedItems.isEmpty()) throw new IllegalArgumentException("Quầy khách chưa có sản phẩm để thanh toán.");
        List<CartEntry> entries = savedItems.stream().map(i -> new CartEntry(i.getProductVariant().getId(),i.getQuantity())).toList();
        CustomerOrder order = orderService.createOrder(entries, invoice.getCustomer(), actor, "POS", paymentMethod,
                invoice.getCustomerName(), invoice.getCustomerPhone(), null, invoice.getCustomerNote(), invoice.getCouponCode());
        invoice.setStatus("CONVERTED"); invoice.setConvertedOrder(order); invoice.setUpdatedAt(LocalDateTime.now()); invoices.save(invoice);
        return order;
    }

    @Transactional
    public void cancel(Long id, AppUser actor, String reason) {
        HeldInvoice invoice = invoices.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn chờ."));
        ensureAccessible(invoice, actor);
        if (!"OPEN".equals(invoice.getStatus())) throw new IllegalArgumentException("Chỉ hủy được hóa đơn đang mở.");
        invoice.setStatus("CANCELLED");
        String oldNote = invoice.getCustomerNote() == null ? "" : invoice.getCustomerNote();
        invoice.setCustomerNote((oldNote + " | Hủy: " + blankTo(reason,"Không ghi lý do")).trim());
        invoice.setUpdatedAt(LocalDateTime.now()); invoices.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<HeldInvoiceItem> getItems(Long id, AppUser actor) { getAccessible(id,actor); return items.findByHeldInvoice_IdOrderByIdAsc(id); }

    private HeldInvoice getAccessible(Long id, AppUser actor) {
        HeldInvoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn chờ."));
        ensureAccessible(invoice, actor);
        return invoice;
    }

    private void ensureAccessible(HeldInvoice invoice, AppUser actor) {
        String role = actor.getRole().getRoleCode();
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)
                && (invoice.getCreatedBy() == null || !invoice.getCreatedBy().getId().equals(actor.getId()))) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập hóa đơn chờ này.");
        }
    }
    private String blankTo(String value,String fallback){return value==null||value.isBlank()?(fallback==null?"":fallback):value.trim();}
}
