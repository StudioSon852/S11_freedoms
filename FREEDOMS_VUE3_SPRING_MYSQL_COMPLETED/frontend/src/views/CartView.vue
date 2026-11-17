<script setup>
import { computed, onMounted } from 'vue'
import { useCartStore } from '../stores/cart'

const cart = useCartStore()
const allSelected = computed(() => cart.items.length > 0 && cart.checkoutIds.length === cart.items.length)
onMounted(() => { if (!cart.checkoutIds.length && cart.items.length) cart.selectAll(true) })
function checkout() {
  if (!cart.checkoutIds.length) return alert('Hãy tích chọn ít nhất một sản phẩm để thanh toán.')
  cart.checkoutIds = cart.checkoutIds.filter(id => cart.items.some(i => i.variantId === id))
  window.location.hash = '#/checkout'
}
</script>

<template>
  <div class="simple-page ocean-backdrop">
    
    <main class="narrow-main">
      <div class="page-heading"><span class="eyebrow">SHOPPING CART</span><h1>Giỏ hàng của bạn</h1><p class="muted">Tích chọn những sản phẩm muốn thanh toán trước. Những sản phẩm chưa chọn sẽ được giữ lại trong giỏ hàng.</p></div>
      <section v-if="cart.items.length" class="surface-card">
        <div class="cart-select-all"><label class="toggle-line"><input type="checkbox" :checked="allSelected" @change="cart.selectAll($event.target.checked)"/> Chọn tất cả ({{ cart.items.length }} sản phẩm)</label></div>
        <article v-for="item in cart.items" :key="item.variantId" class="cart-line">
          <input type="checkbox" :checked="cart.checkoutIds.includes(item.variantId)" @change="cart.toggle(item.variantId, $event.target.checked)"/>
          <img :src="item.imageUrl || '/img/picture1.png'" :alt="item.productName"/>
          <div class="cart-line-info"><b>{{ item.productName }}</b><small>{{ item.variantLabel }} · SKU {{ item.sku }}</small><strong>{{ Number(item.salePrice || item.unitPrice).toLocaleString('vi-VN') }}đ</strong></div>
          <div class="quantity-control"><button @click="cart.setQuantity(item.variantId, item.quantity - 1)">−</button><input :value="item.quantity" type="number" min="1" @change="cart.setQuantity(item.variantId, $event.target.value)"/><button @click="cart.setQuantity(item.variantId, item.quantity + 1)">+</button></div>
          <button class="icon-btn danger-text" title="Xóa" @click="cart.remove(item.variantId)">×</button>
        </article>
        <div class="cart-summary"><div><small>Đã chọn {{ cart.selectedCount }} sản phẩm</small><h2>{{ cart.selectedSubtotal.toLocaleString('vi-VN') }}đ</h2></div><button class="btn btn-primary" :disabled="!cart.checkoutIds.length" @click="checkout">Thanh toán sản phẩm đã chọn →</button></div>
      </section>
      <section v-else class="surface-card empty-state"><h2>Giỏ hàng đang trống</h2><RouterLink to="/store" class="btn btn-primary">Chọn giày ngay</RouterLink></section>
    </main>
  </div>
</template>
