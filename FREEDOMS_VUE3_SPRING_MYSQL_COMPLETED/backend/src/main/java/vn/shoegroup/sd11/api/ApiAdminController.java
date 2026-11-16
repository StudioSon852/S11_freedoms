package vn.shoegroup.sd11.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import vn.shoegroup.sd11.service.OrderService;
import vn.shoegroup.sd11.service.RefundService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {
    private final CatalogApiService catalog;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final SizeRepository sizes;
    private final ColorRepository colors;
    private final OrderItemRepository orderItems;
    private final CouponRepository coupons;
    private final VariantDiscountRepository discounts;
    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;
    private final HeldInvoiceRepository held;
    private final OrderService orderService;
    private final RefundRepository refundRepo;
    private final RefundService refundService;
    private final UserRepository users;
    private final RoleRepository roles;
    private final ProductVariantImageRepository variantImages;
    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads/products}")
    private String uploadDirectory;
    private final MemberTierRepository tiers;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public ApiAdminController(CatalogApiService catalog, ProductRepository products, ProductVariantRepository variants,
            CategoryRepository categories, BrandRepository brands, SizeRepository sizes, ColorRepository colors,
            OrderItemRepository orderItems, CouponRepository coupons, VariantDiscountRepository discounts,
            CustomerOrderRepository orders, PaymentRepository payments, HeldInvoiceRepository held,
            OrderService orderService, RefundRepository refundRepo, RefundService refundService,
            UserRepository users, RoleRepository roles, MemberTierRepository tiers, ProductVariantImageRepository variantImages, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.catalog=catalog; this.products=products; this.variants=variants; this.categories=categories; this.brands=brands;
        this.sizes=sizes; this.colors=colors; this.orderItems=orderItems; this.coupons=coupons; this.discounts=discounts;
        this.orders=orders; this.payments=payments; this.held=held; this.orderService=orderService;
        this.refundRepo=refundRepo; this.refundService=refundService; this.users=users; this.roles=roles; this.tiers=tiers; this.variantImages=variantImages; this.passwordEncoder=passwordEncoder;
    }

    @GetMapping("/overview")
    public Map<String,Object> overview() {
        BigDecimal revenue = Optional.ofNullable(orders.sumCompletedRevenue()).orElse(BigDecimal.ZERO);
        long pendingPayments = payments.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> "BANK_TRANSFER".equals(p.getPaymentMethod()))
                .filter(p -> List.of("PENDING", "WAITING_CONFIRMATION").contains(p.getPaymentStatus())).count();
        List<OrderView> recentOrders = orders.findAllByOrderByCreatedAtDesc().stream().limit(8).map(this::mapOrder).toList();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("products", products.count()); result.put("orders", orders.count());
        result.put("heldInvoices", held.countByStatus("OPEN")); result.put("pendingPayments", pendingPayments);
        result.put("pendingRefunds", refundRepo.findAllByOrderByRequestedAtDesc().stream().filter(r -> "PENDING".equals(r.getStatus())).count());
        result.put("revenue", revenue); result.put("recentOrders", recentOrders);
        return result;
    }

    @GetMapping("/revenue")
    public Map<String,Object> revenueReport(@RequestParam(required=false) LocalDate fromDate,
                                            @RequestParam(required=false) LocalDate toDate,
                                            @RequestParam(defaultValue="") String customer,
                                            @RequestParam(defaultValue="") String orderCode) {
        String customerQuery = customer.trim().toLowerCase(Locale.ROOT);
        String codeQuery = orderCode.trim().toLowerCase(Locale.ROOT);
        List<CustomerOrder> matched = orders.findAllByOrderByCreatedAtDesc().stream().filter(o -> {
            if (!"COMPLETED".equalsIgnoreCase(o.getOrderStatus())) return false;
            Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
            if (payment == null || !"PAID".equalsIgnoreCase(payment.getPaymentStatus())) return false;
            LocalDate revenueDate = o.getCreatedAt().toLocalDate();
            if (fromDate != null && revenueDate.isBefore(fromDate)) return false;
            if (toDate != null && revenueDate.isAfter(toDate)) return false;
            if (!codeQuery.isBlank() && !o.getOrderCode().toLowerCase(Locale.ROOT).contains(codeQuery)) return false;
            if (!customerQuery.isBlank()) {
                AppUser u = o.getCustomer();
                String blob = (Objects.toString(o.getCustomerName(), "") + " " + Objects.toString(o.getCustomerPhone(), "") + " "
                        + (u == null ? "" : Objects.toString(u.getEmail(), "") + " " + Objects.toString(u.getFullName(), ""))).toLowerCase(Locale.ROOT);
                if (!blob.contains(customerQuery)) return false;
            }
            return true;
        }).toList();
        BigDecimal total = matched.stream().map(o -> {
            Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
            BigDecimal paidAmount = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            BigDecimal actuallyRefunded = payment == null ? BigDecimal.ZERO : Optional.ofNullable(refundRepo.sumRefundedAmountByPaymentId(payment.getId())).orElse(BigDecimal.ZERO);
            return paidAmount.subtract(actuallyRefunded).max(BigDecimal.ZERO);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String,Object>> rows = matched.stream().limit(100).map(o -> {
            Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
            BigDecimal refunded = payment == null ? BigDecimal.ZERO : Optional.ofNullable(refundRepo.sumRefundedAmountByPaymentId(payment.getId())).orElse(BigDecimal.ZERO);
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id", o.getId()); row.put("code", o.getOrderCode()); row.put("customerName", o.getCustomerName());
            row.put("customerPhone", o.getCustomerPhone()); row.put("createdAt", o.getCreatedAt()); row.put("salesChannel", o.getSalesChannel());
            row.put("status", o.getOrderStatus()); row.put("paymentStatus", payment == null ? "" : payment.getPaymentStatus());
            row.put("totalAmount", (o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount()).subtract(refunded).max(BigDecimal.ZERO));
            return row;
        }).toList();
        Map<String,Object> result = new LinkedHashMap<>(); result.put("totalRevenue", total); result.put("orderCount", matched.size());
        result.put("orders", rows); result.put("truncated", matched.size() > rows.size());
        return result;
    }

    @GetMapping("/options")
    public Map<String,Object> options() {
        return Map.of(
                "categories", categories.findAll().stream().map(c -> Map.of("id", c.getId(), "name", c.getCategoryName())).toList(),
                "brands", brands.findAll().stream().map(b -> Map.of("id", b.getId(), "name", b.getBrandName())).toList(),
                "sizes", sizes.findAll().stream().map(z -> Map.of("id", z.getId(), "label", z.getSizeLabel())).toList(),
                "colors", colors.findAll().stream().map(c -> Map.of("id", c.getId(), "name", c.getColorName())).toList());
    }

    @GetMapping("/catalog/{type}")
    public List<Map<String,Object>> catalogItems(@PathVariable String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "categories" -> categories.findAll().stream().map(x -> Map.<String,Object>of("id",x.getId(),"name",x.getCategoryName(),"active",Boolean.TRUE.equals(x.getActive()))).toList();
            case "brands" -> brands.findAll().stream().map(x -> Map.<String,Object>of("id",x.getId(),"name",x.getBrandName(),"active",Boolean.TRUE.equals(x.getActive()))).toList();
            case "colors" -> colors.findAll().stream().map(x -> Map.<String,Object>of("id",x.getId(),"name",x.getColorName(),"hex",x.getColorHex()==null?"":x.getColorHex())).toList();
            case "sizes" -> sizes.findAll().stream().map(x -> Map.<String,Object>of("id",x.getId(),"name",x.getSizeLabel(),"sortOrder",x.getSortOrder())).toList();
            default -> throw new IllegalArgumentException("Loại danh mục không hợp lệ.");
        };
    }

    @PostMapping("/catalog/{type}")
    @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> createCatalogItem(@PathVariable String type, @RequestBody Map<String,Object> body) {
        String name = Objects.toString(body.get("name"), "").trim();
        if (name.isBlank()) throw new IllegalArgumentException("Tên danh mục không được để trống.");
        Long id;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "categories" -> { Category x=new Category(); x.setCategoryName(name); x.setActive(true); id=categories.save(x).getId(); }
            case "brands" -> { Brand x=new Brand(); x.setBrandName(name); x.setActive(true); id=brands.save(x).getId(); }
            case "colors" -> { Color x=new Color(); x.setColorName(name); x.setColorHex(Objects.toString(body.get("hex"), "#000000")); id=colors.save(x).getId(); }
            case "sizes" -> { Size x=new Size(); x.setSizeLabel(name); x.setSortOrder(body.get("sortOrder") instanceof Number n?n.intValue():0); id=sizes.save(x).getId(); }
            default -> throw new IllegalArgumentException("Loại danh mục không hợp lệ.");
        }
        return Map.of("id",id,"name",name);
    }

    @PatchMapping("/catalog/{type}/{id}")
    @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> updateCatalogItem(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String,Object> body) {
        String name = Objects.toString(body.get("name"), "").trim();
        switch (type.toLowerCase(Locale.ROOT)) {
            case "categories" -> { Category x=categories.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy danh mục.")); if(!name.isBlank())x.setCategoryName(name); if(body.containsKey("active"))x.setActive(Boolean.valueOf(body.get("active").toString())); categories.save(x); }
            case "brands" -> { Brand x=brands.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy thương hiệu.")); if(!name.isBlank())x.setBrandName(name); if(body.containsKey("active"))x.setActive(Boolean.valueOf(body.get("active").toString())); brands.save(x); }
            case "colors" -> { Color x=colors.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy màu sắc.")); if(!name.isBlank())x.setColorName(name); if(body.containsKey("hex"))x.setColorHex(Objects.toString(body.get("hex"), "")); colors.save(x); }
            case "sizes" -> { Size x=sizes.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy kích cỡ.")); if(!name.isBlank())x.setSizeLabel(name); if(body.get("sortOrder") instanceof Number n)x.setSortOrder(n.intValue()); sizes.save(x); }
            default -> throw new IllegalArgumentException("Loại danh mục không hợp lệ.");
        }
        return Map.of("success",true);
    }

    @DeleteMapping("/catalog/{type}/{id}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView deleteCatalogItem(@PathVariable String type, @PathVariable Long id) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "categories" -> { if(products.existsByCategory_Id(id)) throw new IllegalArgumentException("Danh mục đang được sản phẩm sử dụng; hãy đổi danh mục sản phẩm trước."); categories.delete(categories.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy danh mục."))); }
            case "brands" -> { if(products.existsByBrand_Id(id)) throw new IllegalArgumentException("Thương hiệu đang được sản phẩm sử dụng; hãy đổi thương hiệu sản phẩm trước."); brands.delete(brands.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy thương hiệu."))); }
            case "colors" -> { if(variants.existsByColor_Id(id)) throw new IllegalArgumentException("Màu sắc đang được biến thể sử dụng; hãy đổi màu biến thể trước."); colors.delete(colors.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy màu sắc."))); }
            case "sizes" -> { if(variants.existsBySize_Id(id)) throw new IllegalArgumentException("Kích cỡ đang được biến thể sử dụng; hãy đổi size biến thể trước."); sizes.delete(sizes.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy kích cỡ."))); }
            default -> throw new IllegalArgumentException("Loại danh mục không hợp lệ.");
        }
        return new MessageView(true,"Đã xóa mục danh mục.");
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(defaultValue = "") String q) { return catalog.products(q, "OFFLINE", false, true); }

    @PostMapping("/products")
    @org.springframework.transaction.annotation.Transactional
    public ProductView createProduct(@RequestBody ProductCreateRequest request) {
        if (request.productName() == null || request.productName().isBlank() || request.sku() == null || request.sku().isBlank())
            throw new IllegalArgumentException("Tên sản phẩm và SKU biến thể là bắt buộc.");
        if (variants.existsBySkuIgnoreCase(request.sku().trim()))
            throw new IllegalArgumentException("SKU biến thể đã tồn tại.");
        if (request.unitPrice() == null || request.unitPrice().signum() <= 0 || request.stockQuantity() == null || request.stockQuantity() < 0)
            throw new IllegalArgumentException("Giá và tồn kho không hợp lệ.");
        Product product = new Product();
        product.setProductName(request.productName().trim()); product.setParentSku(request.parentSku());
        product.setDescription(request.description()); product.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? "/img/picture1.png" : request.imageUrl());
        product.setCategory(request.categoryId() == null ? null : categories.findById(request.categoryId()).orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại.")));
        product.setBrand(request.brandId() == null ? null : brands.findById(request.brandId()).orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại.")));
        product.setFeatured(Boolean.TRUE.equals(request.featured())); product.setActive(true); product.setCreatedAt(LocalDateTime.now()); products.save(product);
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setSku(request.sku().trim());
        variant.setSize(request.sizeId() == null ? null : sizes.findById(request.sizeId()).orElseThrow(() -> new IllegalArgumentException("Size không tồn tại.")));
        variant.setColor(request.colorId() == null ? null : colors.findById(request.colorId()).orElseThrow(() -> new IllegalArgumentException("Màu sắc không tồn tại.")));
        variant.setUnitPrice(request.unitPrice()); variant.setStockQuantity(request.stockQuantity()); variant.setActive(true); variants.save(variant);
        return catalog.product(product.getId(), "OFFLINE", true);
    }

    @PatchMapping("/products/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ProductView updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        Product p = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (request.productName() != null && !request.productName().isBlank()) p.setProductName(request.productName().trim());
        if (request.description() != null) p.setDescription(request.description());
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) p.setImageUrl(request.imageUrl());
        if (request.categoryId() != null) p.setCategory(categories.findById(request.categoryId()).orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại.")));
        if (request.brandId() != null) p.setBrand(brands.findById(request.brandId()).orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại.")));
        if (request.active() != null) p.setActive(request.active());
        if (request.featured() != null) p.setFeatured(request.featured());
        products.save(p);
        if (!Boolean.TRUE.equals(p.getActive())) variants.findByProduct_IdOrderByIdAsc(id).forEach(v -> { v.setActive(false); variants.save(v); });
        return catalog.product(id, "OFFLINE", true);
    }

    @DeleteMapping("/products/{id}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView disableProduct(@PathVariable Long id) {
        Product p = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        boolean wasSold = orderItems.existsByProductVariant_Product_Id(id);
        p.setActive(false); products.save(p);
        variants.findByProduct_IdOrderByIdAsc(id).forEach(v -> { v.setActive(false); variants.save(v); });
        return new MessageView(true, wasSold
                ? "Sản phẩm đã phát sinh đơn hàng nên không xóa vật lý để bảo toàn lịch sử; đã chuyển sang ngừng kinh doanh."
                : "Đã ngừng kinh doanh sản phẩm và các biến thể.");
    }

    @PostMapping("/products/{id}/variants")
    @org.springframework.transaction.annotation.Transactional
    public VariantView createVariant(@PathVariable Long id, @RequestBody VariantCreateRequest request) {
        Product product = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (request.sku() == null || request.sku().isBlank()) throw new IllegalArgumentException("SKU biến thể là bắt buộc.");
        if (variants.existsBySkuIgnoreCase(request.sku().trim())) throw new IllegalArgumentException("SKU biến thể đã tồn tại.");
        if (request.unitPrice() == null || request.unitPrice().signum() <= 0 || request.stockQuantity() == null || request.stockQuantity() < 0)
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0 và tồn kho không được âm.");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product); variant.setSku(request.sku().trim());
        variant.setSize(request.sizeId() == null ? null : sizes.findById(request.sizeId()).orElseThrow(() -> new IllegalArgumentException("Size không tồn tại.")));
        variant.setColor(request.colorId() == null ? null : colors.findById(request.colorId()).orElseThrow(() -> new IllegalArgumentException("Màu sắc không tồn tại.")));
        variant.setUnitPrice(request.unitPrice()); variant.setStockQuantity(request.stockQuantity()); variant.setActive(true);
        variants.save(variant);
        return new VariantView(variant.getId(), variant.getSku(), variant.getSize() == null ? "" : variant.getSize().getSizeLabel(),
                variant.getColor() == null ? "" : variant.getColor().getColorName(), variant.getSize() == null ? null : variant.getSize().getId(), variant.getColor() == null ? null : variant.getColor().getId(), variant.getUnitPrice(), BigDecimal.ZERO,
                variant.getUnitPrice(), false, variant.getStockQuantity(), true, List.of());
    }

    @PatchMapping("/variants/{id}")
    @org.springframework.transaction.annotation.Transactional
    public VariantView updateVariant(@PathVariable Long id, @RequestBody VariantUpdateRequest request) {
        ProductVariant v = variants.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));
        if (request.sku() != null && !request.sku().isBlank()) {
            String sku = request.sku().trim();
            if (!sku.equalsIgnoreCase(v.getSku()) && variants.existsBySkuIgnoreCase(sku)) throw new IllegalArgumentException("SKU biến thể đã tồn tại.");
            v.setSku(sku);
        }
        if (request.sizeId() != null) v.setSize(sizes.findById(request.sizeId()).orElseThrow(() -> new IllegalArgumentException("Size không tồn tại.")));
        if (request.colorId() != null) v.setColor(colors.findById(request.colorId()).orElseThrow(() -> new IllegalArgumentException("Màu sắc không tồn tại.")));
        if (request.unitPrice() != null) {
            if (request.unitPrice().signum() <= 0) throw new IllegalArgumentException("Giá bán phải lớn hơn 0.");
            v.setUnitPrice(request.unitPrice());
        }
        if (request.stockQuantity() != null) {
            if (request.stockQuantity() < 0) throw new IllegalArgumentException("Tồn kho không thể âm.");
            v.setStockQuantity(request.stockQuantity());
        }
        if (request.active() != null) v.setActive(request.active());
        variants.save(v);
        BigDecimal discount = BigDecimal.ZERO;
        return new VariantView(v.getId(), v.getSku(), v.getSize() == null ? "" : v.getSize().getSizeLabel(),
                v.getColor() == null ? "" : v.getColor().getColorName(), v.getSize() == null ? null : v.getSize().getId(), v.getColor() == null ? null : v.getColor().getId(), v.getUnitPrice(), discount,
                v.getUnitPrice(), false, v.getStockQuantity(), Boolean.TRUE.equals(v.getActive()), variantImages.findByVariant_IdOrderBySortOrderAscIdAsc(v.getId()).stream().map(i -> new VariantImageView(i.getId(), i.getImageUrl())).toList());
    }

    @PostMapping(value = "/variants/{variantId}/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.transaction.annotation.Transactional
    public List<VariantImageView> uploadVariantImage(@PathVariable Long variantId, @RequestPart("file") MultipartFile file) {
        ProductVariant variant = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));
        String url = storeImage(file);
        ProductVariantImage image = new ProductVariantImage();
        image.setVariant(variant); image.setImageUrl(url);
        image.setSortOrder(variantImages.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).size());
        variantImages.save(image);
        return variantImages.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).stream().map(i -> new VariantImageView(i.getId(), i.getImageUrl())).toList();
    }

    @DeleteMapping("/variants/{variantId}/images/{imageId}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView deleteVariantImage(@PathVariable Long variantId, @PathVariable Long imageId) {
        ProductVariantImage image = variantImages.findByIdAndVariant_Id(imageId, variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh của biến thể."));
        String imageUrl = image.getImageUrl();
        variantImages.delete(image);
        if (!variantImages.existsByImageUrlAndIdNot(imageUrl, imageId) && !products.existsByImageUrl(imageUrl)) {
            deleteStoredImage(imageUrl);
        }
        return new MessageView(true, "Đã xóa ảnh khỏi biến thể.");
    }

    @PostMapping("/variants/{variantId}/images/url")
    @org.springframework.transaction.annotation.Transactional
    public List<VariantImageView> addVariantImageUrl(@PathVariable Long variantId, @RequestBody Map<String,String> body) {
        ProductVariant variant = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));
        String url = body == null ? null : body.get("imageUrl");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/uploads/") || url.startsWith("/img/")))
            throw new IllegalArgumentException("Địa chỉ ảnh phải là URL http(s) hoặc đường dẫn ảnh nội bộ.");
        ProductVariantImage image = new ProductVariantImage(); image.setVariant(variant); image.setImageUrl(url.trim());
        image.setSortOrder(variantImages.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).size()); variantImages.save(image);
        return variantImages.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).stream().map(i -> new VariantImageView(i.getId(),i.getImageUrl())).toList();
    }

    @DeleteMapping("/variants/{id}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView disableVariant(@PathVariable Long id) {
        ProductVariant v = variants.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));
        v.setActive(false); variants.save(v);
        return new MessageView(true, "Đã dừng kinh doanh biến thể; lịch sử đơn hàng được giữ nguyên.");
    }

    @PostMapping(value = "/products/{productId}/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.transaction.annotation.Transactional
    public Map<String,String> uploadProductImage(@PathVariable Long productId, @RequestPart("file") MultipartFile file) {
        Product product = products.findById(productId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        String url = storeImage(file); product.setImageUrl(url); products.save(product);
        return Map.of("imageUrl", url);
    }

    private String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh cần tải lên.");
        if (file.getSize() > 8L * 1024 * 1024) throw new IllegalArgumentException("Mỗi ảnh tối đa 8 MB.");

        // Snipping Tool / Windows screenshots may be reported as application/octet-stream.
        // Detect the actual image format from its signature instead of trusting Content-Type.
        String extension;
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean png = header.length >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E
                    && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A
                    && header[6] == 0x1A && header[7] == 0x0A;
            boolean jpeg = header.length >= 3 && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            boolean gif = header.length >= 6 && header[0] == 'G' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
            boolean webp = header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                    && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            boolean bmp = header.length >= 2 && header[0] == 'B' && header[1] == 'M';

            extension = png ? ".png" : jpeg ? ".jpg" : gif ? ".gif"
                    : webp ? ".webp" : bmp ? ".bmp" : null;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không đọc được tệp ảnh đã chọn.");
        }
        if (extension == null) {
            throw new IllegalArgumentException("Tệp không phải ảnh hợp lệ. Hãy dùng JPG, PNG, WEBP, GIF hoặc BMP.");
        }

        try {
            Path root = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String name = UUID.randomUUID() + extension;
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("Tên ảnh không hợp lệ.");
            file.transferTo(target);
            return "/uploads/products/" + name;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể lưu ảnh: " + ex.getMessage());
        }
    }

    private void deleteStoredImage(String url) {
        if (url == null || !url.startsWith("/uploads/products/")) return;
        try { Path root = Paths.get(uploadDirectory).toAbsolutePath().normalize(); Path file = root.resolve(url.substring("/uploads/products/".length())).normalize(); if (file.startsWith(root)) Files.deleteIfExists(file); }
        catch (IOException ignored) { /* DB reference is removed; orphan cleanup can be done later. */ }
    }

    @GetMapping("/coupons")
    public List<CouponView> listCoupons(@RequestParam(required = false) String channel, @RequestParam(defaultValue = "") String q) {
        String query = q.trim().toLowerCase(Locale.ROOT);
        return coupons.findAllByOrderByIdDesc().stream().filter(c -> channel == null || "BOTH".equalsIgnoreCase(c.getSalesChannel()) || channel.equalsIgnoreCase(c.getSalesChannel()))
                .filter(c -> query.isBlank() || c.getCouponCode().toLowerCase(Locale.ROOT).contains(query))
                .map(catalog::mapCoupon).toList();
    }

    @PostMapping("/coupons")
    @org.springframework.transaction.annotation.Transactional
    public CouponView createCoupon(@RequestBody CouponSaveRequest request) {
        String channel = normalizeChannel(request.salesChannel());
        String type = normalizeDiscountType(request.discountType());
        if (request.couponCode() == null || request.couponCode().isBlank() || request.discountValue() == null || request.discountValue().signum() <= 0)
            throw new IllegalArgumentException("Mã và giá trị giảm là bắt buộc.");
        if (coupons.findByCouponCodeIgnoreCase(request.couponCode().trim()).isPresent()) throw new IllegalArgumentException("Mã giảm giá đã tồn tại.");
        validateDateRange(request.startsAt(), request.endsAt());
        Coupon c = new Coupon(); c.setCouponCode(request.couponCode().trim().toUpperCase(Locale.ROOT)); c.setSalesChannel(channel);
        c.setDiscountType(type); c.setDiscountValue(request.discountValue()); c.setMinOrderAmount(request.minOrderAmount() == null ? BigDecimal.ZERO : request.minOrderAmount());
        c.setMaxDiscount(request.maxDiscount()); c.setUsageLimit(request.usageLimit()); c.setUsageCount(0);
        c.setStartsAt(request.startsAt()); c.setEndsAt(request.endsAt()); c.setActive(request.active() == null || request.active());
        return catalog.mapCoupon(coupons.save(c));
    }

    @PatchMapping("/coupons/{id}")
    @org.springframework.transaction.annotation.Transactional
    public CouponView updateCoupon(@PathVariable Long id, @RequestBody CouponSaveRequest request) {
        Coupon c = coupons.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá."));
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            String code = request.couponCode().trim().toUpperCase(Locale.ROOT);
            coupons.findByCouponCodeIgnoreCase(code).filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new IllegalArgumentException("Mã giảm giá đã tồn tại."); });
            c.setCouponCode(code);
        }
        if (request.salesChannel() != null) c.setSalesChannel(normalizeChannel(request.salesChannel()));
        if (request.discountType() != null) c.setDiscountType(normalizeDiscountType(request.discountType()));
        if (request.discountValue() != null) { if (request.discountValue().signum() <= 0) throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0."); c.setDiscountValue(request.discountValue()); }
        if (request.minOrderAmount() != null) c.setMinOrderAmount(request.minOrderAmount());
        c.setMaxDiscount(request.maxDiscount());
        c.setUsageLimit(request.usageLimit());
        c.setStartsAt(request.startsAt()); c.setEndsAt(request.endsAt());
        if (request.active() != null) c.setActive(request.active());
        validateDateRange(c.getStartsAt(), c.getEndsAt());
        return catalog.mapCoupon(coupons.save(c));
    }

    @DeleteMapping("/coupons/{id}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView deleteCoupon(@PathVariable Long id) {
        Coupon c = coupons.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá."));
        if (c.getUsageCount() != null && c.getUsageCount() > 0) {
            throw new IllegalArgumentException("Mã đã được sử dụng nên không thể xóa để bảo toàn báo cáo; hãy chuyển trạng thái sang Dừng.");
        }
        coupons.delete(c);
        return new MessageView(true, "Đã xóa mã giảm giá chưa phát sinh lượt sử dụng.");
    }

    @GetMapping("/variant-discounts")
    public List<Map<String,Object>> listVariantDiscounts(@RequestParam(required = false) String channel, @RequestParam(defaultValue = "") String q) {
        String query = q.trim().toLowerCase(Locale.ROOT);
        return discounts.findAllByOrderByIdDesc().stream()
                .filter(d -> channel == null || "BOTH".equalsIgnoreCase(d.getSalesChannel()) || channel.equalsIgnoreCase(d.getSalesChannel()))
                .filter(d -> query.isBlank() || d.getProductVariant().getSku().toLowerCase(Locale.ROOT).contains(query) || d.getProductVariant().getProduct().getProductName().toLowerCase(Locale.ROOT).contains(query))
                .map(this::mapDiscount).toList();
    }

    @PostMapping("/variant-discounts")
    @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> createVariantDiscount(@RequestBody VariantDiscountSaveRequest request) {
        if (request.variantId() == null || request.discountValue() == null || request.discountValue().signum() <= 0)
            throw new IllegalArgumentException("Chọn biến thể và nhập giá trị giảm hợp lệ.");
        VariantDiscount d = new VariantDiscount(); d.setProductVariant(variants.findById(request.variantId()).orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại.")));
        d.setSalesChannel(normalizeChannel(request.salesChannel())); d.setDiscountType(normalizeDiscountType(request.discountType()));
        d.setDiscountValue(request.discountValue()); d.setStartsAt(request.startsAt()); d.setEndsAt(request.endsAt()); d.setReason(request.reason());
        d.setActive(request.active() == null || request.active()); validateDateRange(d.getStartsAt(), d.getEndsAt());
        return mapDiscount(discounts.save(d));
    }

    @PatchMapping("/variant-discounts/{id}")
    @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> updateVariantDiscount(@PathVariable Long id, @RequestBody VariantDiscountSaveRequest request) {
        VariantDiscount d = discounts.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảm giá biến thể."));
        if (request.variantId() != null) d.setProductVariant(variants.findById(request.variantId()).orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại.")));
        if (request.salesChannel() != null) d.setSalesChannel(normalizeChannel(request.salesChannel()));
        if (request.discountType() != null) d.setDiscountType(normalizeDiscountType(request.discountType()));
        if (request.discountValue() != null) { if (request.discountValue().signum() <= 0) throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0."); d.setDiscountValue(request.discountValue()); }
        d.setStartsAt(request.startsAt()); d.setEndsAt(request.endsAt());
        if (request.reason() != null) d.setReason(request.reason());
        if (request.active() != null) d.setActive(request.active());
        validateDateRange(d.getStartsAt(), d.getEndsAt());
        return mapDiscount(discounts.save(d));
    }

    @DeleteMapping("/variant-discounts/{id}")
    @org.springframework.transaction.annotation.Transactional
    public MessageView deleteVariantDiscount(@PathVariable Long id) {
        VariantDiscount d = discounts.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảm giá biến thể."));
        discounts.delete(d);
        return new MessageView(true, "Đã xóa ưu đãi biến thể.");
    }

    @GetMapping("/customers")
    public List<CustomerSummaryView> customers(@RequestParam(defaultValue="") String q,
                                               @RequestParam(defaultValue="") String tier) {
        return filterCustomers(q, tier);
    }

    @GetMapping("/customers/{id}")
    public CustomerDetailView customerDetail(@PathVariable Long id) {
        AppUser customer = users.findById(id).filter(u -> "CUSTOMER".equals(u.getRole().getRoleCode()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        List<CustomerOrder> customerOrders = orders.findByCustomer_IdOrderByCreatedAtDesc(id);
        return new CustomerDetailView(mapCustomer(customer), customerOrders.size(), customerOrders.stream().limit(10).map(this::mapOrder).toList());
    }

    private List<CustomerSummaryView> filterCustomers(String q, String tier) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String tierQuery = tier == null ? "" : tier.trim().toUpperCase(Locale.ROOT);
        return users.findByRole_RoleCodeOrderByIdDesc("CUSTOMER").stream().filter(u -> {
            if (!tierQuery.isBlank() && (u.getTier() == null || !tierQuery.equalsIgnoreCase(u.getTier().getTierCode()))) return false;
            if (query.isBlank()) return true;
            String blob = (Objects.toString(u.getFullName(), "") + " " + Objects.toString(u.getEmail(), "") + " "
                    + Objects.toString(u.getPhone(), "") + " " + Objects.toString(u.getDefaultAddress(), "") + " "
                    + (u.getTier() == null ? "" : u.getTier().getTierName())).toLowerCase(Locale.ROOT);
            return blob.contains(query);
        }).map(this::mapCustomer).toList();
    }

    private CustomerSummaryView mapCustomer(AppUser u) {
        return new CustomerSummaryView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getTier() == null ? "NEW" : u.getTier().getTierCode(), u.getTier() == null ? "Mới" : u.getTier().getTierName(),
                u.getTotalSpent() == null ? BigDecimal.ZERO : u.getTotalSpent(),
                u.getTier() == null ? BigDecimal.ZERO : u.getTier().getDiscountPercent(),
                Objects.toString(u.getDefaultAddress(), ""), u.getCreatedAt());
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return users.findAll().stream().sorted(Comparator.comparing(AppUser::getId).reversed())
                .map(u -> new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                        u.getRole().getRoleCode(), Boolean.TRUE.equals(u.getActive()))).toList();
    }

    @PostMapping("/users")
    @org.springframework.transaction.annotation.Transactional
    public UserView createStaff(@RequestBody UserCreateRequest request,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        if (request.email() == null || request.email().isBlank() || !request.email().contains("@"))
            throw new IllegalArgumentException("Email nhân viên không hợp lệ.");
        if (request.password() == null || request.password().length() < 8 || request.password().length() > 72)
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        if (request.fullName() == null || request.fullName().isBlank()) throw new IllegalArgumentException("Vui lòng nhập họ tên.");
        if (users.existsByEmailIgnoreCase(request.email().trim())) throw new IllegalArgumentException("Email đã tồn tại.");
        String code = request.roleCode() == null ? "EMPLOYEE" : request.roleCode().toUpperCase(Locale.ROOT);
        if (!List.of("ADMIN", "MANAGER", "EMPLOYEE").contains(code)) throw new IllegalArgumentException("Chỉ được tạo tài khoản nhân sự Admin, Manager hoặc Employee.");
        if ("ADMIN".equals(code) && !"ADMIN".equals(principal.getUser().getRole().getRoleCode()))
            throw new IllegalArgumentException("Chỉ Admin hiện tại mới được tạo tài khoản Admin mới.");
        Role role = roles.findByRoleCode(code).orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
        AppUser user = new AppUser(); user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.password())); user.setFullName(request.fullName().trim());
        user.setPhone(request.phone()); user.setRole(role); user.setTier(tiers.findByTierCode("NEW").orElse(null)); user.setActive(true); user.setTotalSpent(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now()); users.save(user);
        return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), code, true);
    }

    @PatchMapping("/users/{id}/role")
    @org.springframework.transaction.annotation.Transactional
    public UserView setUserRole(@PathVariable Long id, @RequestBody Map<String,String> body,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        AppUser user = users.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        String code = body == null || body.get("roleCode") == null ? "" : body.get("roleCode").trim().toUpperCase(Locale.ROOT);
        if (!List.of("ADMIN", "MANAGER", "EMPLOYEE").contains(code)) throw new IllegalArgumentException("Vai trò mới không hợp lệ.");
        if ("ADMIN".equals(code) && !"ADMIN".equals(principal.getUser().getRole().getRoleCode()))
            throw new IllegalArgumentException("Chỉ Admin hiện tại mới được cấp quyền Admin.");
        String oldCode = user.getRole().getRoleCode();
        if ("CUSTOMER".equals(oldCode))
            throw new IllegalArgumentException("Không chuyển tài khoản khách hàng thành tài khoản nhân sự. Hãy tạo tài khoản nhân sự riêng.");
        if (user.getId().equals(principal.getUser().getId()) && !oldCode.equals(code))
            throw new IllegalArgumentException("Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
        if ("ADMIN".equals(oldCode) && !"ADMIN".equals(code)) {
            if (!"ADMIN".equals(principal.getUser().getRole().getRoleCode()))
                throw new IllegalArgumentException("Chỉ Admin mới được thay đổi vai trò của một tài khoản Admin.");
            long admins = users.findAll().stream().filter(u -> "ADMIN".equals(u.getRole().getRoleCode()) && Boolean.TRUE.equals(u.getActive())).count();
            if (admins <= 1) throw new IllegalArgumentException("Không thể hạ quyền Admin cuối cùng của hệ thống.");
        }
        user.setRole(roles.findByRoleCode(code).orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại.")));
        users.save(user);
        return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), code, Boolean.TRUE.equals(user.getActive()));
    }

    @PatchMapping("/users/{id}/active")
    @org.springframework.transaction.annotation.Transactional
    public MessageView setUserActive(@PathVariable Long id, @RequestBody Map<String,Boolean> body,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        AppUser user = users.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        boolean active = Boolean.TRUE.equals(body.get("active"));
        if (!active && user.getId().equals(principal.getUser().getId())) throw new IllegalArgumentException("Không thể tự khóa tài khoản đang đăng nhập.");
        if ("ADMIN".equals(user.getRole().getRoleCode()) && !"ADMIN".equals(principal.getUser().getRole().getRoleCode()))
            throw new IllegalArgumentException("Chỉ Admin mới được khóa hoặc mở khóa tài khoản Admin.");
        if (!active && "ADMIN".equals(user.getRole().getRoleCode())) {
            long activeAdmins = users.findAll().stream().filter(u -> "ADMIN".equals(u.getRole().getRoleCode()) && Boolean.TRUE.equals(u.getActive())).count();
            if (activeAdmins <= 1) throw new IllegalArgumentException("Không thể khóa Admin cuối cùng của hệ thống.");
        }
        user.setActive(active); users.save(user);
        return new MessageView(true, active ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
    }

    @GetMapping("/orders")
    public List<OrderView> listOrders(@RequestParam(required=false) LocalDate date,
                                      @RequestParam(defaultValue="") String orderCode,
                                      @RequestParam(defaultValue="") String status) {
        String code = orderCode.trim().toLowerCase(Locale.ROOT);
        String state = status.trim().toUpperCase(Locale.ROOT);
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> date == null || (o.getCreatedAt() != null && date.equals(o.getCreatedAt().toLocalDate())))
                .filter(o -> code.isBlank() || o.getOrderCode().toLowerCase(Locale.ROOT).contains(code))
                .filter(o -> state.isBlank() || state.equalsIgnoreCase(o.getOrderStatus()) || payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).map(Payment::getPaymentStatus).filter(state::equalsIgnoreCase).isPresent())
                .map(this::mapOrder).toList();
    }

    @PostMapping("/orders/{id}/status")
    public MessageView changeStatus(@PathVariable Long id, @RequestBody Map<String,String> body,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        orderService.changeOrderStatus(id, body.get("status"), principal.getUser(), body.get("note"));
        return new MessageView(true, "Đã cập nhật trạng thái đơn hàng.");
    }

    @PostMapping("/payments/{id}/confirm")
    public MessageView confirmPayment(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        orderService.confirmPayment(id, principal.getUser(), body == null ? null : body.get("reference"));
        return new MessageView(true, "Đã xác nhận thanh toán. Đơn online tự chuyển sang CONFIRMED; đơn POS tự chuyển COMPLETED.");
    }

    @PostMapping("/orders/{id}/cancel-no-payment")
    public MessageView cancelAfterNoFunds(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        orderService.cancelAfterNoFundsVerified(id, principal.getUser(), body == null ? null : body.get("note"));
        return new MessageView(true, "Đã đối soát không nhận tiền, hủy đơn và hoàn lại tồn kho.");
    }

    @PostMapping("/orders/{id}/confirm-payment")
    public MessageView confirmOrderPayment(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản thanh toán."));
        orderService.confirmPayment(payment.getId(), principal.getUser(), body == null ? null : body.get("reference"));
        return new MessageView(true, "Đã xác nhận thanh toán. Đơn online tự chuyển CONFIRMED; đơn POS tự chuyển COMPLETED.");
    }

    @PostMapping("/orders/{id}/refund-request")
    public RefundView requestRefundForOrder(@PathVariable Long id, @RequestBody RefundRequest request,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Refund refund = orderService.requestRefund(id, principal.getUser(), request == null ? null : request.amount(),
                request != null && Boolean.TRUE.equals(request.fullRefund()), request == null ? null : request.reason());
        return new RefundView(refund.getId(), refund.getOrder().getId(), refund.getOrder().getOrderCode(),
                refund.getOrder().getCustomerName(), refund.getOrder().getCustomerPhone(), refund.getAmount(),
                refund.getRefundMethod(), refund.getStatus(), refund.getReason(), refund.getTransactionReference(), refund.getRequestedAt());
    }

    @GetMapping("/refunds")
    public List<RefundView> refunds() {
        return refundService.list().stream().map(r -> new RefundView(r.getId(), r.getOrder().getId(), r.getOrder().getOrderCode(),
                r.getOrder().getCustomerName(), r.getOrder().getCustomerPhone(), r.getAmount(), r.getRefundMethod(),
                r.getStatus(), r.getReason(), r.getTransactionReference(), r.getRequestedAt())).toList();
    }

    @PostMapping("/refunds/{id}/reject")
    public RefundView rejectRefund(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        Refund refund = refundService.reject(id, principal.getUser(), body == null ? null : body.get("note"));
        return new RefundView(refund.getId(), refund.getOrder().getId(), refund.getOrder().getOrderCode(),
                refund.getOrder().getCustomerName(), refund.getOrder().getCustomerPhone(), refund.getAmount(),
                refund.getRefundMethod(), refund.getStatus(), refund.getReason(), refund.getTransactionReference(), refund.getRequestedAt());
    }

    @PostMapping("/refunds/{id}/complete")
    public RefundView completeRefund(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        Refund refund = refundService.complete(id, principal.getUser(), body == null ? null : body.get("reference"), body == null ? null : body.get("note"));
        return new RefundView(refund.getId(), refund.getOrder().getId(), refund.getOrder().getOrderCode(),
                refund.getOrder().getCustomerName(), refund.getOrder().getCustomerPhone(), refund.getAmount(),
                refund.getRefundMethod(), refund.getStatus(), refund.getReason(), refund.getTransactionReference(), refund.getRequestedAt());
    }

    private Map<String,Object> mapDiscount(VariantDiscount d) {
        // LinkedHashMap không bị giới hạn 10 cặp key/value như Map.of(...).
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", d.getId());
        result.put("variantId", d.getProductVariant().getId());
        result.put("productName", d.getProductVariant().getProduct().getProductName());
        result.put("sku", d.getProductVariant().getSku());
        result.put("channel", d.getSalesChannel());
        result.put("type", d.getDiscountType());
        result.put("value", d.getDiscountValue());
        result.put("reason", d.getReason() == null ? "" : d.getReason());
        result.put("active", Boolean.TRUE.equals(d.getActive()));
        result.put("startsAt", d.getStartsAt() == null ? "" : d.getStartsAt().toString());
        result.put("endsAt", d.getEndsAt() == null ? "" : d.getEndsAt().toString());
        return result;
    }

    private OrderView mapOrder(CustomerOrder o) {
        Payment p = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
        List<OrderItemView> items = orderService.getItems(o.getId()).stream().map(i -> new OrderItemView(
                i.getProductNameSnapshot(), i.getVariantLabelSnapshot(), i.getSkuSnapshot(), i.getUnitPrice(),
                i.getQuantity(), i.getLineTotal())).toList();
        return new OrderView(o.getId(), o.getOrderCode(), o.getSalesChannel(), o.getOrderStatus(), o.getCustomerName(),
                o.getCustomerPhone(), o.getShippingAddress(), o.getSubtotal(), o.getVariantDiscountTotal(), o.getMemberDiscountTotal(),
                o.getCouponDiscountTotal(), o.getShippingFee(), o.getTotalAmount(), p == null ? "" : p.getPaymentMethod(),
                p == null ? "" : p.getPaymentStatus(), o.getCreatedAt(), items, refundService.eligibility(o, p));
    }
    private String normalizeChannel(String c) {
        String v = c == null ? "BOTH" : c.toUpperCase(Locale.ROOT);
        if (!List.of("ONLINE", "OFFLINE", "BOTH").contains(v)) throw new IllegalArgumentException("Kênh giảm giá phải là ONLINE, OFFLINE hoặc BOTH.");
        return v;
    }
    private String normalizeDiscountType(String t) {
        String v = t == null ? "PERCENT" : t.toUpperCase(Locale.ROOT);
        if (!List.of("PERCENT", "AMOUNT").contains(v)) throw new IllegalArgumentException("Loại giảm phải là PERCENT hoặc AMOUNT.");
        if ("PERCENT".equals(v)) return v; return v;
    }
    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
    }
}
