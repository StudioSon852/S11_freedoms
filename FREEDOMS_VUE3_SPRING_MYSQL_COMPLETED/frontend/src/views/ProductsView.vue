<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'

const products = ref([])
const options = ref({ categories: [], brands: [], sizes: [], colors: [] })
const q = ref('')
const error = ref('')
const success = ref('')
const busy = ref(false)
const variantBusy = ref(null)
const editingId = ref(null)
const productImageFile = ref(null)
const productImagePreviewUrl = ref('')
const productImagePreview = computed(() => productImagePreviewUrl.value || form.value.imageUrl || '')
const variantUrlDrafts = ref({})
const form = ref(emptyForm())

function emptyForm() {
  return { productName: '', parentSku: '', description: '', imageUrl: '/img/picture1.png', categoryId: '', brandId: '', sku: '', sizeId: '', colorId: '', unitPrice: '', stockQuantity: 0, featured: false }
}
function newVariantDraft() { return { sku: '', sizeId: '', colorId: '', unitPrice: '', stockQuantity: 0 } }
const variantDrafts = ref({})
const activeCount = computed(() => products.value.filter(p => p.active).length)

async function load() {
  error.value = ''
  try {
    const [p, o] = await Promise.all([
      api.get('/api/admin/products', { params: { q: q.value } }),
      api.get('/api/admin/options')
    ])
    products.value = p.data
    options.value = o.data
    for (const product of products.value) if (!variantDrafts.value[product.id]) variantDrafts.value[product.id] = newVariantDraft()
  } catch (e) { error.value = errorMessage(e) }
}
function clearProductImagePreview() {
  if (productImagePreviewUrl.value) URL.revokeObjectURL(productImagePreviewUrl.value)
  productImagePreviewUrl.value = ''
}
function isSupportedImage(file) {
  const ext = String(file?.name || '').split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'].includes(ext)
}
function imageFallback(event) {
  const img = event?.target
  if (!img || img.dataset.fallbackApplied === '1') return
  img.dataset.fallbackApplied = '1'
  img.src = '/img/picture1.png'
}
function editProduct(p) {
  clearProductImagePreview()
  editingId.value = p.id
  form.value = {
    ...emptyForm(), productName: p.name || '', parentSku: p.sku || '', description: p.description || '',
    imageUrl: p.imageUrl || '', categoryId: options.value.categories.find(x => x.name === p.category)?.id || '',
    brandId: options.value.brands.find(x => x.name === p.brand)?.id || '', featured: !!p.featured
  }
  productImageFile.value = null
  document.querySelector('.product-editor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function cancelEdit() {
  clearProductImagePreview()
  editingId.value = null
  form.value = emptyForm()
  productImageFile.value = null
  const input = document.querySelector('.product-image-file-input')
  if (input) input.value = ''
}
function selectProductImage(event) {
  const file = event.target.files?.[0] || null
  error.value = ''
  if (!file) {
    productImageFile.value = null
    clearProductImagePreview()
    return
  }
  if (!isSupportedImage(file)) {
    event.target.value = ''
    productImageFile.value = null
    clearProductImagePreview()
    error.value = 'Ảnh chụp màn hình cần ở định dạng JPG, PNG, WEBP, GIF hoặc BMP.'
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    event.target.value = ''
    productImageFile.value = null
    clearProductImagePreview()
    error.value = 'Ảnh tải lên tối đa 8 MB.'
    return
  }
  clearProductImagePreview()
  productImageFile.value = file
  productImagePreviewUrl.value = URL.createObjectURL(file)
}
async function uploadProductImage(productId, file) {
  if (!file) return null
  const data = new FormData()
  data.append('file', file, file.name)
  // Do not set Content-Type manually; Axios/browser must add the multipart boundary.
  return (await api.post(`/api/admin/products/${productId}/image`, data)).data.imageUrl
}
async function saveProduct() {
  error.value = ''; success.value = ''; busy.value = true
  try {
    const payload = {
      productName: form.value.productName.trim(), description: form.value.description,
      imageUrl: form.value.imageUrl, categoryId: form.value.categoryId ? Number(form.value.categoryId) : null,
      brandId: form.value.brandId ? Number(form.value.brandId) : null,
      active: true, featured: !!form.value.featured
    }
    let productId
    if (editingId.value) {
      await api.patch(`/api/admin/products/${editingId.value}`, payload)
      productId = editingId.value
      success.value = 'Đã lưu thay đổi sản phẩm.'
    } else {
      if (!form.value.sku.trim()) throw new Error('SKU biến thể đầu tiên là bắt buộc.')
      const created = (await api.post('/api/admin/products', {
        ...form.value,
        categoryId: form.value.categoryId ? Number(form.value.categoryId) : null,
        brandId: form.value.brandId ? Number(form.value.brandId) : null,
        sizeId: form.value.sizeId ? Number(form.value.sizeId) : null,
        colorId: form.value.colorId ? Number(form.value.colorId) : null,
        unitPrice: Number(form.value.unitPrice), stockQuantity: Number(form.value.stockQuantity)
      })).data
      productId = created.id
      success.value = 'Đã tạo sản phẩm và biến thể đầu tiên.'
    }
    if (productImageFile.value) {
      const uploadedUrl = await uploadProductImage(productId, productImageFile.value)
      if (uploadedUrl) form.value.imageUrl = uploadedUrl
    }
    cancelEdit(); await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
async function toggleProduct(p) {
  error.value = ''; success.value = ''
  try {
    await api.patch(`/api/admin/products/${p.id}`, { active: !p.active })
    success.value = p.active ? 'Đã chuyển sản phẩm sang Dừng.' : 'Đã bật lại sản phẩm.'
    await load()
  } catch (e) { error.value = errorMessage(e) }
}
async function saveVariant(v) {
  error.value = ''; success.value = ''; variantBusy.value = v.id
  try {
    await api.patch(`/api/admin/variants/${v.id}`, {
      sku: v.sku, sizeId: v.sizeId || null, colorId: v.colorId || null,
      unitPrice: Number(v.unitPrice), stockQuantity: Number(v.stockQuantity), active: !!v.active
    })
    success.value = `Đã cập nhật biến thể ${v.sku}.`; await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { variantBusy.value = null }
}
async function createVariant(p) {
  const d = variantDrafts.value[p.id]
  error.value = ''; success.value = ''; variantBusy.value = p.id
  try {
    await api.post(`/api/admin/products/${p.id}/variants`, {
      sku: d.sku.trim(), sizeId: d.sizeId ? Number(d.sizeId) : null,
      colorId: d.colorId ? Number(d.colorId) : null, unitPrice: Number(d.unitPrice), stockQuantity: Number(d.stockQuantity)
    })
    success.value = `Đã thêm biến thể cho ${p.name}.`; variantDrafts.value[p.id] = newVariantDraft(); await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { variantBusy.value = null }
}
async function uploadVariantImages(v, event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  const invalid = files.find(file => !isSupportedImage(file) || file.size > 8 * 1024 * 1024)
  if (invalid) {
    error.value = !isSupportedImage(invalid)
      ? `Tệp ${invalid.name} không phải ảnh JPG, PNG, WEBP, GIF hoặc BMP.`
      : `Ảnh ${invalid.name} vượt quá giới hạn 8 MB.`
    event.target.value = ''
    return
  }
  error.value = ''; success.value = ''; variantBusy.value = v.id
  try {
    for (const file of files) {
      const data = new FormData(); data.append('file', file)
      await api.post(`/api/admin/variants/${v.id}/images`, data, {})
    }
    success.value = `Đã tải ${files.length} ảnh cho SKU ${v.sku}.`; await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { variantBusy.value = null; event.target.value = '' }
}
async function addImageUrl(v) {
  const url = (variantUrlDrafts.value[v.id] || '').trim()
  if (!url) return
  try {
    await api.post(`/api/admin/variants/${v.id}/images/url`, { imageUrl: url })
    variantUrlDrafts.value[v.id] = ''; success.value = 'Đã thêm địa chỉ ảnh.'; await load()
  } catch (e) { error.value = errorMessage(e) }
}
async function removeImage(v, image) {
  if (!confirm('Xóa địa chỉ ảnh này khỏi biến thể?')) return
  try {
    await api.delete(`/api/admin/variants/${v.id}/images/${image.id}`)
    success.value = 'Đã xóa ảnh khỏi biến thể.'; await load()
  } catch (e) { error.value = errorMessage(e) }
}
async function disableVariant(v) {
  if (!confirm(`Dừng kinh doanh biến thể ${v.sku}?`)) return
  try { await api.delete(`/api/admin/variants/${v.id}`); success.value = 'Đã dừng biến thể.'; await load() }
  catch (e) { error.value = errorMessage(e) }
}
onMounted(load)
onBeforeUnmount(clearProductImagePreview)
</script>

<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">CATALOG / INVENTORY CRUD</span><h1>Quản lý sản phẩm</h1><p>Tải ảnh trực tiếp, quản lý nhiều ảnh theo từng biến thể, chỉnh sửa sản phẩm và tồn kho.</p></div></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>

    <section class="surface-card product-editor">
      <div class="section-toolbar"><div><span class="eyebrow">{{ editingId ? 'UPDATE / PRODUCT' : 'CREATE / PRODUCT' }}</span><h2>{{ editingId ? `Cập nhật sản phẩm #${editingId}` : 'Thêm sản phẩm và biến thể đầu tiên' }}</h2></div><button v-if="editingId" class="btn btn-outline btn-sm" type="button" @click="cancelEdit">Hủy chỉnh sửa</button></div>
      <form class="product-create-grid" @submit.prevent="saveProduct">
        <label>Tên sản phẩm<input v-model.trim="form.productName" required maxlength="180" /></label>
        <label>SKU sản phẩm / SKU cha<input v-model.trim="form.parentSku" :disabled="!!editingId" /></label>
        <template v-if="!editingId">
          <label>SKU biến thể đầu tiên<input v-model.trim="form.sku" required /></label>
          <label>Giá bán (₫)<input v-model="form.unitPrice" type="number" min="1" step="1" required /></label>
          <label>Tồn kho ban đầu<input v-model="form.stockQuantity" type="number" min="0" step="1" required /></label>
        </template>
        <label>Đường dẫn ảnh đại diện<input v-model="form.imageUrl" placeholder="URL hoặc /img/..." /></label>
        <label>Tải ảnh đại diện / ảnh chụp màn hình<input class="product-image-file-input" type="file" accept=".jpg,.jpeg,.png,.webp,.gif,.bmp,image/jpeg,image/png,image/webp,image/gif,image/bmp" @change="selectProductImage" /><small class="muted">Chọn JPG, PNG, WEBP, GIF hoặc BMP, tối đa 8 MB.</small></label>
        <div v-if="productImagePreview" class="product-image-preview"><span>Ảnh xem trước</span><img :src="productImagePreview" alt="Ảnh sản phẩm xem trước" @error="imageFallback" /></div>
        <label>Danh mục<select v-model="form.categoryId"><option value="">Chưa chọn</option><option v-for="x in options.categories" :key="x.id" :value="x.id">{{ x.name }}</option></select></label>
        <label>Thương hiệu<select v-model="form.brandId"><option value="">Chưa chọn</option><option v-for="x in options.brands" :key="x.id" :value="x.id">{{ x.name }}</option></select></label>
        <template v-if="!editingId">
          <label>Kích cỡ<select v-model="form.sizeId"><option value="">Mặc định</option><option v-for="x in options.sizes" :key="x.id" :value="x.id">{{ x.label }}</option></select></label>
          <label>Màu sắc<select v-model="form.colorId"><option value="">Mặc định</option><option v-for="x in options.colors" :key="x.id" :value="x.id">{{ x.name }}</option></select></label>
        </template>
        <label class="wide-field">Mô tả<textarea v-model="form.description" rows="2" maxlength="2000"></textarea></label>
        <label class="toggle-line"><input v-model="form.featured" type="checkbox" /> Sản phẩm nổi bật</label>
        <button class="btn btn-primary" :disabled="busy">{{ busy ? 'Đang lưu…' : (editingId ? 'Lưu thay đổi' : 'Tạo sản phẩm') }}</button>
      </form>
      <p class="muted small-text">Ảnh tải lên được lưu vào backend tại <code>uploads/products</code>; hỗ trợ JPG, PNG, WEBP, GIF, tối đa 8 MB/ảnh.</p>
    </section>

    <section class="surface-card">
      <div class="section-toolbar"><div><span class="eyebrow">READ / UPDATE / DISABLE</span><h2>Danh sách sản phẩm ({{ products.length }})</h2></div><div class="search-tools"><input v-model="q" @keyup.enter="load" placeholder="Tìm tên sản phẩm / SKU"/><button class="btn btn-outline btn-sm" @click="load">Tìm kiếm</button></div></div>
      <article v-for="p in products" :key="p.id" class="managed-product">
        <div class="managed-product-head">
          <img :src="p.imageUrl || '/img/picture1.png'" :alt="p.name" @error="imageFallback" />
          <div class="product-edit-fields"><div class="product-meta"><small>#{{ p.id }} · {{ p.sku || 'Chưa có SKU cha' }} · {{ p.brand || 'Chưa có thương hiệu' }} · {{ p.category || 'Chưa phân loại' }}</small><span :class="p.active ? 'text-success' : 'muted'">{{ p.active ? 'Hoạt động' : 'Dừng' }}</span></div><b>{{ p.name }}</b><small>{{ p.description }}</small></div>
          <div class="product-actions"><button class="btn btn-outline btn-sm" @click="editProduct(p)">Chỉnh sửa</button><button class="btn btn-danger-outline btn-sm" @click="toggleProduct(p)">{{ p.active ? 'Dừng' : 'Hoạt động' }}</button></div>
        </div>
        <div class="variant-management">
          <div class="variant-management-heading"><strong>Biến thể / tồn kho / thư viện ảnh ({{ p.variants?.length || 0 }})</strong><button class="btn btn-outline btn-sm" @click="variantDrafts[p.id] = variantDrafts[p.id] || newVariantDraft(); variantDrafts[p.id].show = !variantDrafts[p.id].show">{{ variantDrafts[p.id]?.show ? 'Đóng' : 'Thêm biến thể' }}</button></div>
          <article v-for="v in p.variants" :key="v.id" class="variant-admin-item">
            <div class="variant-management-row">
              <label>SKU<input v-model.trim="v.sku" /></label>
              <label>Size<select v-model="v.sizeId"><option :value="null">Không chọn</option><option v-for="x in options.sizes" :key="x.id" :value="x.id">{{ x.label }}</option></select></label>
              <label>Màu<select v-model="v.colorId"><option :value="null">Không chọn</option><option v-for="x in options.colors" :key="x.id" :value="x.id">{{ x.name }}</option></select></label>
              <label>Giá (₫)<input v-model="v.unitPrice" type="number" min="1" step="1" /></label>
              <label>Tồn kho<input v-model="v.stockQuantity" type="number" min="0" step="1" /></label>
              <label class="toggle-line"><input v-model="v.active" type="checkbox"/> Hoạt động</label>
              <div class="variant-row-actions"><button class="btn btn-primary btn-sm" :disabled="variantBusy===v.id" @click="saveVariant(v)">Lưu biến thể</button><button class="btn btn-danger-outline btn-sm" @click="disableVariant(v)">Dừng</button></div>
            </div>
            <div class="variant-image-manager"><div class="variant-image-list"><div v-for="image in v.images || []" :key="image.id" class="variant-image-card"><img :src="image.imageUrl" :alt="v.sku" @error="imageFallback"/><button type="button" title="Xóa ảnh" @click="removeImage(v,image)">×</button><small>{{ image.imageUrl }}</small></div><p v-if="!(v.images || []).length" class="muted small-text">Chưa có ảnh riêng cho biến thể này.</p></div>
              <div class="variant-image-add"><label class="file-upload-label">Tải nhiều ảnh<input type="file" multiple accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadVariantImages(v,$event)"/></label><div class="image-url-input"><input v-model="variantUrlDrafts[v.id]" placeholder="Hoặc dán địa chỉ ảnh http(s)…"/><button class="btn btn-outline btn-sm" @click="addImageUrl(v)">Thêm URL</button></div></div>
            </div>
          </article>
          <form v-if="variantDrafts[p.id]?.show" class="variant-create-form" @submit.prevent="createVariant(p)"><strong>Thêm size / màu mới cho {{ p.name }}</strong><label>SKU<input v-model.trim="variantDrafts[p.id].sku" required /></label><label>Size<select v-model="variantDrafts[p.id].sizeId"><option value="">Không chọn</option><option v-for="x in options.sizes" :key="x.id" :value="x.id">{{ x.label }}</option></select></label><label>Màu sắc<select v-model="variantDrafts[p.id].colorId"><option value="">Không chọn</option><option v-for="x in options.colors" :key="x.id" :value="x.id">{{ x.name }}</option></select></label><label>Giá bán<input v-model="variantDrafts[p.id].unitPrice" type="number" min="1" required/></label><label>Tồn kho<input v-model="variantDrafts[p.id].stockQuantity" type="number" min="0" required/></label><button class="btn btn-primary btn-sm" :disabled="variantBusy===p.id">Lưu biến thể</button></form>
        </div>
      </article>
      <div v-if="!products.length" class="empty-state">Không có sản phẩm phù hợp.</div>
    </section>
  </div>
</template>
