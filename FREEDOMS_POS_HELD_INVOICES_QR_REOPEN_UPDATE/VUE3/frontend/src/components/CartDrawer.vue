<script setup>
import { computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useCartStore } from '../stores/cart'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])
const cart = useCartStore()
const subtotal = computed(() => cart.items.reduce((sum, item) => sum + Number(item.salePrice ?? item.unitPrice) * Number(item.quantity), 0))
const money = value => `${Number(value || 0).toLocaleString('vi-VN')}₫`
function change(item, delta) { cart.setQuantity(item.variantId, Number(item.quantity) + delta) }
function close() { emit('close') }
watch(() => props.open, value => {
  if (typeof document !== 'undefined') document.body.classList.toggle('cart-drawer-open', value)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer-fade">
      <div v-if="open" class="cart-drawer-overlay" role="presentation" @click.self="close" @keydown.esc="close">
        <aside class="freedoms-cart-drawer" role="dialog" aria-modal="true" aria-label="Giỏ hàng">
          <header class="drawer-head">
            <div><span class="drawer-kicker">FREEDOMS / YOUR BAG</span><h2>Giỏ hàng <small>({{ cart.count }})</small></h2></div>
            <button type="button" class="drawer-close" aria-label="Đóng giỏ hàng" @click="close">×</button>
          </header>
          <div class="drawer-body">
            <div v-if="!cart.items.length" class="drawer-empty">
              <span class="drawer-empty-icon">▢</span><h3>Giỏ hàng đang trống</h3>
              <p>Chọn một đôi giày phù hợp để bắt đầu hành trình của bạn.</p>
              <RouterLink to="/store" class="drawer-dark-button" @click="close">Tiếp tục mua sắm</RouterLink>
            </div>
            <div v-else class="drawer-items">
              <article v-for="item in cart.items" :key="item.variantId" class="drawer-item">
                <RouterLink v-if="item.productId" :to="`/product/${item.productId}`" class="drawer-item-image" @click="close">
                  <img :src="item.imageUrl || '/img/picture1.png'" :alt="item.productName" />
                </RouterLink>
                <div v-else class="drawer-item-image"><img :src="item.imageUrl || '/img/picture1.png'" :alt="item.productName" /></div>
                <div class="drawer-item-content">
                  <div class="drawer-item-top">
                    <div class="drawer-item-title">
                      <RouterLink v-if="item.productId" :to="`/product/${item.productId}`" @click="close">{{ item.productName }}</RouterLink>
                      <strong v-else>{{ item.productName }}</strong>
                      <small>{{ item.variantLabel || item.sku }}</small>
                    </div>
                    <button type="button" class="drawer-remove" :aria-label="`Xóa ${item.productName}`" @click="cart.remove(item.variantId)">×</button>
                  </div>
                  <div class="drawer-item-bottom">
                    <div class="drawer-quantity">
                      <button type="button" :disabled="item.quantity <= 1" @click="change(item, -1)" aria-label="Giảm số lượng">−</button>
                      <span>{{ item.quantity }}</span>
                      <button type="button" @click="change(item, 1)" aria-label="Tăng số lượng">＋</button>
                    </div>
                    <strong>{{ money((item.salePrice ?? item.unitPrice) * item.quantity) }}</strong>
                  </div>
                </div>
              </article>
              <button type="button" class="drawer-clear" @click="cart.clear()">Xóa toàn bộ giỏ hàng</button>
            </div>
          </div>
          <footer v-if="cart.items.length" class="drawer-footer">
            <div class="drawer-total"><span>Tạm tính</span><strong>{{ money(subtotal) }}</strong></div>
            <p>Phí vận chuyển và ưu đãi sẽ được tính tại bước thanh toán.</p>
            <RouterLink to="/cart" class="drawer-cart-button" @click="close">Xem giỏ hàng đầy đủ</RouterLink>
            <RouterLink to="/checkout" class="drawer-checkout-button" @click="cart.beginCheckout(); close()">Tiến hành thanh toán <span>→</span></RouterLink>
            <button type="button" class="drawer-continue" @click="close">Tiếp tục mua sắm</button>
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style>
body.cart-drawer-open{overflow:hidden}
.cart-drawer-overlay{position:fixed;inset:0;z-index:2500;background:rgba(10,10,10,.45);display:flex;justify-content:flex-end}
.freedoms-cart-drawer{width:min(460px,100vw);height:100%;background:#fff;color:#111;display:flex;flex-direction:column;box-shadow:-18px 0 50px rgba(0,0,0,.13);font-family:Inter,Arial,sans-serif}
.drawer-head{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:23px 25px;border-bottom:1px solid #e8e8e8}
.drawer-kicker{display:block;color:#777;font-size:9px;font-weight:800;letter-spacing:.16em;margin-bottom:6px}
.drawer-head h2{font-family:Georgia,'Times New Roman',serif;font-size:25px;font-weight:500;margin:0;color:#111}
.drawer-head h2 small{font-family:Arial,sans-serif;font-size:12px;color:#777}
.drawer-close{width:36px;height:36px;border:0;border-radius:50%;background:#f3f3f3;color:#111;font-size:23px;cursor:pointer}
.drawer-close:hover{background:#e8e8e8}
.drawer-body{flex:1;min-height:0;overflow:auto}
.drawer-empty{min-height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:38px 26px}
.drawer-empty-icon{font-size:38px;color:#bbb;margin-bottom:12px}
.drawer-empty h3{font-family:Georgia,'Times New Roman',serif;font-size:23px;font-weight:500;margin:0 0 8px}
.drawer-empty p{max-width:280px;font-size:12px;line-height:1.7;color:#777;margin:0 0 19px}
.drawer-dark-button{padding:12px 18px;background:#111;color:#fff;text-decoration:none;border-radius:4px;font-size:11px;font-weight:800}
.drawer-items{padding:0 24px 18px}
.drawer-item{display:flex;gap:15px;padding:20px 0;border-bottom:1px solid #e8e8e8}
.drawer-item-image{width:88px;height:88px;flex:none;display:grid;place-items:center;background:#f4f4f4;border-radius:6px;overflow:hidden}
.drawer-item-image img{width:100%;height:100%;object-fit:contain;mix-blend-mode:multiply}
.drawer-item-content{min-width:0;flex:1;display:flex;flex-direction:column;justify-content:space-between;gap:12px}
.drawer-item-top,.drawer-item-bottom{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}
.drawer-item-title{min-width:0;display:flex;flex-direction:column;gap:5px}
.drawer-item-title a,.drawer-item-title strong{font-family:Georgia,'Times New Roman',serif;font-size:14px;font-weight:600;color:#111;text-decoration:none;line-height:1.3}
.drawer-item-title a:hover{text-decoration:underline}
.drawer-item-title small{font-size:10px;color:#777}
.drawer-remove{width:25px;height:25px;border:0;background:transparent;color:#999;font-size:20px;cursor:pointer;flex:none}
.drawer-remove:hover{color:#111}
.drawer-item-bottom{align-items:center}
.drawer-item-bottom>strong{font-size:13px;white-space:nowrap}
.drawer-quantity{display:flex;align-items:center;border:1px solid #e2e2e2;border-radius:5px;overflow:hidden}
.drawer-quantity button{width:28px;height:27px;border:0;background:#fff;color:#111;cursor:pointer}
.drawer-quantity button:disabled{opacity:.35;cursor:not-allowed}
.drawer-quantity span{min-width:27px;text-align:center;font-size:11px;font-weight:700;border-inline:1px solid #eee;line-height:27px}
.drawer-clear{border:0;background:transparent;color:#777;font-size:10px;text-decoration:underline;margin-top:15px;padding:4px 0;cursor:pointer}
.drawer-footer{padding:18px 25px 20px;border-top:1px solid #e5e5e5;background:#fff}
.drawer-total{display:flex;align-items:center;justify-content:space-between;font-size:13px;margin-bottom:5px}
.drawer-total strong{font-size:16px}
.drawer-footer>p{margin:0 0 13px;color:#777;font-size:10px}
.drawer-cart-button,.drawer-checkout-button{display:flex;align-items:center;justify-content:center;gap:10px;width:100%;box-sizing:border-box;padding:12px;border-radius:4px;text-decoration:none;font-size:11px;font-weight:800}
.drawer-cart-button{border:1px solid #111;color:#111;background:#fff}
.drawer-checkout-button{margin-top:8px;background:#111;color:#fff;border:1px solid #111}
.drawer-checkout-button:hover,.drawer-dark-button:hover{background:#333}
.drawer-continue{display:block;margin:12px auto 0;border:0;background:transparent;color:#777;text-decoration:underline;font-size:10px;cursor:pointer}
.drawer-fade-enter-active,.drawer-fade-leave-active{transition:opacity .2s ease}
.drawer-fade-enter-active .freedoms-cart-drawer,.drawer-fade-leave-active .freedoms-cart-drawer{transition:transform .24s ease}
.drawer-fade-enter-from,.drawer-fade-leave-to{opacity:0}
.drawer-fade-enter-from .freedoms-cart-drawer,.drawer-fade-leave-to .freedoms-cart-drawer{transform:translateX(30px)}
@media(max-width:480px){.drawer-head{padding:18px}.drawer-items{padding-inline:16px}.drawer-footer{padding:16px}.drawer-item-image{width:74px;height:74px}}
</style>
