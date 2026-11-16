import { defineStore } from 'pinia'

const STORAGE_KEY = 'freedoms-cart-v2'
function readCart() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') } catch { return [] }
}

export const useCartStore = defineStore('cart', {
  state: () => ({ items: readCart(), checkoutIds: [], drawerOpen: false }),
  getters: {
    count: state => state.items.reduce((sum, item) => sum + item.quantity, 0),
    selected: state => state.items.filter(item => state.checkoutIds.includes(item.variantId)),
    selectedCount() { return this.selected.reduce((sum, item) => sum + item.quantity, 0) },
    selectedSubtotal() { return this.selected.reduce((sum, item) => sum + Number(item.salePrice ?? item.unitPrice) * item.quantity, 0) }
  },
  actions: {
    persist() { localStorage.setItem(STORAGE_KEY, JSON.stringify(this.items)) },
    add(product, variant, quantity = 1) {
      const found = this.items.find(item => item.variantId === variant.id)
      if (found) {
        found.quantity = Math.min(1000, found.quantity + quantity)
        found.productId = product.id
        found.productName = product.name
        found.imageUrl = variant.images?.[0]?.imageUrl || product.imageUrl || found.imageUrl
        found.salePrice = Number(variant.salePrice ?? variant.unitPrice)
        found.unitPrice = Number(variant.unitPrice)
      } else this.items.push({
        productId: product.id, variantId: variant.id, productName: product.name, sku: variant.sku,
        variantLabel: [variant.size, variant.color].filter(Boolean).join(' · '),
        imageUrl: variant.images?.[0]?.imageUrl || product.imageUrl || '/img/picture1.png', unitPrice: Number(variant.unitPrice),
        salePrice: Number(variant.salePrice ?? variant.unitPrice), hasSale: Boolean(variant.hasSale),
        quantity, selected: true
      })
      this.persist()
    },
    setQuantity(variantId, quantity) {
      const item = this.items.find(x => x.variantId === variantId)
      if (!item) return
      item.quantity = Math.max(1, Math.min(1000, Number(quantity) || 1))
      this.persist()
    },
    toggle(variantId, selected) {
      if (selected) { if (!this.checkoutIds.includes(variantId)) this.checkoutIds.push(variantId) }
      else this.checkoutIds = this.checkoutIds.filter(id => id !== variantId)
    },
    selectAll(value) { this.checkoutIds = value ? this.items.map(x => x.variantId) : [] },
    beginCheckout() { this.checkoutIds = this.items.filter(x => x.selected !== false).map(x => x.variantId) },
    remove(variantId) { this.items = this.items.filter(x => x.variantId !== variantId); this.checkoutIds = this.checkoutIds.filter(x => x !== variantId); this.persist() },
    removeCheckedOut() { const ids = new Set(this.checkoutIds); this.items = this.items.filter(x => !ids.has(x.variantId)); this.checkoutIds = []; this.persist() },
    openDrawer() { this.drawerOpen = true },
    closeDrawer() { this.drawerOpen = false },
    clear() { this.items = []; this.checkoutIds = []; this.persist() }
  }
})
