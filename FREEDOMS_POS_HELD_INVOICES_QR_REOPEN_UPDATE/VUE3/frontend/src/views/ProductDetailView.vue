<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const product = ref(null)
const loading = ref(true)
const error = ref('')
const selectedVariantId = ref(null)
const quantity = ref(1)
const activeImage = ref('')
const money = value => `${Number(value || 0).toLocaleString('vi-VN')}₫`
const variants = computed(() => (product.value?.variants || []).filter(v => v.active !== false))
const selectedVariant = computed(() => variants.value.find(v => Number(v.id) === Number(selectedVariantId.value)) || null)
const availableVariants = computed(() => variants.value.filter(v => Number(v.stockQuantity) > 0))
const galleryImages = computed(() => {
  const urls = [product.value?.imageUrl, ...(selectedVariant.value?.images || []).map(image => image.imageUrl)]
  return [...new Set(urls.filter(Boolean))]
})
const minPrice = computed(() => {
  const prices = availableVariants.value.map(v => Number(v.hasSale ? v.salePrice : v.unitPrice)).filter(v => v > 0)
  return prices.length ? Math.min(...prices) : 0
})

async function load() {
  loading.value = true
  error.value = ''
  product.value = null
  selectedVariantId.value = null
  quantity.value = 1
  try {
    const { data } = await api.get(`/api/store/products/${route.params.id}`, { params: { channel: 'ONLINE' } })
    product.value = data
    const first = (data.variants || []).find(v => Number(v.stockQuantity) > 0) || data.variants?.[0]
    selectedVariantId.value = first?.id ?? null
    activeImage.value = [data.imageUrl, ...(first?.images || []).map(image => image.imageUrl)].filter(Boolean)[0] || '/img/picture1.png'
  } catch (e) { error.value = errorMessage(e, 'Không tải được chi tiết sản phẩm.') }
  finally { loading.value = false }
}
function addToBag(buyNow = false) {
  if (!product.value || !selectedVariant.value) return
  if (Number(selectedVariant.value.stockQuantity) < quantity.value) {
    error.value = `Chỉ còn ${selectedVariant.value.stockQuantity} sản phẩm ở phân loại đã chọn.`
    return
  }
  cart.add(product.value, selectedVariant.value, quantity.value)
  if (buyNow) {
    cart.checkoutIds = [selectedVariant.value.id]
    router.push('/checkout')
  } else {
    cart.openDrawer()
  }
}
function changeQuantity(amount) {
  const max = Math.max(1, Number(selectedVariant.value?.stockQuantity || 1))
  quantity.value = Math.max(1, Math.min(max, quantity.value + amount))
}
watch(() => route.params.id, load)
watch(selectedVariantId, () => { activeImage.value = galleryImages.value[0] || product.value?.imageUrl || '/img/picture1.png' })
onMounted(load)
</script>

