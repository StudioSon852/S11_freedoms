<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const products = ref([])
const search = ref(String(route.query.q || ''))
const salesOnly = ref(false)
const selectedCategory = ref('Tất cả')
const loading = ref(false)
const error = ref('')
const selected = ref({})
let timer

const categories = computed(() => ['Tất cả', ...new Set(products.value.map(p => String(p.category || '').trim()).filter(Boolean))])
const filteredProducts = computed(() => products.value.filter(p => selectedCategory.value === 'Tất cả' || String(p.category || '').toLowerCase() === selectedCategory.value.toLowerCase()))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/api/store/products', { params: { q: search.value.trim(), salesOnly: salesOnly.value, channel: 'ONLINE' } })
    products.value = Array.isArray(data) ? data : []
    for (const product of products.value) {
      if (product.variants?.length && !selected.value[product.id]) {
        const firstAvailable = product.variants.find(v => Number(v.stockQuantity) > 0)
        selected.value[product.id] = (firstAvailable || product.variants[0]).id
      }
    }
    if (selectedCategory.value !== 'Tất cả' && !categories.value.includes(selectedCategory.value)) selectedCategory.value = 'Tất cả'
  } catch (e) {
    error.value = errorMessage(e)
    products.value = []
  } finally {
    loading.value = false
  }
}

function add(product) {
  const variant = product.variants?.find(v => v.id === selected.value[product.id])
  if (!variant || Number(variant.stockQuantity) < 1) return
  cart.add(product, variant, 1)
  cart.openDrawer()
}
function openProduct(product) { router.push(`/product/${product.id}`) }
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')}đ` }
function scrollToProducts() { document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function isSale(product) {
  const variant = product.variants?.find(v => v.id === selected.value[product.id])
  return Boolean(variant?.hasSale)
}

watch([search, salesOnly], () => {
  clearTimeout(timer)
  timer = setTimeout(load, 240)
})
watch(() => route.query.q, value => { search.value = String(value || '') })
onMounted(load)
</script>

<template>
  <div class="home-page">
    <main class="home-main">
      <section class="home-hero">
        <div class="home-hero-copy">
          <span class="home-eyebrow">FREEDOMS · NEW COLLECTION</span>
          <h1>Mỗi bước chân<br />là một <em>phong cách.</em></h1>
          <p>Khám phá những đôi giày đồng hành cùng phong cách và nhịp sống của bạn.</p>
          <button type="button" class="home-primary-button" @click="scrollToProducts">KHÁM PHÁ BỘ SƯU TẬP <span>→</span></button>
          <div class="home-hero-note"><span>✓</span> Sản phẩm chọn lọc <span>✓</span> Hỗ trợ đổi size</div>
        </div>
        <div class="home-hero-art">
          <div class="home-hero-circle"></div>
          <img src="/img/picture1.png" alt="Giày FREEDOMS nổi bật" />
          <div class="home-floating-tag"><strong>FIND YOUR STYLE</strong><small>Fresh looks. Fresh steps.</small></div>
        </div>
      </section>

      <section class="home-benefits" aria-label="Dịch vụ cửa hàng">
        <div><span class="benefit-icon">↗</span><span><strong>Giao hàng toàn quốc</strong><small>Đóng gói cẩn thận</small></span></div>
        <div><span class="benefit-icon">◇</span><span><strong>Đổi size linh hoạt</strong><small>Hỗ trợ chọn kích thước</small></span></div>
        <div><span class="benefit-icon">✓</span><span><strong>Thanh toán thuận tiện</strong><small>COD và chuyển khoản QR</small></span></div>
      </section>

      <section id="catalog" class="home-catalog">
        <div class="home-section-heading">
          <div><span class="home-eyebrow">DÀNH RIÊNG CHO BẠN</span><h2>Khám phá bộ sưu tập</h2><p>Giá và tồn kho được kiểm tra lại tại thời điểm đặt hàng.</p></div>
          <span class="home-product-count">{{ filteredProducts.length }} sản phẩm</span>
        </div>
        <div class="home-catalog-tools">
          <div class="home-category-tabs">
            <button v-for="category in categories" :key="category" type="button" :class="{ active: selectedCategory === category }" @click="selectedCategory = category">{{ category }}</button>
          </div>
          <label class="home-sale-filter"><input v-model="salesOnly" type="checkbox" /> Chỉ xem sản phẩm đang giảm</label>
        </div>
        <p v-if="error" class="home-notice home-error">{{ error }}</p>
        <p v-else-if="loading" class="home-notice">Đang tải danh sách sản phẩm…</p>
        <div v-else-if="!filteredProducts.length" class="home-empty">Không tìm thấy sản phẩm phù hợp. Hãy thử từ khóa khác.</div>
        <div v-else class="home-product-grid">
          <article v-for="product in filteredProducts" :key="product.id" class="home-product-card">
            <button type="button" class="home-product-image home-product-image-button" @click="openProduct(product)" :aria-label="`Xem chi tiết ${product.name}`">
              <span v-if="product.hasSale || isSale(product)" class="home-sale-badge">ĐANG GIẢM</span>
              <img :src="product.imageUrl || '/img/picture1.png'" :alt="product.name" loading="lazy" />
            </button>
            <div class="home-product-info">
              <div class="home-product-meta">{{ product.brand || 'FREEDOMS' }} <span>·</span> {{ product.category || 'Giày thể thao' }}</div>
              <h3><button type="button" class="home-product-title" @click="openProduct(product)">{{ product.name }}</button></h3>
              <p class="home-product-description">{{ product.description || 'Thiết kế năng động, dễ phối cùng phong cách hằng ngày.' }}</p>
              <label class="home-variant-label">Chọn size / màu
                <select v-model.number="selected[product.id]">
                  <option v-for="variant in product.variants || []" :key="variant.id" :value="variant.id" :disabled="Number(variant.stockQuantity) < 1">
                    {{ variant.size || 'Size tự do' }} · {{ variant.color || 'Mặc định' }} · còn {{ variant.stockQuantity }}
                  </option>
                </select>
              </label>
              <div v-if="product.variants?.find(v => v.id === selected[product.id])" class="home-price-row">
                <template v-if="product.variants.find(v => v.id === selected[product.id]).hasSale">
                  <strong class="home-sale-price">{{ money(product.variants.find(v => v.id === selected[product.id]).salePrice) }}</strong>
                  <del>{{ money(product.variants.find(v => v.id === selected[product.id]).unitPrice) }}</del>
                </template>
                <strong v-else>{{ money(product.variants.find(v => v.id === selected[product.id]).unitPrice) }}</strong>
              </div>
              <button class="home-add-button" type="button" :disabled="!product.variants?.length || Number(product.variants.find(v => v.id === selected[product.id])?.stockQuantity) < 1" @click="add(product)">THÊM VÀO GIỎ <span>＋</span></button>
            </div>
          </article>
        </div>
      </section>

      <section id="home-about" class="home-about">
        <div class="home-about-copy">
          <span class="home-eyebrow">CÂU CHUYỆN FREEDOMS</span>
          <h2>Không chỉ là một đôi giày.<br /><em>Đó là cách bạn bước đi.</em></h2>
          <p>FREEDOMS được xây dựng với mong muốn giúp mỗi người tìm thấy đôi giày phù hợp với nhịp sống và dấu ấn riêng. Từ những bước chân thường ngày đến các hoạt động thể thao, chúng tôi chú trọng sự thoải mái, tính ứng dụng và trải nghiệm mua sắm rõ ràng.</p>
          <p>Chúng tôi đồng hành cùng khách hàng từ khâu chọn sản phẩm, chọn kích cỡ đến theo dõi đơn hàng và hỗ trợ sau mua.</p>
          <RouterLink to="/register" class="home-about-link">KHÁM PHÁ FREEDOMS <span>→</span></RouterLink>
        </div>
        <div class="home-about-mark"><img src="/img/freedoms-bird.png" alt="Biểu tượng FREEDOMS" /><strong>STEP INTO YOUR STYLE</strong><small>Move freely. Live confidently.</small></div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-page{background:#f9fcff;color:#17324d;min-height:100%;}
.home-main{width:min(1240px,calc(100% - 48px));margin:0 auto;padding:30px 0 58px;}
.home-hero{min-height:400px;display:grid;grid-template-columns:1fr 1fr;align-items:center;gap:28px;padding:38px 4%;background:linear-gradient(125deg,#f4faff 0%,#e1f1ff 100%);border:1px solid #d8eafa;border-radius:14px;overflow:hidden;}
.home-eyebrow{display:block;font-size:10px;letter-spacing:.17em;font-weight:900;color:#54748f;}
.home-hero-copy h1{margin:18px 0;font-family:Georgia,'Times New Roman',serif;font-size:clamp(38px,4.4vw,60px);line-height:1.02;font-weight:600;letter-spacing:-.045em;color:#17324d;}
.home-hero-copy h1 em,.home-about h2 em{font-style:italic;font-weight:500;color:#2d8bc5;}
.home-hero-copy p{max-width:410px;color:#526c83;line-height:1.75;font-size:13px;margin:0 0 24px;}
.home-primary-button{display:inline-flex;align-items:center;justify-content:center;gap:14px;padding:13px 18px;border:1px solid #216b9d;border-radius:6px;background:#216b9d;color:#fff;text-decoration:none;font-size:10px;font-weight:900;letter-spacing:.07em;cursor:pointer;transition:background .2s,transform .2s;}
.home-primary-button:hover{background:#174e77;transform:translateY(-1px);}
.home-hero-note{display:flex;align-items:center;gap:9px;margin-top:26px;color:#526c83;font-size:10px;}.home-hero-note span{color:#2785bd;font-weight:900;}
.home-hero-art{min-height:310px;display:flex;align-items:center;justify-content:center;position:relative;}
.home-hero-circle{position:absolute;width:min(31vw,330px);aspect-ratio:1;border-radius:50%;background:radial-gradient(circle,#d2ecff,#9ed2f4);}
.home-hero-art img{position:relative;z-index:1;width:100%;max-height:330px;object-fit:contain;mix-blend-mode:multiply;filter:drop-shadow(0 23px 25px rgba(28,94,136,.18));}
.home-floating-tag{position:absolute;z-index:2;right:0;bottom:15px;background:#fff;padding:12px 15px;border:1px solid #e1eef8;border-radius:7px;box-shadow:0 8px 24px rgba(35,87,121,.08);display:flex;flex-direction:column;gap:4px;}
.home-floating-tag strong{font-size:9px;letter-spacing:.12em;color:#17324d;}.home-floating-tag small{font-size:9px;color:#6d859a;}
.home-benefits{display:grid;grid-template-columns:repeat(3,1fr);gap:20px;padding:23px 0;border-bottom:1px solid #dce8f2;}
.home-benefits>div{display:flex;align-items:center;justify-content:center;gap:12px;}.benefit-icon{width:38px;height:38px;border:1px solid #d4e8f7;background:#f3faff;color:#287fb5;display:grid;place-items:center;border-radius:50%;font-size:18px;}
.home-benefits strong,.home-benefits small{display:block;}.home-benefits strong{font-size:11px;color:#17324d;}.home-benefits small{font-size:9px;color:#7a91a5;margin-top:4px;}
.home-catalog{padding-top:40px;scroll-margin-top:24px;}.home-section-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:20px;}.home-section-heading h2{font-family:Georgia,'Times New Roman',serif;font-size:32px;font-weight:600;letter-spacing:-.035em;color:#17324d;margin:8px 0 5px;}.home-section-heading p{font-size:11px;color:#6f8499;margin:0;}.home-product-count{font-size:10px;color:#6e879c;white-space:nowrap;}
.home-catalog-tools{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-bottom:19px;}.home-category-tabs{display:flex;gap:8px;overflow-x:auto;padding:2px 0 5px;}.home-category-tabs button{padding:8px 14px;border:1px solid #d6e4ef;background:#fff;border-radius:20px;white-space:nowrap;color:#526c83;font-size:10px;cursor:pointer;}.home-category-tabs button.active,.home-category-tabs button:hover{background:#216b9d;color:#fff;border-color:#216b9d;}.home-sale-filter{display:flex;align-items:center;gap:7px;font-size:10px;color:#526c83;white-space:nowrap;}.home-sale-filter input{accent-color:#216b9d;}
.home-notice,.home-empty{text-align:center;padding:28px;color:#617a90;font-size:12px;background:#fff;border:1px solid #e0eaf2;border-radius:8px;}.home-error{color:#a53a3a;background:#fff5f4;border-color:#f2d7d5;}
.home-product-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:17px;}.home-product-card{min-width:0;display:flex;flex-direction:column;background:#fff;border:1px solid #e0eaf2;border-radius:9px;overflow:hidden;transition:transform .2s,box-shadow .2s,border-color .2s;}.home-product-card:hover{transform:translateY(-3px);border-color:#beddf3;box-shadow:0 12px 28px rgba(32,93,128,.08);}.home-product-image{height:190px;position:relative;display:grid;place-items:center;background:#f1f8fd;padding:10px;}
.home-product-image-button{width:100%;border:0;cursor:pointer;}
.home-product-title{padding:0;border:0;background:transparent;text-align:left;font:inherit;color:inherit;font-weight:inherit;cursor:pointer;}
.home-product-title:hover{color:#111;text-decoration:underline;text-underline-offset:3px;}.home-product-image img{width:100%;height:100%;object-fit:contain;mix-blend-mode:multiply;}.home-sale-badge{position:absolute;z-index:2;left:10px;top:10px;padding:5px 7px;border-radius:3px;background:#216b9d;color:white;font-size:8px;font-weight:900;letter-spacing:.06em;}.home-product-info{display:flex;flex-direction:column;gap:9px;padding:13px;flex:1;}.home-product-meta{font-size:9px;color:#71879a;letter-spacing:.04em;}.home-product-meta span{padding:0 3px;}.home-product-info h3{font-size:12px;line-height:1.5;color:#17324d;margin:0;min-height:35px;}.home-product-description{font-size:9px;color:#8092a2;line-height:1.5;margin:0;min-height:27px;}.home-variant-label{display:flex;flex-direction:column;gap:5px;font-size:9px;color:#526c83;}.home-variant-label select{width:100%;min-width:0;border:1px solid #d6e4ef;border-radius:5px;padding:8px;background:#fff;color:#324b62;font-size:9px;}.home-price-row{display:flex;align-items:center;gap:8px;min-height:23px;}.home-price-row strong{font-size:12px;color:#1f6f9f;}.home-price-row del{font-size:9px;color:#9aacba;}.home-price-row .home-sale-price{color:#b84d45;}.home-add-button{display:flex;justify-content:space-between;align-items:center;width:100%;padding:10px 11px;border:1px solid #216b9d;border-radius:5px;background:#216b9d;color:#fff;font-size:9px;font-weight:900;letter-spacing:.04em;cursor:pointer;margin-top:auto;}.home-add-button:hover{background:#174e77;}.home-add-button:disabled{opacity:.5;cursor:not-allowed;}
.home-about{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(240px,.65fr);align-items:center;gap:42px;margin-top:48px;padding:38px 42px;background:#fff;border:1px solid #e6e6e6;border-radius:10px;scroll-margin-top:24px;color:#111;}.home-about h2{font-family:Georgia,'Times New Roman',serif;font-size:32px;font-weight:500;line-height:1.18;margin:10px 0 15px;color:#111;}.home-about h2 em{font-style:italic;color:#555;}.home-about p{font-size:12px;color:#555;line-height:1.85;margin:0 0 10px;max-width:720px;}.home-about-link{display:inline-flex;align-items:center;gap:14px;margin-top:12px;padding:11px 16px;border:1px solid #111;background:#111;color:#fff;text-decoration:none;font-size:10px;font-weight:800;letter-spacing:.08em;}.home-about-link:hover{background:#333;}.home-about-mark{min-height:230px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#f7f7f7;border-radius:8px;padding:22px;text-align:center;}.home-about-mark img{width:min(160px,70%);height:160px;object-fit:contain;}.home-about-mark strong{font-size:11px;letter-spacing:.17em;color:#111;margin-top:7px;}.home-about-mark small{font-size:10px;color:#777;margin-top:7px;}
@media(max-width:980px){.home-product-grid{grid-template-columns:repeat(3,minmax(0,1fr));}.home-hero{min-height:350px;}.home-hero-art{min-height:250px;}}
@media(max-width:700px){.home-main{width:calc(100% - 28px);padding:17px 0 36px;}.home-hero{grid-template-columns:1fr;gap:0;padding:26px 22px 12px;min-height:0;}.home-hero-copy h1{font-size:42px;}.home-hero-art{min-height:220px;}.home-hero-art img{max-height:220px;}.home-hero-circle{width:215px;}.home-floating-tag{bottom:5px;}.home-benefits{grid-template-columns:1fr;gap:14px;padding:18px 4px;}.home-benefits>div{justify-content:flex-start;}.home-section-heading h2{font-size:27px;}.home-catalog-tools{align-items:flex-start;flex-direction:column;gap:10px;}.home-product-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;}.home-product-image{height:140px;}.home-product-info{padding:10px;gap:7px;}.home-about{grid-template-columns:1fr;padding:25px 22px;margin-top:30px;gap:22px;}.home-about h2{font-size:27px;}}
</style>