<template>
  <div class="product-detail-page">
    <div class="detail-container">
      <nav class="detail-breadcrumb"><RouterLink to="/store">Trang chủ</RouterLink><span>/</span><RouterLink to="/store">Sản phẩm</RouterLink><span>/</span><b>{{ product?.name || 'Chi tiết sản phẩm' }}</b></nav>
      <div v-if="loading" class="detail-state">Đang tải thông tin sản phẩm…</div>
      <div v-else-if="error && !product" class="detail-state detail-error">{{ error }} <button type="button" @click="load">Thử lại</button></div>
      <template v-else-if="product">
        <section class="detail-grid">
          <div class="detail-gallery">
            <div class="detail-main-image"><img :src="activeImage || product.imageUrl || '/img/picture1.png'" :alt="product.name" /></div>
            <div class="detail-thumbnails"><button v-for="(image,index) in galleryImages" :key="`${selectedVariantId}-${index}-${image}`" type="button" :class="{active:activeImage===image}" @click="activeImage=image"><img :src="image" :alt="`${product.name} - ảnh ${index+1}`" /></button></div>
          </div>
          <div class="detail-info">
            <span class="detail-eyebrow">{{ product.brand || 'FREEDOMS' }} · {{ product.category || 'GIÀY THỂ THAO' }}</span>
            <h1>{{ product.name }}</h1>
            <p class="detail-sku">Mã sản phẩm: {{ product.sku || `FD-${product.id}` }}</p>
            <div v-if="selectedVariant" class="detail-price">
              <strong>{{ money(selectedVariant.hasSale ? selectedVariant.salePrice : selectedVariant.unitPrice) }}</strong>
              <del v-if="selectedVariant.hasSale">{{ money(selectedVariant.unitPrice) }}</del>
              <span v-if="selectedVariant.hasSale">ĐANG ƯU ĐÃI</span>
            </div>
            <div v-else class="detail-price"><strong>{{ money(minPrice) }}</strong></div>
            <p class="detail-description">{{ product.description || 'Thiết kế linh hoạt, dễ phối đồ và phù hợp cho các hoạt động hằng ngày.' }}</p>
            <div class="detail-divider"></div>
            <label class="detail-field">Chọn kích cỡ và màu sắc
              <select v-model.number="selectedVariantId" @change="quantity=1">
                <option v-for="variant in variants" :key="variant.id" :value="variant.id" :disabled="Number(variant.stockQuantity) < 1">
                  {{ variant.size || 'Size tiêu chuẩn' }} · {{ variant.color || 'Màu tiêu chuẩn' }} · {{ variant.hasSale ? money(variant.salePrice) : money(variant.unitPrice) }} · {{ variant.stockQuantity > 0 ? `Còn ${variant.stockQuantity}` : 'Hết hàng' }}
                </option>
              </select>
            </label>
            <div v-if="selectedVariant" class="detail-stock" :class="{'out-of-stock':selectedVariant.stockQuantity<1}">
              <span></span>{{ selectedVariant.stockQuantity > 0 ? `Còn ${selectedVariant.stockQuantity} sản phẩm trong kho` : 'Tạm hết hàng ở phân loại này' }}
            </div>
            <div class="detail-buy-row">
              <div class="detail-quantity"><button type="button" :disabled="quantity<=1" @click="changeQuantity(-1)">−</button><span>{{ quantity }}</span><button type="button" :disabled="!selectedVariant || quantity>=selectedVariant.stockQuantity" @click="changeQuantity(1)">＋</button></div>
              <button type="button" class="detail-add" :disabled="!selectedVariant || selectedVariant.stockQuantity<1" @click="addToBag(false)">THÊM VÀO GIỎ</button>
            </div>
            <button type="button" class="detail-buy-now" :disabled="!selectedVariant || selectedVariant.stockQuantity<1" @click="addToBag(true)">MUA NGAY <span>→</span></button>
            <p v-if="error" class="detail-inline-error">{{ error }}</p>
            <div class="detail-promises"><div><b>✓</b><span><strong>Thanh toán linh hoạt</strong><small>COD hoặc chuyển khoản</small></span></div><div><b>↗</b><span><strong>Giao hàng toàn quốc</strong><small>Theo dõi đơn mua thuận tiện</small></span></div><div><b>◇</b><span><strong>Hỗ trợ sau mua</strong><small>Liên hệ FREEDOMS khi cần trợ giúp</small></span></div></div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.product-detail-page{background:#fff;color:#111;min-height:70vh}.detail-container{width:min(1180px,calc(100% - 48px));margin:0 auto;padding:28px 0 62px}.detail-breadcrumb{display:flex;align-items:center;gap:9px;margin-bottom:28px;font-size:10px;color:#888}.detail-breadcrumb a{color:#777;text-decoration:none}.detail-breadcrumb a:hover{color:#111;text-decoration:underline}.detail-breadcrumb b{color:#222;font-weight:700}.detail-grid{display:grid;grid-template-columns:minmax(0,1.05fr) minmax(360px,.95fr);gap:56px;align-items:start}.detail-gallery{min-width:0}.detail-main-image{height:550px;background:#f5f5f5;display:grid;place-items:center;border-radius:5px;overflow:hidden}.detail-main-image img{width:100%;height:100%;object-fit:contain;mix-blend-mode:multiply}.detail-thumbnails{display:flex;gap:10px;margin-top:12px}.detail-thumbnails button{width:72px;height:72px;border:1px solid #e2e2e2;background:#f7f7f7;padding:4px;border-radius:4px;cursor:pointer}.detail-thumbnails button.active{border-color:#111}.detail-thumbnails img{width:100%;height:100%;object-fit:contain}.detail-info{padding-top:8px}.detail-eyebrow{font-size:9px;font-weight:900;letter-spacing:.16em;color:#777}.detail-info h1{font-family:Georgia,'Times New Roman',serif;font-size:36px;font-weight:500;line-height:1.12;margin:13px 0 7px;color:#111}.detail-sku{font-size:10px;color:#888;margin:0 0 22px}.detail-price{display:flex;align-items:center;gap:12px;margin-bottom:20px}.detail-price strong{font-size:24px}.detail-price del{color:#999;font-size:13px}.detail-price span{font-size:8px;font-weight:900;letter-spacing:.08em;background:#111;color:#fff;padding:5px 7px}.detail-description{font-size:12px;color:#5f5f5f;line-height:1.9;margin:0;max-width:600px}.detail-divider{height:1px;background:#e8e8e8;margin:24px 0}.detail-field{display:flex;flex-direction:column;gap:9px;font-size:10px;font-weight:800;letter-spacing:.04em}.detail-field select{width:100%;height:44px;border:1px solid #ddd;background:#fff;border-radius:4px;padding:0 12px;color:#222;font-size:12px}.detail-stock{display:flex;align-items:center;gap:8px;color:#4d7658;font-size:10px;margin-top:11px}.detail-stock span{width:7px;height:7px;border-radius:50%;background:#4b9a62}.detail-stock.out-of-stock{color:#b42318}.detail-stock.out-of-stock span{background:#b42318}.detail-buy-row{display:grid;grid-template-columns:112px 1fr;gap:10px;margin-top:24px}.detail-quantity{height:46px;border:1px solid #ddd;display:flex;align-items:center;justify-content:space-between;border-radius:4px}.detail-quantity button{width:34px;height:100%;border:0;background:#fff;color:#111;font-size:17px;cursor:pointer}.detail-quantity button:disabled{opacity:.35;cursor:not-allowed}.detail-quantity span{font-size:12px;font-weight:700}.detail-add,.detail-buy-now{height:46px;border:1px solid #111;background:#fff;color:#111;font-size:10px;font-weight:900;letter-spacing:.1em;cursor:pointer}.detail-add:hover{background:#f4f4f4}.detail-buy-now{width:100%;margin-top:10px;background:#111;color:#fff;display:flex;align-items:center;justify-content:center;gap:20px}.detail-buy-now:hover{background:#333}.detail-add:disabled,.detail-buy-now:disabled{opacity:.45;cursor:not-allowed}.detail-inline-error{font-size:11px;color:#b42318;margin:12px 0 0}.detail-promises{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;border-top:1px solid #e8e8e8;margin-top:28px;padding-top:20px}.detail-promises>div{display:flex;gap:8px;align-items:flex-start}.detail-promises>div>b{width:22px;height:22px;border:1px solid #ddd;border-radius:50%;display:grid;place-items:center;font-size:10px;flex:none}.detail-promises strong,.detail-promises small{display:block}.detail-promises strong{font-size:9px}.detail-promises small{font-size:8px;color:#888;margin-top:5px;line-height:1.4}.detail-state{padding:55px 20px;background:#fafafa;text-align:center;color:#666;font-size:13px;border:1px solid #eee}.detail-error{color:#b42318}.detail-error button{margin-left:10px;border:0;background:#111;color:#fff;padding:8px 12px;cursor:pointer}
@media(max-width:850px){.detail-grid{grid-template-columns:1fr;gap:28px}.detail-main-image{height:min(90vw,480px)}.detail-info h1{font-size:30px}.detail-promises{grid-template-columns:1fr}.detail-container{width:calc(100% - 28px)}}
</style>
